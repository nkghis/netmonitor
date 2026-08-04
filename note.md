Puisque le code est maintenant sur GitHub, tu peux déployer par git pull sur le serveur au lieu de recopier le dossier — ça évitera au passage de réintroduire docker-compose.override.yml en production. Le rebuild forcé reste indispensable dans les deux cas :

git pull
docker compose build --no-cache netwatch
docker compose up -d