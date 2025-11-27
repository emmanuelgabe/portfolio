# Git Workflow

Branch strategy, commit conventions, and version management for the Portfolio project.

---

## Table of Contents

1. [Branch Strategy](#branch-strategy)
2. [Commit Conventions](#commit-conventions)
3. [Merge Strategy](#merge-strategy)
4. [Version Management](#version-management)

---

## Branch Strategy

### Main Branches

| Branch | Purpose | Protected |
|--------|---------|-----------|
| `main` | Production-ready code | Yes |
| `staging` | Pre-production testing | Yes |
| `dev` | Development integration | No |

### Branch Flow

```
feature/* ──> dev ──> staging ──> main
hotfix/*  ─────────────────────> main
```

### Feature Branches

**Naming Convention:** `feature/<ticket-id>-<short-description>`

**Examples:**
- `feature/123-add-dark-mode`
- `feature/456-user-authentication`
- `feature/789-image-optimization`

### Hotfix Branches

**Naming Convention:** `hotfix/<ticket-id>-<short-description>`

**Examples:**
- `hotfix/321-fix-login-error`
- `hotfix/654-security-patch`

---

## Commit Conventions

### Conventional Commits

Format: `<type>(<scope>): <description>`

### Types

| Type | Description |
|------|-------------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation changes |
| `style` | Code style (formatting, no logic change) |
| `refactor` | Code refactoring |
| `perf` | Performance improvement |
| `test` | Adding or updating tests |
| `chore` | Maintenance tasks |
| `ci` | CI/CD changes |
| `build` | Build system changes |

### Scopes

| Scope | Description |
|-------|-------------|
| `backend` | Backend changes |
| `frontend` | Frontend changes |
| `api` | API changes |
| `auth` | Authentication |
| `db` | Database |
| `docker` | Docker configuration |
| `docs` | Documentation |

### Examples

```bash
# Feature
feat(frontend): add dark mode toggle

# Bug fix
fix(backend): resolve JWT token expiration issue

# Documentation
docs(api): update authentication endpoints

# Refactoring
refactor(backend): extract image processing logic

# Tests
test(frontend): add unit tests for login component

# CI/CD
ci: add staging deployment workflow
```

### Commit Message Body

For complex changes, add a body:

```bash
feat(backend): add rate limiting for authentication

- Add AuthRateLimitFilter for login endpoint
- Configure Redis-backed rate limit storage
- Set default limit to 5 attempts per minute
- Add exponential backoff for repeated failures
```

---

## Merge Strategy

### Pull Request Flow

1. Create feature branch from `dev`
2. Implement changes
3. Run tests locally
4. Create Pull Request to `dev`
5. Code review
6. Merge to `dev`

### Merge Methods

| Target Branch | Method | Notes |
|---------------|--------|-------|
| `dev` | Squash merge | Clean history |
| `staging` | Merge commit | Track releases |
| `main` | Merge commit | Track releases |

### Pull Request Checklist

- [ ] Tests pass locally
- [ ] Code follows conventions
- [ ] Documentation updated if needed
- [ ] No sensitive data in commits
- [ ] Commit messages follow conventions

---

## Version Management

### Semantic Versioning

Format: `MAJOR.MINOR.PATCH`

| Component | Increment When |
|-----------|----------------|
| MAJOR | Breaking API changes |
| MINOR | New features (backward compatible) |
| PATCH | Bug fixes |

### Examples

- `1.0.0` - Initial release
- `1.1.0` - New feature added
- `1.1.1` - Bug fix
- `2.0.0` - Breaking change

### Version Locations

| File | Location |
|------|----------|
| Backend | `build.gradle` - `version` property |
| Frontend | `package.json` - `version` property |
| Changelog | `CHANGELOG.md` |

### Tagging Releases

```bash
# Create annotated tag
git tag -a v1.2.0 -m "Release version 1.2.0"

# Push tags
git push origin v1.2.0
```

---

## Related Documentation

- [CI/CD Guide](../deployment/ci-cd.md) - Deployment pipelines
- [Code Quality](./code-quality.md) - Quality checks
- [Contributing](../contributing.md) - Contribution guidelines
