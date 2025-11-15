#!/bin/bash
# Emergency cleanup script for staging containers
# Run this on the mini-pc to clean up stuck containers

echo "========================================="
echo "Cleaning up staging containers"
echo "========================================="

# Stop all portfolio staging containers
echo "Stopping containers..."
docker stop portfolio-frontend-staging 2>/dev/null || true
docker stop portfolio-backend-staging 2>/dev/null || true
docker stop portfolio-db-staging 2>/dev/null || true
docker stop portfolio-nginx-staging 2>/dev/null || true

# Remove all portfolio staging containers
echo "Removing containers..."
docker rm portfolio-frontend-staging 2>/dev/null || true
docker rm portfolio-backend-staging 2>/dev/null || true
docker rm portfolio-db-staging 2>/dev/null || true
docker rm portfolio-nginx-staging 2>/dev/null || true

# Clean up any containers matching the pattern
echo "Cleaning up any remaining containers..."
docker ps -a --filter "name=portfolio-.*-staging" --format "{{.Names}}" | xargs -r docker stop 2>/dev/null || true
docker ps -a --filter "name=portfolio-.*-staging" --format "{{.Names}}" | xargs -r docker rm 2>/dev/null || true

# Also clean up "stage" pattern (in case of old deployments)
docker ps -a --filter "name=portfolio-.*-stage" --format "{{.Names}}" | xargs -r docker stop 2>/dev/null || true
docker ps -a --filter "name=portfolio-.*-stage" --format "{{.Names}}" | xargs -r docker rm 2>/dev/null || true

# Clean up dangling images
echo "Cleaning up dangling images..."
docker image prune -f

echo ""
echo "========================================="
echo "[SUCCESS] Cleanup complete!"
echo "========================================="
echo ""
echo "Current containers:"
docker ps -a --filter "name=portfolio" --format "table {{.Names}}\t{{.Status}}"