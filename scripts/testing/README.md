# Testing Scripts

## Available Scripts

### run-all-tests.sh

Runs complete test suite for both backend and frontend with coverage reports.

**Usage:**
```bash
./scripts/testing/run-all-tests.sh
```

**What it does:**
1. Cleans and runs backend tests with JaCoCo coverage
2. Runs frontend tests with Karma coverage
3. Reports test results and coverage locations

**Requirements:**
- Backend: Java 21, Gradle
- Frontend: Node.js 20+

**Output:**
- Backend coverage: `portfolio-backend/build/reports/jacoco/test/html/index.html`
- Frontend coverage: `portfolio-frontend/coverage/portfolio-frontend/index.html`

## Individual Test Commands

**Backend only:**
```bash
cd portfolio-backend
./gradlew test jacocoTestReport
```

**Frontend only:**
```bash
cd portfolio-frontend
npm test -- --watch=false --code-coverage
```

**Specific backend test:**
```bash
cd portfolio-backend
./gradlew test --tests SkillServiceTest
```

**See also:** [Testing Guide](../../docs/development/testing.md)
