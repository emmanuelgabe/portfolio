# Code Quality Tools

Configuration and usage of code quality tools for the Portfolio application.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Checkstyle](#2-checkstyle)
3. [SpotBugs](#3-spotbugs)
4. [JaCoCo](#4-jacoco)
5. [CI/CD Integration](#5-cicd-integration)
6. [Configuration Files](#6-configuration-files)

---

## 1. Overview

The Portfolio backend uses three main code quality tools:
- **Checkstyle**: Code style verification
- **SpotBugs**: Static analysis for bug detection
- **JaCoCo**: Test coverage measurement

### 1.1 Run All Checks

```bash
./gradlew check
```

Executes tests, Checkstyle, SpotBugs, and coverage verification.

---

## 2. Checkstyle

Code style verification enforcing project conventions based on Google Java Style Guide.

### 2.1 Usage

```bash
./gradlew checkstyleMain checkstyleTest
```

### 2.2 Configuration

| File | Purpose |
|------|---------|
| `config/checkstyle/checkstyle.xml` | Main rules configuration |
| `config/checkstyle/suppressions.xml` | Rule suppressions |

### 2.3 Reports

- **Location**: `build/reports/checkstyle/main.html`
- **Format**: HTML with violations listed

### 2.4 Critical Rules

**No Wildcard Imports**:
```java
// Wrong
import static org.mockito.Mockito.*;

// Correct
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
```

**Operator at Beginning of Line**:
```java
// Wrong
boolean isValid = condition1 &&
                 condition2;

// Correct
boolean isValid = condition1
                 && condition2;
```

---

## 3. SpotBugs

Static analysis for bug detection and code smells.

### 3.1 Usage

```bash
./gradlew spotbugsMain
```

### 3.2 Configuration

| Setting | Value |
|---------|-------|
| Effort | Maximum |
| Report Level | Medium |
| Exclusions | `config/spotbugs/spotbugs-exclude.xml` |

### 3.3 Reports

- **Location**: `build/reports/spotbugs/main.html`
- **Format**: HTML with bug categories

### 3.4 Common Bug Categories

| Category | Description |
|----------|-------------|
| `BAD_PRACTICE` | Violations of best practices |
| `CORRECTNESS` | Probable bugs |
| `PERFORMANCE` | Performance issues |
| `SECURITY` | Security vulnerabilities |
| `STYLE` | Code style issues |

---

## 4. JaCoCo

Test coverage measurement and enforcement.

### 4.1 Usage

```bash
# Generate coverage report
./gradlew test jacocoTestReport

# Verify coverage thresholds
./gradlew jacocoTestCoverageVerification
```

### 4.2 Reports

- **Location**: `build/reports/jacoco/test/html/index.html`
- **Format**: HTML with line and branch coverage

### 4.3 Coverage Thresholds

| Metric | Minimum |
|--------|---------|
| Global Line Coverage | 80% |
| Global Branch Coverage | 70% |
| Per-Package Coverage | 70% |

### 4.4 Exclusions

Excluded from coverage:
- Configuration classes (`*Config.java`)
- Main application class
- Entity classes (JPA)
- DTO classes
- MapStruct generated mappers

---

## 5. CI/CD Integration

### 5.1 Workflow

The `.github/workflows/backend-tests.yml` workflow runs on push/PR:

1. Checkstyle verification
2. SpotBugs analysis
3. Unit tests
4. JaCoCo coverage report
5. Coverage threshold verification

### 5.2 Build Failure

Build fails if:
- Checkstyle violations found
- SpotBugs bugs detected (medium+ severity)
- Coverage below thresholds
- Tests fail

---

## 6. Configuration Files

### 6.1 File Locations

| Tool | Configuration | Suppressions |
|------|---------------|--------------|
| Checkstyle | `config/checkstyle/checkstyle.xml` | `config/checkstyle/suppressions.xml` |
| SpotBugs | Build config | `config/spotbugs/spotbugs-exclude.xml` |
| JaCoCo | `build.gradle` | Inline exclusions |

### 6.2 Gradle Configuration

```gradle
// build.gradle
checkstyle {
    toolVersion = '10.x'
    configFile = file('config/checkstyle/checkstyle.xml')
}

spotbugs {
    effort = Effort.valueOf('MAX')
    reportLevel = Confidence.valueOf('MEDIUM')
    excludeFilter = file('config/spotbugs/spotbugs-exclude.xml')
}

jacoco {
    toolVersion = '0.8.x'
}
```

### 6.3 Exclusions Summary

All tools exclude:
- Generated code (`build/generated/`, `*MapperImpl.java`)
- Configuration classes
- Main application class

Checkstyle/SpotBugs also exclude:
- Entity classes
- DTO classes
- Exception classes

---

## Related Documentation

- [Testing Guide](./testing-guide.md) - Testing conventions
- [Setup Guide](./setup.md) - Development environment
- [Deployment: CI/CD](../deployment/ci-cd.md) - CI/CD pipeline
