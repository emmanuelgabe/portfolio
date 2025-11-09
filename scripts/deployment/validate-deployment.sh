#!/bin/bash
# Pre-commit validation script for Docker Compose configurations
# Usage: ./scripts/validate-deployment.sh [local|staging|prod]

set -e

ENV=${1:-local}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "========================================="
echo "Validating environment: $ENV"
echo "========================================="

# Environment-specific configuration
case $ENV in
  local)
    COMPOSE_FILE="docker-compose.local.yml"
    PROJECT_NAME="portfolio-local"
    PORT=8081
    DB_PORT=5435
    DB_NAME="portfolio_local"
    ;;
  staging)
    COMPOSE_FILE="docker-compose.staging.yml"
    PROJECT_NAME="portfolio-staging"
    PORT=3000
    DB_PORT=5434
    DB_NAME="portfolio_staging"
    ;;
  prod)
    COMPOSE_FILE="docker-compose.prod.yml"
    PROJECT_NAME="portfolio-prod"
    PORT=80
    DB_PORT=5432
    DB_NAME="portfolio_prod"
    ;;
  *)
    echo "❌ Invalid environment: $ENV (local|staging|prod)"
    exit 1
    ;;
esac

cd "$PROJECT_ROOT"

# Check 1: Docker Compose files exist
echo ""
echo "✓ Checking Docker Compose files..."
if [ ! -f "docker-compose.yml" ]; then
  echo "❌ docker-compose.yml not found"
  exit 1
fi
if [ ! -f "$COMPOSE_FILE" ]; then
  echo "❌ $COMPOSE_FILE not found"
  exit 1
fi
echo "✅ Docker Compose files present"

# Check 2: Validate YAML syntax
echo ""
echo "✓ Validating YAML syntax..."
docker-compose -f docker-compose.yml -f "$COMPOSE_FILE" config > /dev/null
echo "✅ YAML syntax valid"

# Check 3: Verify environment variables
echo ""
echo "✓ Checking environment variables..."
if [ -z "$DB_USER_PASSWORD" ]; then
  echo "⚠️  DB_USER_PASSWORD not set, using 'test' for validation"
  export DB_USER_PASSWORD="test"
fi
echo "✅ Environment variables configured"

# Check 4: Clean existing containers
echo ""
echo "✓ Cleaning existing containers..."
docker ps -a --filter "name=portfolio-.*-${ENV}" --format "{{.Names}}" | xargs -r docker stop 2>/dev/null || true
docker ps -a --filter "name=portfolio-.*-${ENV}" --format "{{.Names}}" | xargs -r docker rm 2>/dev/null || true
echo "✅ Environment cleaned"

# Check 5: Build and start containers
echo ""
echo "✓ Building and starting containers..."
docker-compose -p "$PROJECT_NAME" -f docker-compose.yml -f "$COMPOSE_FILE" up --build -d

