#!/bin/bash
# Setup script to make all scripts executable
# Run this once after cloning the repository

echo "Setting execute permissions on all scripts..."

# Get the directory of this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Make all .sh files executable
find "$SCRIPT_DIR" -type f -name "*.sh" -exec chmod +x {} \;

# Also set permission on pre-commit hook
if [ -f "$SCRIPT_DIR/../.git/hooks/pre-commit" ]; then
    chmod +x "$SCRIPT_DIR/../.git/hooks/pre-commit"
    echo "✅ Pre-commit hook made executable"
fi

echo "✅ All scripts are now executable"
echo ""
echo "Script structure:"
echo "  deployment/  - Deployment and validation scripts"
echo "  maintenance/ - Cleanup and maintenance scripts"
echo "  testing/     - Health checks and testing scripts"
echo "  docs/        - Documentation validation scripts"
echo ""
echo "Run 'cat scripts/README.md' for more information"
