#!/bin/bash
# Run all tests for Portfolio Application
# Usage: ./scripts/testing/run-all-tests.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

cd "$PROJECT_ROOT"

echo "========================================="
echo "Running Portfolio Test Suite"
echo "========================================="
echo ""

# Backend Tests
echo "[1/2] Running Backend Tests..."
echo "----------------------------------------"
cd portfolio-backend
./gradlew clean test jacocoTestReport
BACKEND_EXIT=$?

if [ $BACKEND_EXIT -eq 0 ]; then
    echo "[SUCCESS] Backend tests passed"
    echo "Coverage report: build/reports/jacoco/test/html/index.html"
else
    echo "[FAILED] Backend tests failed"
    exit 1
fi

echo ""

# Frontend Tests
echo "[2/2] Running Frontend Tests..."
echo "----------------------------------------"
cd ../portfolio-frontend
npm test -- --watch=false --code-coverage --browsers=ChromeHeadlessCI
FRONTEND_EXIT=$?

if [ $FRONTEND_EXIT -eq 0 ]; then
    echo "[SUCCESS] Frontend tests passed"
    echo "Coverage report: coverage/portfolio-frontend/index.html"
else
    echo "[FAILED] Frontend tests failed"
    exit 1
fi

echo ""
echo "========================================="
echo "[SUCCESS] All tests passed!"
echo "========================================="
echo ""
echo "Backend coverage:  portfolio-backend/build/reports/jacoco/test/html/index.html"
echo "Frontend coverage: portfolio-frontend/coverage/portfolio-frontend/index.html"