# Check 6: Wait for containers to be healthy
echo ""
echo "✓ Waiting for containers to be healthy (max 180s)..."
MAX_ATTEMPTS=36
ATTEMPT=0

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
  ATTEMPT=$((ATTEMPT + 1))

  # Get health status
  DB_HEALTH=$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}running{{end}}' portfolio-db-${ENV} 2>/dev/null || echo "not_running")
  BACKEND_HEALTH=$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}running{{end}}' portfolio-backend-${ENV} 2>/dev/null || echo "not_running")
  FRONTEND_HEALTH=$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}running{{end}}' portfolio-frontend-${ENV} 2>/dev/null || echo "not_running")
  NGINX_RUNNING=$(docker ps --filter "name=portfolio-nginx-${ENV}" --format "{{.Names}}" 2>/dev/null)

  if [ -n "$NGINX_RUNNING" ]; then
    NGINX_HEALTH="running"
  else
    NGINX_HEALTH="not_running"
  fi

  echo "[$ATTEMPT/$MAX_ATTEMPTS] DB: $DB_HEALTH | Backend: $BACKEND_HEALTH | Frontend: $FRONTEND_HEALTH | Nginx: $NGINX_HEALTH"

  # Check if all are healthy
  if [ "$DB_HEALTH" = "healthy" ] && [ "$BACKEND_HEALTH" = "healthy" ] && [ "$FRONTEND_HEALTH" = "healthy" ] && [ "$NGINX_HEALTH" = "running" ]; then
    echo "✅ All containers are healthy!"
    break
  fi

  # Check for unhealthy status
  if [ "$DB_HEALTH" = "unhealthy" ] || [ "$BACKEND_HEALTH" = "unhealthy" ] || [ "$FRONTEND_HEALTH" = "unhealthy" ]; then
    echo "❌ One or more containers are unhealthy!"
    echo ""
    echo "Container logs:"
    [ "$DB_HEALTH" = "unhealthy" ] && docker logs --tail=50 portfolio-db-${ENV}
    [ "$BACKEND_HEALTH" = "unhealthy" ] && docker logs --tail=50 portfolio-backend-${ENV}
    [ "$FRONTEND_HEALTH" = "unhealthy" ] && docker logs --tail=50 portfolio-frontend-${ENV}
    exit 1
  fi

  if [ "$DB_HEALTH" = "not_running" ] || [ "$BACKEND_HEALTH" = "not_running" ] || [ "$FRONTEND_HEALTH" = "not_running" ] || [ "$NGINX_HEALTH" = "not_running" ]; then
    echo "⚠️ Some containers are not running yet, waiting..."
    docker ps -a --filter "name=portfolio-.*-${ENV}"
  fi

  sleep 5
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
  echo "❌ Timeout: containers did not become healthy"
  echo ""
  echo "Final status - DB: $DB_HEALTH | Backend: $BACKEND_HEALTH | Frontend: $FRONTEND_HEALTH | Nginx: $NGINX_HEALTH"
  echo ""
  echo "Container status:"
  docker ps -a --filter "name=portfolio-.*-${ENV}"
  echo ""
  echo "Container logs:"
  docker logs --tail=50 portfolio-db-${ENV} 2>&1 || true
  docker logs --tail=50 portfolio-backend-${ENV} 2>&1 || true
  docker logs --tail=50 portfolio-frontend-${ENV} 2>&1 || true
  docker logs --tail=50 portfolio-nginx-${ENV} 2>&1 || true
  exit 1
fi

# Check 7: Functional smoke tests
echo ""
echo "✓ Running functional tests..."

# Test 1: Frontend health endpoint
echo "  - Testing frontend health endpoint..."
FRONTEND_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$PORT/health.json || echo "000")
if [ "$FRONTEND_STATUS" = "200" ]; then
  echo "    ✅ Frontend health OK (HTTP $FRONTEND_STATUS)"
else
  echo "    ❌ Frontend health FAILED (HTTP $FRONTEND_STATUS)"
  exit 1
fi

# Test 2: Backend actuator health endpoint
echo "  - Testing backend actuator health endpoint..."
BACKEND_STATUS=$(docker exec portfolio-backend-${ENV} curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health || echo "000")
if [ "$BACKEND_STATUS" = "200" ]; then
  echo "    ✅ Backend health OK (HTTP $BACKEND_STATUS)"
else
  echo "    ❌ Backend health FAILED (HTTP $BACKEND_STATUS)"
  exit 1
fi

# Test 3: Database connectivity
echo "  - Testing database connectivity..."
DB_TEST=$(docker exec portfolio-db-${ENV} psql -U postgres_app -d $DB_NAME -c "SELECT 1;" 2>&1)
if echo "$DB_TEST" | grep -q "1 row"; then
  echo "    ✅ Database connectivity OK"
else
  echo "    ❌ Database connectivity FAILED"
  echo "$DB_TEST"
  exit 1
fi

# Test 4: Full stack integration
echo "  - Testing full stack integration..."
STACK_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$PORT/ || echo "000")
if [ "$STACK_STATUS" = "200" ] || [ "$STACK_STATUS" = "304" ]; then
  echo "    ✅ Full stack OK (HTTP $STACK_STATUS)"
else
  echo "    ❌ Full stack FAILED (HTTP $STACK_STATUS)"
  exit 1
fi

# Check 8: Final summary
echo ""
echo "========================================="
echo "✅ Validation successful for $ENV!"
echo "========================================="
echo ""
echo "Running containers:"
docker ps --filter "name=portfolio-.*-${ENV}" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""
echo "To clean up:"
echo "  docker-compose -p $PROJECT_NAME -f docker-compose.yml -f $COMPOSE_FILE down"
echo ""
echo "To view logs:"
echo "  docker logs -f portfolio-backend-${ENV}"
echo "  docker logs -f portfolio-frontend-${ENV}"
echo ""