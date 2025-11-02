# Version Management Reference

**Document Type:** Technical Reference
**Version:** 1.0.0
**Last Updated:** 2025-11-09
**Status:** Active

---

## Table of Contents

1. [Overview](#1-overview)
2. [Versioning Strategy](#2-versioning-strategy)
3. [Frontend Versioning](#3-frontend-versioning)
4. [Backend Versioning](#4-backend-versioning)
5. [CI/CD Integration](#5-cicd-integration)
6. [Release Procedures](#6-release-procedures)
7. [Best Practices](#7-best-practices)
8. [Related Documentation](#8-related-documentation)

---

## 1. Overview

Automated versioning system using Git tags. Version numbers are generated automatically for frontend and backend components.

**Principle:**
```
Git Tag → Automated Generation → Build → Deployment
```

**Version Sources:**

| Component | Source | Example |
|-----------|--------|---------|
| Frontend | Git tags (automatic) | `0.0.1-SNAPSHOT` |
| Backend | `build.gradle` (manual) | `0.0.1-SNAPSHOT` |
| Docker Images | CI/CD pipeline | `v1.2.3` |

---

## 2. Versioning Strategy

Follows Semantic Versioning (SemVer) 2.0.0: `MAJOR.MINOR.PATCH[-PRERELEASE]`

| Element | When to Increment |
|---------|------------------|
| MAJOR | Incompatible API changes |
| MINOR | Backward-compatible functionality |
| PATCH | Backward-compatible bug fixes |

**Git Describe Output:**

| Scenario | Output | Meaning |
|----------|--------|---------|
| Exact tag | `v1.2.3` | HEAD at tag v1.2.3 |
| After tag | `v1.2.3-5-ga1b2c3d` | 5 commits after tag |
| No tags | `a1b2c3d` | Commit hash only |
| Uncommitted changes | `v1.2.3-dirty` | Working directory modified |

---

## 3. Frontend Versioning

### 3.1 Configuration

**Base version:** `portfolio-frontend/package.json`

```json
{
  "version": "0.0.1"
}
```

**Generation script:** `portfolio-frontend/scripts/generate-version.js`

### 3.2 Automatic Generation

Generated file: `portfolio-frontend/src/environments/version.ts`

```typescript
export const VERSION = 'v1.2.3-5-ga1b2c3d';
```

**Triggers:**

| Event | Command |
|-------|---------|
| Development | `npm start` |
| Build | `npm run build` |
| Docker build | Dockerfile execution |

### 3.3 Usage

```typescript
import { VERSION } from '../environments/version';

@Component({
  selector: 'app-root',
  template: `<footer>Version: {{ version }}</footer>`
})
export class AppComponent {
  version = VERSION;
}
```

**Health endpoint:**

```bash
curl http://localhost:4200/health.json
```

---

## 4. Backend Versioning

### 4.1 Configuration

**File:** `portfolio-backend/build.gradle`

```gradle
version = '0.0.1-SNAPSHOT'
```

### 4.2 Access via Actuator

**Endpoint:** `/actuator/info`

```bash
curl http://localhost:8080/actuator/info
```

**Response:**

```json
{
  "build": {
    "version": "0.0.1-SNAPSHOT",
    "artifact": "portfolio-backend"
  }
}
```


---

## 5. CI/CD Integration

### 5.1 Version Extraction

**Workflow:** `.github/workflows/ci-cd.yml`

```yaml
- name: Extract version from Git tags
  run: |
    VERSION=$(git describe --tags --always --dirty 2>/dev/null || echo "0.0.1-SNAPSHOT")
    echo "VERSION=$VERSION" >> $GITHUB_OUTPUT
```

### 5.2 Docker Image Tagging

Images tagged with multiple tags after deployment:

```
portfolio-backend:v1.2.3
portfolio-backend:latest
portfolio-backend:prod-latest
```

### 5.3 Git Directory Requirement

CI/CD pipeline copies `.git` directory to deployment location for frontend version generation.

---

## 6. Release Procedures

### 6.1 Standard Release

```bash
git add .
git commit -m "feat: implement new feature"
git tag -a v1.1.0 -m "Release v1.1.0: New feature description"
git push origin main v1.1.0
```

### 6.2 Release Candidate

```bash
git tag -a v1.1.0-rc1 -m "Release Candidate 1 for v1.1.0"
git push origin v1.1.0-rc1
```

### 6.3 Hotfix Release

```bash
git tag -a v1.0.1 -m "Hotfix: Critical bug correction"
git push origin v1.0.1
```

### 6.4 Manual Version Update

**Frontend:**
```bash
# Edit portfolio-frontend/package.json → "version": "1.1.0"
npm run version:generate
npm run build
```

**Backend:**
```bash
# Edit portfolio-backend/build.gradle → version = '1.1.0'
./gradlew clean bootJar
```

### 6.5 Version Verification

```bash
# Git version
git describe --tags --always --dirty

# Frontend version
cat portfolio-frontend/src/environments/version.ts

# Backend version
curl http://localhost:8080/actuator/info | jq '.build.version'

# Docker images
docker images | grep portfolio
```

---

## 7. Best Practices

### 7.1 Tagging Standards

Use annotated tags with descriptive messages:

```bash
# Good
git tag -a v1.0.0 -m "Release 1.0.0: Add authentication and error handling"

# Avoid
git tag v1.0.0
```

### 7.2 Version Synchronization

- Update `build.gradle` manually before creating tag
- Use Git tags for automated versioning
- Tag Docker images via CI/CD pipeline
- Maintain `CHANGELOG.md` for release notes

---

## 8. Related Documentation

- [Semantic Versioning Specification](https://semver.org/)
- [CI/CD Deployment Guide](../deployment/ci-cd.md)

---

## Change History

| Version | Date       | Changes |
|---------|------------|---------|
| 1.0.0   | 2025-11-09 | Initial release |

---

**Document Type:** Technical Reference
**Version:** 1.0.0
**Last Updated:** 2025-11-09
**Status:** Active
