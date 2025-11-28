#!/bin/bash

# =============================================================================
# Script: generate-bcrypt-hash.sh
# Description: Generate BCrypt hash for admin password
# Usage: ./generate-bcrypt-hash.sh [password]
#        If no password provided, will prompt for input
# =============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "========================================"
echo "  BCrypt Password Hash Generator"
echo "========================================"
echo ""

# Get password
if [ -n "$1" ]; then
    PASSWORD="$1"
else
    echo -n "Enter password to hash: "
    read -s PASSWORD
    echo ""
    echo -n "Confirm password: "
    read -s PASSWORD_CONFIRM
    echo ""

    if [ "$PASSWORD" != "$PASSWORD_CONFIRM" ]; then
        echo -e "${RED}Error: Passwords do not match${NC}"
        exit 1
    fi
fi

# Validate password strength
if [ ${#PASSWORD} -lt 8 ]; then
    echo -e "${RED}Error: Password must be at least 8 characters${NC}"
    exit 1
fi

echo ""
echo "Generating BCrypt hash..."
echo ""

# Method 1: Python with bcrypt module
if command -v python3 &> /dev/null; then
    HASH=$(python3 -c "
import sys
try:
    import bcrypt
    password = '''$PASSWORD'''.encode('utf-8')
    salt = bcrypt.gensalt(rounds=10)
    hashed = bcrypt.hashpw(password, salt)
    print(hashed.decode('utf-8'))
except ImportError:
    print('BCRYPT_NOT_INSTALLED', file=sys.stderr)
    sys.exit(1)
except Exception as e:
    print(f'ERROR: {e}', file=sys.stderr)
    sys.exit(1)
" 2>&1)

    if [[ "$HASH" == \$2* ]]; then
        echo -e "${GREEN}BCrypt hash generated successfully:${NC}"
        echo ""
        echo "$HASH"
        echo ""
        echo "========================================"
        echo "Add to your .env file:"
        echo "========================================"
        echo ""
        echo "ADMIN_PASSWORD_HASH=$HASH"
        echo ""
        exit 0
    elif [[ "$HASH" == *"BCRYPT_NOT_INSTALLED"* ]]; then
        echo -e "${YELLOW}Python bcrypt module not installed.${NC}"
        echo "Installing bcrypt..."
        pip install bcrypt

        # Retry after installation
        HASH=$(python3 -c "
import bcrypt
password = '''$PASSWORD'''.encode('utf-8')
salt = bcrypt.gensalt(rounds=10)
hashed = bcrypt.hashpw(password, salt)
print(hashed.decode('utf-8'))
" 2>&1)

        if [[ "$HASH" == \$2* ]]; then
            echo -e "${GREEN}BCrypt hash generated successfully:${NC}"
            echo ""
            echo "$HASH"
            echo ""
            echo "========================================"
            echo "Add to your .env file:"
            echo "========================================"
            echo ""
            echo "ADMIN_PASSWORD_HASH=$HASH"
            echo ""
            exit 0
        fi
    fi
fi

# Method 2: Python with passlib (fallback)
if command -v python3 &> /dev/null; then
    HASH=$(python3 -c "
import sys
try:
    from passlib.hash import bcrypt
    hashed = bcrypt.using(rounds=10).hash('''$PASSWORD''')
    print(hashed)
except ImportError:
    sys.exit(1)
" 2>/dev/null)

    if [[ "$HASH" == \$2* ]]; then
        echo -e "${GREEN}BCrypt hash generated successfully:${NC}"
        echo ""
        echo "$HASH"
        echo ""
        echo "========================================"
        echo "Add to your .env file:"
        echo "========================================"
        echo ""
        echo "ADMIN_PASSWORD_HASH=$HASH"
        echo ""
        exit 0
    fi
fi

# Method 3: htpasswd (Apache utils) - mainly for Linux
if command -v htpasswd &> /dev/null; then
    HASH=$(htpasswd -nbBC 10 "" "$PASSWORD" 2>/dev/null | tr -d ':\n' | sed 's/\$2y\$/\$2a\$/')

    if [[ "$HASH" == \$2* ]]; then
        echo -e "${GREEN}BCrypt hash generated successfully:${NC}"
        echo ""
        echo "$HASH"
        echo ""
        echo "========================================"
        echo "Add to your .env file:"
        echo "========================================"
        echo ""
        echo "ADMIN_PASSWORD_HASH=$HASH"
        echo ""
        exit 0
    fi
fi

# If all methods fail, provide instructions
echo -e "${YELLOW}Could not generate hash automatically.${NC}"
echo ""
echo "To fix this, run:"
echo -e "${GREEN}  pip install bcrypt${NC}"
echo ""
echo "Then run this script again."
echo ""
echo "Or generate manually with Python:"
echo -e "${GREEN}  python3 -c \"import bcrypt; print(bcrypt.hashpw(b'YourPassword', bcrypt.gensalt(10)).decode())\"${NC}"
echo ""
echo "Or use an online BCrypt generator (10 rounds):"
echo "  - https://bcrypt-generator.com/"
echo ""

exit 1
