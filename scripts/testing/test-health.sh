#!/bin/bash

# Health Check Script for Portfolio Application
# Tests the complete chain: Frontend -> Backend -> Database
# Usage: ./test-health.sh [environment]
# Environments: local (default), staging, prod

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
ENVIRONMENT=${1:-local}
TIMEOUT=5

case $ENVIRONMENT in
  local)
    BASE_URL="http://localhost:8081"
    BACKEND_URL="http://localhost:8080"
    FRONTEND_URL="http://localhost:4200"
    ;;
  staging)
    BASE_URL="http://localhost:3000"
    BACKEND_URL="http://localhost:8080"
    FRONTEND_URL="http://localhost:80"
    ;;
  prod)
    BASE_URL="http://localhost:80"
    BACKEND_URL="http://localhost:8080"
    FRONTEND_URL="http://localhost:80"
    ;;
  *)
    echo -e "${RED}[ERROR] Invalid environment: $ENVIRONMENT${NC}"
    echo "Usage: $0 [local|staging|prod]"
    exit 1
    ;;
esac

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  Portfolio Application Health Check${NC}"
echo -e "${BLUE}  Environment: ${YELLOW}$ENVIRONMENT${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Counter for passed/failed tests
PASSED=0
FAILED=0

# Test function
test_endpoint() {
  local name=$1
  local url=$2
  local expected_status=${3:-200}

  echo -n "Testing $name... "

  if response=$(curl -s -o /dev/null -w "%{http_code}" --max-time $TIMEOUT "$url" 2>/dev/null); then
    if [ "$response" -eq "$expected_status" ]; then
      echo -e "${GREEN}[PASS]${NC} (HTTP $response)"
      PASSED=$((PASSED + 1))
      return 0
    else
      echo -e "${RED}[FAIL]${NC} (HTTP $response, expected $expected_status)"
      FAILED=$((FAILED + 1))
      return 1
    fi
  else
    echo -e "${RED}[FAIL]${NC} (Connection failed or timeout)"
    FAILED=$((FAILED + 1))
    return 1
  fi
}

# Run tests (use || true to continue on failure and report all results)
echo -e "${YELLOW}[TEST] Testing Nginx endpoints...${NC}"
test_endpoint "Nginx health" "$BASE_URL/health" || true
echo ""

echo -e "${YELLOW}[TEST] Testing Frontend...${NC}"
test_endpoint "Frontend static health" "$FRONTEND_URL/health.json" || true
echo ""

echo -e "${YELLOW}[TEST] Testing Backend...${NC}"
test_endpoint "Backend ping" "$BACKEND_URL/api/health/ping" || true
test_endpoint "Backend actuator" "$BACKEND_URL/actuator/health" || true
echo ""

echo -e "${YELLOW}[TEST] Testing Database connection...${NC}"
test_endpoint "Database health" "$BACKEND_URL/api/health/db" || true
echo ""

echo -e "${YELLOW}[TEST] Testing full chain...${NC}"
test_endpoint "Full health check" "$BASE_URL/health/full" || true
echo ""

# Summary
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  Summary${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "  ${GREEN}Passed: $PASSED${NC}"
echo -e "  ${RED}Failed: $FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
  echo -e "${GREEN}[SUCCESS] All health checks passed!${NC}"
  echo ""
  exit 0
else
  echo -e "${RED}[FAILURE] Some health checks failed!${NC}"
  echo ""
  exit 1
fi
