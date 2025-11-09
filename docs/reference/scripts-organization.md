# Scripts Organization Reference

**Document Type:** Reference Guide
**Version:** 1.0.0
**Last Updated:** 2025-11-10
**Status:** Active

---

## Overview

All utility scripts are organized in the `scripts/` directory by category. This organization makes it easier to find and maintain scripts as the project grows.

## Directory Structure

```
scripts/
├── deployment/              # Deployment and environment synchronization
│   ├── validate-deployment.sh
│   └── sync-prod-to-staging.sh
├── maintenance/             # Cleanup and maintenance tasks
│   └── cleanup-staging.sh
├── testing/                 # Health checks and validation
│   └── test-health.sh
├── docs/                    # Documentation validation
│   └── validate-docs.sh
├── setup-permissions.sh     # Setup script (run once after clone)
└── README.md               # Detailed documentation
```

## Quick Reference

### Most Used Scripts

| Script | Location | Purpose | Usage |
|--------|----------|---------|-------|
| **validate-deployment.sh** | `deployment/` | Validate Docker config before commit | `./scripts/deployment/validate-deployment.sh staging` |
| **cleanup-staging.sh** | `maintenance/` | Emergency cleanup of stuck containers | `./scripts/maintenance/cleanup-staging.sh` |
| **test-health.sh** | `testing/` | Test health endpoints | `./scripts/testing/test-health.sh staging` |

### Using Makefile Instead

For convenience, use the Makefile commands instead of calling scripts directly:

```bash
# Validation
make validate-staging

# Testing
make test-staging

# Cleanup
make clean-staging
```

The Makefile automatically calls the appropriate scripts from their organized locations.

## Adding New Scripts

### 1. Choose the Correct Category

- **deployment/** - Anything related to deploying or promoting code
  - Example: deployment automation, environment setup, version tagging

- **maintenance/** - Cleanup and housekeeping tasks
  - Example: container cleanup, log rotation, disk space management

- **testing/** - Testing and validation
  - Example: health checks, smoke tests, integration tests

- **docs/** - Documentation tasks
  - Example: doc validation, doc generation, link checking

### 2. Create the Script

```bash
# Create new script in appropriate directory
touch scripts/testing/new-test.sh

# Make it executable
chmod +x scripts/testing/new-test.sh

# Or run the setup script to fix all permissions
./scripts/setup-permissions.sh
```

### 3. Follow Script Conventions

All scripts should follow these conventions:

```bash
#!/bin/bash
# Brief description of what this script does
# Usage: ./scripts/category/script-name.sh [args]

set -e  # Exit on error

# Your code here

# Use clear status messages
echo "✅ Success message"
echo "❌ Error message"
echo "⚠️ Warning message"
```

### 4. Update Documentation

After adding a script:

1. Update `scripts/README.md` - Add entry in the appropriate table
2. Update this file if needed - Add to quick reference if commonly used
3. Update Makefile - Add convenience command if appropriate

## Scripts and CI/CD Integration

### GitHub Actions

Scripts are used in `.github/workflows/ci-cd.yml`:

- Build step references scripts indirectly through Makefile
- Deploy step may call scripts directly for validation

### Git Hooks

Pre-commit hook (`.git/hooks/pre-commit`) calls:
- `scripts/deployment/validate-deployment.sh local`

### Makefile

All Makefile commands reference scripts in their organized locations.

## Troubleshooting

### "Permission denied" when running script

**Solution:**
```bash
# Fix permissions for all scripts
./scripts/setup-permissions.sh

# Or fix individual script
chmod +x scripts/deployment/validate-deployment.sh
```

### "Script not found" error

**Cause:** Script was moved but references weren't updated

**Check these files:**
- `Makefile`
- `.git/hooks/pre-commit`
- `.github/workflows/*.yml`

### Script organization changed

If you reorganize scripts:

1. Update all references in:
   - Makefile
   - Git hooks
   - GitHub Actions workflows
   - Documentation

2. Test all integration points:
   ```bash
   # Test Makefile commands
   make validate-staging

   # Test pre-commit hook (if installed)
   git add .
   git commit -m "test"  # Should trigger validation
   ```

## Future Improvements

Potential additions to the scripts directory:

- **scripts/database/** - Database-related scripts (migrations, backups, restores)
- **scripts/monitoring/** - Monitoring and alerting setup scripts
- **scripts/security/** - Security scanning and vulnerability checks
- **scripts/performance/** - Performance testing and benchmarking

Add these directories as needed when the project grows.

## See Also

- [Scripts README](../../scripts/README.md) - Detailed script documentation
- [CI/CD Guide](../deployment/ci-cd.md) - CI/CD pipeline documentation
- [Local Testing Guide](../deployment/local-testing.md) - Local testing workflows

---

**Document Type:** Reference Guide
**Version:** 1.0.0
**Last Updated:** 2025-11-10
**Status:** Active
