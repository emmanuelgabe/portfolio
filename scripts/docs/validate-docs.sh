#!/bin/bash
# Script to validate documentation with Vale locally
# Usage: ./scripts/validate-docs.sh [path]
# Example: ./scripts/validate-docs.sh docs/

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default path
DOC_PATH="${1:-.}"

echo -e "${BLUE}Portfolio Documentation Validator${NC}"
echo "=================================="
echo ""

# Check if Vale is installed
if ! command -v vale &> /dev/null; then
    echo -e "${YELLOW}Vale is not installed.${NC}"
    echo ""
    echo "Install Vale:"
    echo "  macOS:   brew install vale"
    echo "  Linux:   snap install vale"
    echo "  Windows: choco install vale"
    echo ""
    echo "Or download from: https://vale.sh/docs/vale-cli/installation/"
    exit 1
fi

# Check if .vale.ini exists
if [ ! -f ".vale.ini" ]; then
    echo -e "${RED}Error: .vale.ini not found${NC}"
    echo "Run this script from the project root directory."
    exit 1
fi

# Download Google styles if not present
if [ ! -d ".vale/styles/Google" ]; then
    echo -e "${YELLOW}Downloading Google style guide...${NC}"
    mkdir -p .vale/styles
    cd .vale/styles
    wget -q https://github.com/errata-ai/Google/releases/latest/download/Google.zip
    unzip -q Google.zip -d Google
    rm Google.zip
    cd ../..
    echo -e "${GREEN}✓ Google style guide downloaded${NC}"
    echo ""
fi

# Run Vale
echo -e "${BLUE}Running Vale on: ${DOC_PATH}${NC}"
echo ""

if vale --config=.vale.ini "$DOC_PATH"; then
    echo ""
    echo -e "${GREEN}✓ Documentation validation passed!${NC}"
    exit 0
else
    echo ""
    echo -e "${YELLOW}⚠ Documentation has suggestions or warnings${NC}"
    echo ""
    echo "To ignore specific rules, update .vale.ini"
    echo "To add accepted terms, update .vale/styles/Vocab/Portfolio/accept.txt"
    exit 1
fi
