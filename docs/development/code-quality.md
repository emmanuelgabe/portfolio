# Code Quality Tools

## Backend Tools

### Checkstyle
Code style verification enforcing project conventions.

```bash
./gradlew checkstyleMain checkstyleTest
```

**Configuration**: `config/checkstyle/checkstyle.xml`
**Report**: `build/reports/checkstyle/main.html`
**Version**: See `build.gradle` line 73

### SpotBugs
Static analysis for bug detection and code smells.

```bash
./gradlew spotbugsMain
```

**Configuration**: `config/spotbugs/spotbugs-exclude.xml`
**Report**: `build/reports/spotbugs/main.html`
**Settings**: Maximum effort, medium report level
**Version**: See `build.gradle` line 89

### JaCoCo
Test coverage measurement and enforcement.

```bash
./gradlew test jacocoTestReport
./gradlew jacocoTestCoverageVerification
```

**Report**: `build/reports/jacoco/test/html/index.html`
**Configuration**: See `build.gradle` lines 108-167 for:
- Coverage thresholds
- Exclusions (config, entity, dto, mapper, etc.)
- Report formats

### Run All Checks
```bash
./gradlew check
```
Executes tests, Checkstyle, SpotBugs, and coverage verification.

## CI/CD Integration

Workflow `.github/workflows/backend-tests.yml` runs on push/PR to main, develop, dev branches:
1. Checkstyle verification
2. SpotBugs analysis
3. Unit tests
4. JaCoCo coverage report
5. Coverage threshold verification

Build fails if any check fails.

## Exclusions

All tools exclude:
- Generated code (`build/generated/`, `*MapperImpl.java`)
- Configuration classes
- Main application class

Checkstyle/SpotBugs also exclude:
- Entity classes (JPA-specific patterns)
- DTO classes (data carriers)
- Exception classes

## Configuration Files

| Tool | Configuration | Suppressions |
|------|---------------|--------------|
| Checkstyle | `config/checkstyle/checkstyle.xml` | `config/checkstyle/suppressions.xml` |
| SpotBugs | - | `config/spotbugs/spotbugs-exclude.xml` |
| JaCoCo | `build.gradle` (lines 108-149) | `build.gradle` exclusions |

See `config/README.md` for modification instructions.

## Related Files

- Build configuration: `portfolio-backend/build.gradle`
- CI workflow: `.github/workflows/backend-tests.yml`
- Checkstyle rules: `config/checkstyle/checkstyle.xml`
- SpotBugs exclusions: `config/spotbugs/spotbugs-exclude.xml`
