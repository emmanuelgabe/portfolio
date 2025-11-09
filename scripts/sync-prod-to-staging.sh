#!/bin/bash

# Script to copy production database to staging
# Usage: ./scripts/sync-prod-to-staging.sh

set -e  # Exit on error

echo "========================================="
echo "  Synchronization PROD → STAGING"
echo "========================================="

# Configuration
PROD_CONTAINER="portfolio-db-prod"
STAGING_CONTAINER="portfolio-db-staging"
DB_USER="postgres_app"
PROD_DB="portfolio_prod"
STAGING_DB="portfolio_staging"
BACKUP_DIR="/tmp/portfolio-backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="$BACKUP_DIR/prod_backup_$TIMESTAMP.sql"

# Create backup directory if it doesn't exist
mkdir -p "$BACKUP_DIR"

echo "Checking containers..."
if ! docker ps | grep -q "$PROD_CONTAINER"; then
    echo "❌ Error: Production container ($PROD_CONTAINER) is not running"
    exit 1
fi

if ! docker ps | grep -q "$STAGING_CONTAINER"; then
    echo "❌ Error: Staging container ($STAGING_CONTAINER) is not running"
    exit 1
fi

echo "✅ Containers found and running"

echo ""
echo "⚠️  WARNING: This operation will replace all staging data with production data."
read -p "Do you want to continue? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "❌ Operation cancelled"
    exit 0
fi

echo ""
echo "📦 Step 1/4: Exporting production database..."
docker exec -t "$PROD_CONTAINER" pg_dump -U "$DB_USER" -d "$PROD_DB" > "$BACKUP_FILE"
echo "✅ Export completed: $BACKUP_FILE ($(du -h "$BACKUP_FILE" | cut -f1))"

echo ""
echo "🔌 Step 2/4: Closing connections to staging..."
docker exec -t "$STAGING_CONTAINER" psql -U "$DB_USER" -d postgres -c \
    "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$STAGING_DB' AND pid <> pg_backend_pid();" || true

echo ""
echo "🗑️  Step 3/4: Dropping and recreating staging database..."
docker exec -t "$STAGING_CONTAINER" psql -U "$DB_USER" -d postgres -c "DROP DATABASE IF EXISTS $STAGING_DB;"
docker exec -t "$STAGING_CONTAINER" psql -U "$DB_USER" -d postgres -c "CREATE DATABASE $STAGING_DB OWNER $DB_USER;"
echo "✅ Staging database recreated"

echo ""
echo "📥 Step 4/4: Restoring data to staging..."
docker exec -i "$STAGING_CONTAINER" psql -U "$DB_USER" -d "$STAGING_DB" < "$BACKUP_FILE"
echo "✅ Restore completed"

echo ""
echo "🧹 Cleaning up old backups..."
ls -t "$BACKUP_DIR"/prod_backup_*.sql | tail -n +6 | xargs -r rm
echo "✅ Cleanup completed"

echo ""
echo "========================================="
echo "  ✅ SYNCHRONIZATION SUCCESSFUL"
echo "========================================="
echo "📊 Statistics:"
docker exec -t "$STAGING_CONTAINER" psql -U "$DB_USER" -d "$STAGING_DB" -c \
    "SELECT schemaname, tablename, n_live_tup as row_count FROM pg_stat_user_tables ORDER BY n_live_tup DESC LIMIT 10;"

echo ""
echo "💾 Backup saved: $BACKUP_FILE"
echo "🔗 Staging DB accessible on: localhost:5434"
echo ""
