-- NetWatch - Initialisation MySQL
-- Ce script est exécuté au premier démarrage du conteneur MySQL

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- La base de données est créée via les variables d'environnement Docker
-- Ce script ajoute des optimisations et données initiales si besoin

USE netwatch;

-- Optimisations de performance
-- (Les tables sont créées par Hibernate via ddl-auto: update)
