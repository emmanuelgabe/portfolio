#!/usr/bin/env bash
#
# ci-local.sh - Reproduce the GitHub Actions CI pipeline locally.
#
# Runs the same commands as .github/workflows/ci-cd.yml, backend-tests.yml
# and frontend-tests.yml so you can catch failures before pushing.
#
# Extra checks not in CI (marked [LOCAL] in output):
#   - Frontend: tsc --noEmit (type check) -- catches type errors early
#
# Usage:
#   ./scripts/ci-local.sh              # full pipeline (backend + frontend)
#   ./scripts/ci-local.sh backend      # backend only
#   ./scripts/ci-local.sh frontend     # frontend only
#   ./scripts/ci-local.sh fast         # skip tests, coverage, and type check (lint + build only)
#
# Exit codes:
#   0  all stages passed
#   1  a stage failed (see output for which one)
#   2  environment mismatch with CI (Node/Java version)

set -euo pipefail

# ---------- Configuration (must match .github/workflows/*.yml) ----------
CI_NODE_MAJOR="20"
CI_JAVA_MAJOR="21"
BACKEND_DIR="portfolio-backend"
FRONTEND_DIR="portfolio-frontend"

# ---------- Output helpers ----------
if [ -t 1 ]; then
  C_RED=$'\033[0;31m'; C_GREEN=$'\033[0;32m'; C_YELLOW=$'\033[1;33m'
  C_BLUE=$'\033[0;34m'; C_BOLD=$'\033[1m'; C_RESET=$'\033[0m'
else
  C_RED=''; C_GREEN=''; C_YELLOW=''; C_BLUE=''; C_BOLD=''; C_RESET=''
fi

log_stage() { echo; echo "${C_BOLD}${C_BLUE}==> $*${C_RESET}"; }
log_ok()    { echo "${C_GREEN}[OK]${C_RESET} $*"; }
log_warn()  { echo "${C_YELLOW}[WARN]${C_RESET} $*"; }
log_err()   { echo "${C_RED}[FAIL]${C_RESET} $*"; }

# ---------- Root directory detection ----------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT_DIR"

MODE="${1:-all}"
case "$MODE" in
  all|backend|frontend|fast) ;;
  *) echo "Unknown mode: $MODE (expected: all|backend|frontend|fast)"; exit 1 ;;
esac

# ---------- Timing ----------
PIPELINE_START=$(date +%s)
declare -a STAGE_NAMES
declare -a STAGE_DURATIONS

run_stage() {
  local name="$1"; shift
  log_stage "$name"
  local start=$(date +%s)
  if "$@"; then
    local dur=$(($(date +%s) - start))
    STAGE_NAMES+=("$name")
    STAGE_DURATIONS+=("$dur")
    log_ok "$name (${dur}s)"
  else
    local dur=$(($(date +%s) - start))
    log_err "$name failed after ${dur}s"
    exit 1
  fi
}

# ---------- Environment check ----------
check_environment() {
  local mismatch=0

  if command -v node >/dev/null 2>&1; then
    local node_major
    node_major=$(node -v | sed -E 's/^v([0-9]+).*/\1/')
    if [ "$node_major" != "$CI_NODE_MAJOR" ]; then
      log_warn "Node local=v${node_major} vs CI=v${CI_NODE_MAJOR} - frontend may behave differently"
      mismatch=1
    else
      log_ok "Node v${node_major} matches CI"
    fi
  else
    log_err "node not found in PATH"
    exit 2
  fi

  if command -v java >/dev/null 2>&1; then
    local java_major
    java_major=$(java -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+).*/\1/')
    if [ "$java_major" != "$CI_JAVA_MAJOR" ]; then
      log_warn "Java local=${java_major} vs CI=${CI_JAVA_MAJOR} - Gradle toolchain should auto-download, but non-gradle commands will drift"
    else
      log_ok "Java ${java_major} matches CI"
    fi
  else
    log_err "java not found in PATH"
    exit 2
  fi

  if [ "$mismatch" -eq 1 ] && [ "${CI_LOCAL_STRICT:-0}" = "1" ]; then
    log_err "Environment mismatch detected and CI_LOCAL_STRICT=1 - aborting"
    exit 2
  fi
}

# ---------- Backend stages ----------
# Match CI: backend-tests.yml sets SPRING_PROFILES_ACTIVE=test
export SPRING_PROFILES_ACTIVE=test

backend_clean_build() {
  if [ "$MODE" = "fast" ]; then
    ( cd "$BACKEND_DIR" && ./gradlew clean build -x test --no-daemon --stacktrace )
  else
    ( cd "$BACKEND_DIR" && ./gradlew clean build --no-daemon --stacktrace )
  fi
}
backend_checkstyle() {
  ( cd "$BACKEND_DIR" && ./gradlew checkstyleMain checkstyleTest --no-daemon --stacktrace )
}
backend_spotbugs() {
  ( cd "$BACKEND_DIR" && ./gradlew spotbugsMain --no-daemon --stacktrace )
}
backend_coverage_verify() {
  ( cd "$BACKEND_DIR" && ./gradlew jacocoTestCoverageVerification --no-daemon )
}

run_backend() {
  if [ ! -d "$BACKEND_DIR" ]; then
    log_warn "$BACKEND_DIR not found, skipping backend"
    return
  fi
  run_stage "Backend: clean build (includes tests)" backend_clean_build
  run_stage "Backend: checkstyle (main + test)"     backend_checkstyle
  run_stage "Backend: spotbugs"                     backend_spotbugs
  if [ "$MODE" != "fast" ]; then
    run_stage "Backend: jacoco coverage verification" backend_coverage_verify
  fi
}

# ---------- Frontend stages ----------
frontend_install() {
  ( cd "$FRONTEND_DIR" && npm ci )
}
frontend_lint() {
  ( cd "$FRONTEND_DIR" && npm run lint )
}
frontend_typecheck() {
  ( cd "$FRONTEND_DIR" && npx tsc --noEmit )
}
frontend_test() {
  ( cd "$FRONTEND_DIR" && npm test -- --watch=false --browsers=ChromeHeadlessCI --code-coverage )
}
frontend_build() {
  ( cd "$FRONTEND_DIR" && npm run build -- --configuration=production )
}

run_frontend() {
  if [ ! -d "$FRONTEND_DIR" ]; then
    log_warn "$FRONTEND_DIR not found, skipping frontend"
    return
  fi
  run_stage "Frontend: npm ci (clean install)"        frontend_install
  run_stage "Frontend: eslint"                         frontend_lint
  if [ "$MODE" != "fast" ]; then
    run_stage "Frontend: tsc --noEmit [LOCAL extra]"     frontend_typecheck
  fi
  if [ "$MODE" != "fast" ]; then
    run_stage "Frontend: karma tests (ChromeHeadlessCI)" frontend_test
  fi
  run_stage "Frontend: production build"               frontend_build
}

# ---------- Main ----------
log_stage "Environment check (must match GitHub Actions)"
check_environment

case "$MODE" in
  backend)  run_backend ;;
  frontend) run_frontend ;;
  all|fast) run_backend; run_frontend ;;
esac

# ---------- Summary ----------
TOTAL=$(($(date +%s) - PIPELINE_START))
echo
echo "${C_BOLD}========== ci-local.sh summary ==========${C_RESET}"
for i in "${!STAGE_NAMES[@]}"; do
  printf "  %-55s %ss\n" "${STAGE_NAMES[$i]}" "${STAGE_DURATIONS[$i]}"
done
echo "${C_BOLD}Total: ${TOTAL}s${C_RESET}"
log_ok "All stages passed - safe to push"
