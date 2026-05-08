#!/bin/bash
# ============================================
# NetWatch - Script de déploiement
# Usage: ./deploy.sh [start|stop|restart|logs|status|update]
# ============================================

set -e

APP_NAME="NetWatch"
COMPOSE_FILE="docker-compose.yml"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log() { echo -e "${GREEN}[$(date '+%H:%M:%S')] $1${NC}"; }
warn() { echo -e "${YELLOW}[$(date '+%H:%M:%S')] ⚠ $1${NC}"; }
error() { echo -e "${RED}[$(date '+%H:%M:%S')] ✗ $1${NC}"; }
info() { echo -e "${BLUE}[$(date '+%H:%M:%S')] ℹ $1${NC}"; }

check_env() {
    if [ ! -f ".env" ]; then
        warn ".env non trouvé. Création depuis .env.example..."
        cp .env.example .env
        error "IMPORTANT: Configurez le fichier .env avant de continuer !"
        exit 1
    fi
}

case "${1:-help}" in

    start)
        log "Démarrage de $APP_NAME..."
        check_env
        docker compose -f $COMPOSE_FILE up -d --build
        log "Attente du démarrage de l'application..."
        sleep 5
        docker compose ps
        log "✅ $APP_NAME démarré sur http://localhost:$(grep SERVER_PORT .env | cut -d= -f2 || echo 8080)"
        info "Login par défaut: admin / Admin@2024! (changez après la première connexion)"
        ;;

    stop)
        log "Arrêt de $APP_NAME..."
        docker compose -f $COMPOSE_FILE down
        log "✅ $APP_NAME arrêté."
        ;;

    restart)
        log "Redémarrage de $APP_NAME..."
        docker compose -f $COMPOSE_FILE restart netwatch
        log "✅ Application redémarrée."
        ;;

    update)
        log "Mise à jour de $APP_NAME..."
        check_env
        docker compose -f $COMPOSE_FILE pull
        docker compose -f $COMPOSE_FILE up -d --build netwatch
        log "✅ Application mise à jour."
        ;;

    logs)
        docker compose -f $COMPOSE_FILE logs -f netwatch
        ;;

    logs-all)
        docker compose -f $COMPOSE_FILE logs -f
        ;;

    status)
        docker compose -f $COMPOSE_FILE ps
        echo ""
        info "URL application: http://localhost:$(grep SERVER_PORT .env 2>/dev/null | cut -d= -f2 || echo 8080)"
        ;;

    clean)
        warn "Suppression de tous les conteneurs et données !"
        read -p "Êtes-vous sûr ? (oui/non): " confirm
        if [ "$confirm" = "oui" ]; then
            docker compose -f $COMPOSE_FILE down -v --remove-orphans
            log "✅ Nettoyage complet effectué."
        fi
        ;;

    backup-db)
        BACKUP_FILE="netwatch_backup_$(date +%Y%m%d_%H%M%S).sql"
        log "Sauvegarde de la base de données..."
        docker exec netwatch-mysql mysqldump -u netwatch -pnetwatch123 netwatch > $BACKUP_FILE
        log "✅ Sauvegarde créée: $BACKUP_FILE"
        ;;

    *)
        echo ""
        echo "  $APP_NAME — Commandes disponibles:"
        echo ""
        echo "  ./deploy.sh start       Construire et démarrer tous les services"
        echo "  ./deploy.sh stop        Arrêter tous les services"
        echo "  ./deploy.sh restart     Redémarrer l'application"
        echo "  ./deploy.sh update      Reconstruire et déployer la nouvelle version"
        echo "  ./deploy.sh logs        Afficher les logs de l'application"
        echo "  ./deploy.sh status      Afficher le statut des conteneurs"
        echo "  ./deploy.sh backup-db   Sauvegarder la base de données"
        echo "  ./deploy.sh clean       Supprimer tous les conteneurs et données"
        echo ""
        ;;
esac
