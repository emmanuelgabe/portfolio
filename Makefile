.PHONY: help validate-local validate-staging validate-prod test-local test-staging test-prod clean-local clean-staging clean-prod

help:
	@echo "Commandes disponibles pour tester le déploiement localement:"
	@echo ""
	@echo "  make validate-local     - Valider la configuration local"
	@echo "  make validate-staging   - Valider la configuration staging"
	@echo "  make validate-prod      - Valider la configuration production"
	@echo ""
	@echo "  make test-local         - Démarrer l'environnement local"
	@echo "  make test-staging       - Démarrer l'environnement staging"
	@echo "  make test-prod          - Démarrer l'environnement production"
	@echo ""
	@echo "  make clean-local        - Nettoyer l'environnement local"
	@echo "  make clean-staging      - Nettoyer l'environnement staging"
	@echo "  make clean-prod         - Nettoyer l'environnement production"
	@echo ""
	@echo "Exemples:"
	@echo "  make validate-staging   # Valide staging avant de commit"
	@echo "  make test-staging       # Lance staging localement"
	@echo "  make clean-staging      # Nettoie staging local"

# Validation complète avec tests
validate-local:
	@chmod +x scripts/deployment/validate-deployment.sh
	@scripts/deployment/validate-deployment.sh local

validate-staging:
	@chmod +x scripts/deployment/validate-deployment.sh
	@scripts/deployment/validate-deployment.sh staging

validate-prod:
	@chmod +x scripts/deployment/validate-deployment.sh
	@scripts/deployment/validate-deployment.sh prod

# Démarrage simple sans validation
test-local:
	@echo "Démarrage de l'environnement local..."
	@DB_USER_PASSWORD=${DB_USER_PASSWORD:-test} docker-compose -p portfolio-local -f docker-compose.yml -f docker-compose.local.yml up --build -d
	@echo "✅ Environnement local démarré sur http://localhost:8081"

test-staging:
	@echo "Démarrage de l'environnement staging..."
	@DB_USER_PASSWORD=${DB_USER_PASSWORD:-test} docker-compose -p portfolio-staging -f docker-compose.yml -f docker-compose.staging.yml up --build -d
	@echo "✅ Environnement staging démarré sur http://localhost:3000"

test-prod:
	@echo "Démarrage de l'environnement production..."
	@DB_USER_PASSWORD=${DB_USER_PASSWORD:-test} docker-compose -p portfolio-prod -f docker-compose.yml -f docker-compose.prod.yml up --build -d
	@echo "✅ Environnement production démarré sur http://localhost:80"

# Nettoyage
clean-local:
	@echo "Nettoyage de l'environnement local..."
	@docker-compose -p portfolio-local -f docker-compose.yml -f docker-compose.local.yml down -v
	@echo "✅ Environnement local nettoyé"

clean-staging:
	@echo "Nettoyage de l'environnement staging..."
	@docker-compose -p portfolio-staging -f docker-compose.yml -f docker-compose.staging.yml down -v
	@echo "✅ Environnement staging nettoyé"

clean-prod:
	@echo "Nettoyage de l'environnement production..."
	@docker-compose -p portfolio-prod -f docker-compose.yml -f docker-compose.prod.yml down -v
	@echo "✅ Environnement production nettoyé"

# Nettoyage complet de tous les environnements
clean-all: clean-local clean-staging clean-prod
	@echo "✅ Tous les environnements nettoyés"

# Afficher les logs
logs-local:
	@docker-compose -p portfolio-local -f docker-compose.yml -f docker-compose.local.yml logs -f

logs-staging:
	@docker-compose -p portfolio-staging -f docker-compose.yml -f docker-compose.staging.yml logs -f

logs-prod:
	@docker-compose -p portfolio-prod -f docker-compose.yml -f docker-compose.prod.yml logs -f

# Vérifier le statut des conteneurs
status:
	@echo "Conteneurs en cours d'exécution:"
	@docker ps --filter "name=portfolio-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"