# Documentation Contribution Guide

**Version:** 1.0.0
**Last Updated:** 2025-11-09
**Status:** Active

---

## 1. Overview

This guide defines the minimal standards for contributing to the Portfolio Application documentation. For comprehensive style guidelines, refer to the [Google Developer Documentation Style Guide](https://developers.google.com/style).


---

## 2. Document Structure

### 2.1 Required Header

Every documentation file must start with:

```markdown
# Document Title

**Document Type:** Guide | Reference | Operational | Deployment
**Version:** X.Y.Z
**Last Updated:** YYYY-MM-DD
**Status:** Active | Draft | Deprecated

## Table of Contents

1. [Section One](#1-section-one)
2. [Section Two](#2-section-two)

---

## 1. Section One

Content...
```

### 2.2 Section Numbering

Use hierarchical numbering:

```markdown
## 1. Introduction
### 1.1 Overview
### 1.2 Prerequisites

## 2. Installation
### 2.1 Requirements
### 2.2 Procedure
```

---

## 3. Writing Standards

### 3.1 Automated Validation

Documentation quality is automatically validated using [Vale](https://vale.sh/) with the Google Developer Documentation Style Guide.

**How it works:**
- Vale runs automatically in CI/CD on every pull request
- Checks all `.md` files against style rules
- Reports errors (blocking) and warnings (non-blocking)
- Reviews vocabulary and terminology consistency

**Running Vale locally:**

```bash
# Validate all documentation
./scripts/validate-docs.sh

# Validate specific file or directory
./scripts/validate-docs.sh docs/development/
./scripts/validate-docs.sh README.md
```

**Installation (for local validation):**

```bash
# macOS
brew install vale

# Linux
snap install vale

# Windows
choco install vale
```

**Customizing validation:**
- Add accepted terms to: `.vale/styles/Vocab/Portfolio/accept.txt`
- Add rejected terms to: `.vale/styles/Vocab/Portfolio/reject.txt`
- Adjust rule severity in: `.vale.ini`

**Note:** The Google style guide is downloaded automatically when running Vale for the first time.

### 3.2 Code Blocks

Always specify language:

````markdown
```bash
docker-compose up -d
```

```java
public class Example {
    // Code
}
```
````

### 3.3 Standard Patterns

**Prerequisites:**
```markdown
## Prerequisites

The following software is required:
- Java Development Kit (JDK) 24
- Docker Engine 20.10+
- Git 2.30+
```

**Commands with Output:**
````markdown
Execute the health check:

```bash
curl http://localhost:8080/actuator/health
```

Expected response:

```json
{
  "status": "UP"
}
```
````

**Error Documentation:**
```markdown
**Error:** `Connection refused`
**Cause:** Backend service not running
**Solution:** Restart the service using `docker restart`
```

**Notes and Warnings:**
```markdown
**Note:** This applies to production only.
**Warning:** This operation deletes all data.
**Important:** Create a backup first.
```

---

## 4. File Conventions

### 4.1 Naming

- Use lowercase with hyphens: `health-check-guide.md`
- Be descriptive: `database-migration-procedure.md`
- Always use `.md` extension

---

## 5. Version Control

### 5.1 Document Versioning

Follow Semantic Versioning:

- **Major (X.0.0):** Complete rewrite
- **Minor (X.Y.0):** New sections
- **Patch (X.Y.Z):** Corrections, clarifications

### 5.2 Change History

Include at document end:

```markdown
## Change History

| Version | Date       | Changes |
|---------|------------|---------|
| 1.0.0   | 2025-11-09 | Initial release |
```

---

## 6. Quality Assurance

### 6.1 Pre-submission Checklist

Before submitting documentation changes:

1. **Run Vale locally:**
   ```bash
   ./scripts/validate-docs.sh docs/
   ```

2. **Fix errors:** Address all error-level issues (required)

3. **Review warnings:** Consider addressing warning-level issues (recommended)

4. **Verify formatting:**
   - Required header present
   - Section numbering correct
   - Code blocks have language specified
   - Links are valid

5. **Test code samples:** Ensure all commands and code examples work

### 6.2 CI/CD Validation

The CI/CD pipeline automatically validates documentation:

- **Trigger:** On every push and pull request to `main`, `develop`, `dev`
- **Scope:** All `.md` files (README, docs/, CHANGELOG)
- **Tool:** Vale with Google Developer Documentation Style Guide
- **Result:** Annotations on GitHub pull requests with suggestions

**Workflow file:** `.github/workflows/vale-docs.yml`

---

## 7. Troubleshooting Vale

### 7.1 Common Issues

**False positives for technical terms:**

Add the term to `.vale/styles/Vocab/Portfolio/accept.txt`:

```
Angular
PostgreSQL
MyCustomTerm
```

**Rule too strict:**

Adjust severity in `.vale.ini`:

```ini
Google.Acronyms = suggestion  # Change from warning to suggestion
```

**Vale not running in CI/CD:**

1. Check workflow triggers in `.github/workflows/vale-docs.yml`
2. Verify file paths match what was changed
3. Check GitHub Actions logs for errors

---

## 8. External References

Primary style authority: [Google Developer Documentation Style Guide](https://developers.google.com/style)

**Key Sections to Review:**
- [Tone and Content](https://developers.google.com/style/tone)
- [Language and Grammar](https://developers.google.com/style/language)
- [Formatting](https://developers.google.com/style/formatting)
- [Computer Interfaces](https://developers.google.com/style/ui-elements)
- [Code Samples](https://developers.google.com/style/code-samples)

---

## Change History

| Version | Date       | Changes |
|---------|------------|---------|
| 1.1.0   | 2025-11-09 | Added Vale integration and automated validation |
| 1.0.0   | 2025-11-09 | Initial release |

---

**Document Type:** Reference
**Version:** 1.1.0
**Last Updated:** 2025-11-09
**Status:** Active
