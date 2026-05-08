# 📡 NetWatch — Surveillance de Connexions Internet

Application de monitoring réseau développée pour **Ivoire Cartes Systemes**, permettant de surveiller 4 connexions internet avec alertes email et WhatsApp.

---

## ✨ Fonctionnalités

- **Ping automatique** des 4 adresses IP toutes les 60 secondes (configurable)
- **Dashboard temps réel** avec statut, latence et disponibilité
- **Alertes automatiques** par Email (SMTP) et WhatsApp (CallMeBot, gratuit)
- **Historique complet** des pings avec graphique de latence
- **Authentification sécurisée** avec gestion des rôles (Admin / User)
- **Gestion des utilisateurs** (création, activation, notifications)
- **Interface responsive** avec thème sombre professionnel
- **Déploiement Docker** en une commande

---

## 🚀 Déploiement rapide

### Prérequis
- Docker & Docker Compose installés
- Port 8080 disponible

### 1. Configurer l'environnement

```bash
cp .env.example .env
nano .env   # Remplissez vos paramètres
```

### 2. Démarrer l'application

```bash
chmod +x deploy.sh
./deploy.sh start
```

### 3. Accéder à l'application

```
http://localhost:8080
Login : admin
Mot de passe : Admin@2024!   ← À changer immédiatement !
```

---

## ⚙️ Configuration

### Variables d'environnement (.env)

| Variable | Description | Défaut |
|---|---|---|
| `MAIL_USERNAME` | Email expéditeur (Gmail) | — |
| `MAIL_PASSWORD` | App password Gmail | — |
| `PING_INTERVAL` | Intervalle entre pings (sec) | 60 |
| `ALERT_THRESHOLD` | Échecs avant alerte | 3 |
| `WHATSAPP_ENABLED` | Activer WhatsApp | false |
| `DB_PASSWORD` | Mot de passe MySQL | — |

### Email Gmail

1. Activez la vérification en 2 étapes sur votre compte Google
2. Créez un "Mot de passe d'application" : `myaccount.google.com/apppasswords`
3. Utilisez ce mot de passe dans `MAIL_PASSWORD`

### WhatsApp (CallMeBot — Gratuit)

1. Enregistrez `+34 644 44 21 26` dans vos contacts
2. Envoyez : `I allow callmebot to send me messages`
3. Notez la clé API reçue
4. Dans NetWatch → Admin → Utilisateurs → 🔔 → renseignez numéro + clé

---

## 📊 Adresses IP surveillées

| Nom | Adresse IP | Description |
|---|---|---|
| Connexion 1 - Principale | 160.154.207.178 | Lien principal |
| Connexion 2 - Backup | 105.235.6.210 | Lien de secours |
| Connexion 3 - Secondaire | 41.66.42.46 | Lien secondaire |
| Connexion 4 - Backup 2 | 105.235.6.163 | Backup secondaire |

---

## 🛠️ Commandes utiles

```bash
./deploy.sh start       # Démarrer
./deploy.sh stop        # Arrêter
./deploy.sh logs        # Voir les logs
./deploy.sh status      # Statut des conteneurs
./deploy.sh backup-db   # Sauvegarder la base
./deploy.sh update      # Mettre à jour
```

---

## 🔐 Sécurité

- Authentification par session Spring Security
- Mots de passe hashés BCrypt (force 12)
- Rôles : ADMIN (accès complet) / USER (lecture seule)
- Session expiration configurable
- Isolation réseau Docker

---

## 🏗️ Architecture technique

```
netwatch/
├── src/main/java/com/netwatch/
│   ├── config/          # Spring Security, Docker init
│   ├── controller/      # Dashboard, Admin, Auth
│   ├── entity/          # User, Link, PingResult, AlertLog
│   ├── repository/      # JPA Repositories
│   ├── scheduler/       # Monitoring automatique
│   └── service/         # PingService, NotificationService, UserService
├── src/main/resources/
│   └── templates/       # Thymeleaf HTML
├── Dockerfile
├── docker-compose.yml
└── deploy.sh
```

**Stack :** Spring Boot 3.2 • MySQL 8 • Thymeleaf • Docker

---

*Ivoire Cartes Systemes — Abidjan, Côte d'Ivoire*
