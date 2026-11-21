# 🎓 StudyAI — Plateforme Éducative Intelligente

> Plateforme d'apprentissage assistée par IA pour étudiants, avec panneau d'administration centralisé.

---

## 📋 Table des matières

1. [Description](#-description)
2. [Architecture du système](#-architecture-du-système)
3. [Prérequis](#-prérequis)
4. [Démarrage rapide (Docker)](#-démarrage-rapide-docker)
5. [Démarrage en développement](#-démarrage-en-développement)
6. [Comptes de test](#-comptes-de-test)
7. [Endpoints API](#-endpoints-api)
8. [Variables d'environnement](#-variables-denvironnement)
9. [Structure du projet](#-structure-du-projet)

---

## 📖 Description

**StudyAI** est une solution éducative moderne combinant la puissance de l'Intelligence Artificielle générative et des technologies web robustes afin de transformer l'expérience d'apprentissage des étudiants. 

Fonctionnalités clés :
- 📚 **Gestion de cours & documents** : Dépôt et indexation de cours (supports PDF, textes).
- 🧠 **Tuteur IA Interactif & RAG** : Réponses contextualisées basées sur les documents de cours grâce au Retrieval-Augmented Generation (RAG) et au modèle Google Gemini.
- 📝 **Génération automatique de résumés & synthèses** : Extraction des points essentiels pour faciliter les révisions.
- 🎯 **Quiz & Cartes mémoire (Flashcards)** : Évaluations adaptatives et révision espacée.
- 🛡️ **Espace d'administration** : Statistiques calculées depuis les données persistées et gestion des utilisateurs.

Les cours, textes extraits, conversations, synthèses, quiz, cartes mémoire et résultats sont enregistrés dans PostgreSQL. Les migrations Flyway ajoutent les tables et colonnes nécessaires sans modifier les paramètres de connexion existants. Les documents binaires sont stockés dans le répertoire configuré par `UPLOAD_DIR`.

Pour utiliser les fonctions IA, renseignez `GEMINI_API_KEY` avant de démarrer le service IA. Sans clé, les fonctions de cours et de dépôt restent disponibles et les requêtes IA renvoient une erreur explicite.

---

## 🏗️ Architecture du système

L'application repose sur une architecture de microservices conteneurisée :

```text
                               +--------------------------------------------------+
                               |                 Navigateur Web                   |
                               +--------------------------------------------------+
                                        |                             |
                       Requêtes Web (HTTP:80)             Appels API directs (Dev)
                                        |                             |
                                        v                             v
                        +-------------------------------+   +--------------------+
                        |       Frontend (Angular)      |   |   Swagger UI / Dev |
                        |       Nginx Reverse-Proxy     |   +--------------------+
                        +-------------------------------+              |
                                        | (Proxy /api)                 |
                                        +-------------------+----------+
                                                            |
                                                            v
                                            +--------------------------------+
                                            |     Backend Spring Boot        |
                                            |           (Port 8080)          |
                                            +--------------------------------+
                                                    |                 |
                             Persistance SQL (JPA)  |                 | Appels REST Internes
                                                    v                 v
                                +-----------------------+   +--------------------+
                                |  Base de Données      |   |  Service IA        |
                                |  PostgreSQL 16        |   |  FastAPI (Python)  |
                                |  (Port 5433)          |   |  (Port 8000)       |
                                +-----------------------+   +--------------------+
                                                                      |
                                                                      | Google Gemini API
                                                                      v
                                                            +--------------------+
                                                            | Cloud Google GenAI |
                                                            +--------------------+
```

### Services et responsabilités

- **Frontend (`frontend`)** : Application Single Page en Angular 17+ servie via Nginx (port 80). Interface moderne pour étudiants et administrateurs.
- **Backend (`backend`)** : API REST sécurisée sous Spring Boot 3 (Java 17) (port 8080). Gère l'authentification JWT, la logique métier, la persistance des données et l'orchestration.
- **Service IA (`ai-service`)** : Microservice Python FastAPI (port 8000). Responsable du pipeline RAG, de l'extraction des PDF, des embeddings vectoriels et des interactions avec l'API Gemini.
- **Base de données (`postgres`)** : PostgreSQL 16 Alpine (port 5433) pour le stockage relationnel (utilisateurs, cours, quiz, statistiques).

---

## ✅ Prérequis

Pour exécuter et développer sur StudyAI, assurez-vous d'avoir installé les outils suivants selon votre mode d'exécution :

### Mode Docker (Recommandé)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (version 24.0+)
- [Docker Compose](https://docs.docker.com/compose/) (v2.20+ ou intégré avec Docker Desktop)

### Mode Développement Local
- **Node.js** : v20.x ou supérieure (avec npm v10+)
- **Angular CLI** : `npm install -g @angular/cli`
- **Java JDK** : version 17 (OpenJDK ou Oracle JDK)
- **Apache Maven** : version 3.9+
- **Python** : version 3.11+ (avec `pip` et `venv`)
- **PostgreSQL** : version 16 (ou via conteneur Docker)

---

## 🚀 Démarrage rapide (Docker)

Le moyen le plus simple et rapide de lancer l'intégralité de la plateforme est d'utiliser Docker Compose.

### 1. Cloner le projet
```bash
git clone https://github.com/votre-organisation/studyai.git
cd studyai
```

### 2. Configurer les variables d'environnement
Copiez le modèle de configuration et éditez-le avec vos identifiants :
```bash
cp .env.example .env
```
> [!IMPORTANT]
> Pour activer les fonctionnalités intelligentes de l'IA, définissez une clé API valide pour `GEMINI_API_KEY` dans votre fichier `.env`. Obtenez une clé gratuitement sur [Google AI Studio](https://aistudio.google.com/app/apikey).

### 3. Démarrer tous les services
```bash
docker-compose up --build
```
Pour exécuter les conteneurs en arrière-plan (mode détaché) :
```bash
docker-compose up -d --build
```

### 4. Accéder à l'application

| Composant | URL | Description |
|-----------|-----|-------------|
| **Frontend Web** | [http://localhost](http://localhost) | Application Angular pour étudiants et admins |
| **API Backend** | [http://localhost:8080](http://localhost:8080) | Point d'entrée de l'API REST Spring Boot |
| **Documentation Swagger UI** | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | Documentation interactive des endpoints backend |
| **Service IA** | [http://localhost:8000](http://localhost:8000) | Microservice FastAPI |
| **Documentation Service IA** | [http://localhost:8000/docs](http://localhost:8000/docs) | Swagger interactif FastAPI |

### 5. Arrêter les services
```bash
docker-compose down
```
Pour réinitialiser les volumes de données (effacer la base de données PostgreSQL) :
```bash
docker-compose down -v
```

---

## 💻 Démarrage en développement

Si vous souhaitez modifier le code source et bénéficier du rechargement à chaud (*hot-reload*), lancez chaque service individuellement.

### 1. Base de données PostgreSQL
Vous pouvez lancer uniquement le conteneur PostgreSQL via Docker :
```bash
docker-compose up -d postgres
```

### 2. Service IA (FastAPI)
```bash
cd ai-service

# Création et activation de l'environnement virtuel
python -m venv venv

# Sous Windows (PowerShell) :
.\venv\Scripts\Activate.ps1
# Sous Linux / macOS :
# source venv/bin/activate

# Installation des dépendances
pip install -r requirements.txt

# Démarrage avec rechargement automatique
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

### 3. Backend (Spring Boot)
```bash
cd backend

# Sous Windows :
mvnw.cmd spring-boot:run

# Sous Linux / macOS :
./mvnw spring-boot:run
```
*Le backend sera accessible sur `http://localhost:8080`.*

### 4. Frontend (Angular)
```bash
cd frontend

# Installation des dépendances npm
npm install

# Lancement du serveur de développement
ng serve --open
```
*L'application sera accessible sur `http://localhost:4200`.*

---

## 👤 Comptes de test

À l'initialisation de la base de données, des comptes pré-configurés sont disponibles pour les tests :

| Rôle | Adresse Email | Mot de passe | Permissions |
|------|---------------|--------------|-------------|
| **Administrateur** | `admin@studyai.com` | `Admin@1234` | Accès complet au panneau admin, gestion des utilisateurs, statistiques système |
| **Étudiant (démo)** | `student@studyai.com` | `Student@1234` | Accès aux cours, tuteur IA, quiz, révisions et flashcards |
| **Nouvel Étudiant** | *Libre* | *Libre* | Créer directement un compte via la page d'inscription `/auth/register` |

---

## 📡 Endpoints API

### Backend Spring Boot (`http://localhost:8080`)

#### Authentification & profils
| Méthode | URL | Description | Accès |
|---------|-----|-------------|-------|
| `POST` | `/api/users/register` | Inscription d'un étudiant | Public |
| `POST` | `/api/auth/login` | Connexion | Public |
| `POST` | `/api/auth/refresh` | Renouvellement du jeton | Public |
| `POST` | `/api/auth/logout` | Déconnexion | Authentifié |
| `GET` / `PUT` | `/api/users/profile` | Consulter ou modifier son profil | Authentifié |
| `PUT` | `/api/users/password` | Modifier son mot de passe | Authentifié |

#### Cours & documents
| Méthode | URL | Description | Accès |
|---------|-----|-------------|-------|
| `GET` | `/api/courses` | Liste des cours personnels et de leurs documents | Authentifié |
| `POST` | `/api/courses` | Création d'un cours (`title`, `subject`, `description`) | Authentifié |
| `DELETE` | `/api/courses/{courseId}` | Suppression d'un cours et de ses données associées | Propriétaire |
| `POST` | `/api/courses/{courseId}/files` | Dépôt PDF ou TXT (multipart `file`, 10 Mo max) et extraction | Propriétaire |
| `DELETE` | `/api/courses/{courseId}/files/{fileId}` | Suppression d'un document | Propriétaire |

#### Tuteur IA, synthèses & révisions
| Méthode | URL | Description | Accès |
|---------|-----|-------------|-------|
| `POST` | `/api/study/courses/{courseId}/messages` | Poser une question au tuteur sur le cours | Propriétaire |
| `GET` | `/api/study/courses/{courseId}/messages` | Historique sauvegardé de la conversation | Propriétaire |
| `POST` | `/api/study/courses/{courseId}/summary` | Générer et enregistrer une synthèse | Propriétaire |
| `POST` | `/api/study/courses/{courseId}/quiz` | Générer un quiz (JSON `{"count":3}` à `{"count":15}`) | Propriétaire |
| `POST` | `/api/study/courses/{courseId}/flashcards` | Générer des cartes mémoire | Propriétaire |
| `GET` | `/api/study/resources` | Liste des synthèses et activités sauvegardées | Authentifié |
| `POST` | `/api/study/resources/{resourceId}/attempts` | Enregistrer et corriger une activité | Propriétaire |

#### Administration
| Méthode | URL | Description | Accès |
|---------|-----|-------------|-------|
| `GET` | `/api/admin/users` | Liste paginée et filtrable des étudiants | Admin |
| `PUT` / `DELETE` | `/api/admin/users/{id}` | Modifier ou supprimer un compte | Admin |
| `PUT` | `/api/admin/users/{id}/status` | Modifier le statut d'un compte | Admin |
| `GET` | `/api/admin/stats` | Statistiques agrégées des utilisateurs et activités | Admin |

---

### Service IA FastAPI (`http://localhost:8000`)

| Méthode | URL | Description | Accès |
|---------|-----|-------------|-------|
| `GET`  | `/health` | Statut du service IA et configuration du modèle Gemini | Public / Interne |
| `POST` | `/extract` | Extraction de texte PDF/TXT (appel interne backend) | Interne |
| `POST` | `/generate` | Génération contextualisée (appel interne backend) | Interne |
| `GET`  | `/` | Informations sur le service et liens vers la documentation | Public |
| `GET`  | `/docs` | Interface Swagger interactive OpenAPI | Public |
| `GET`  | `/redoc` | Documentation ReDoc | Public |

---

## ⚙️ Variables d'environnement

Toutes les variables sont configurées dans le fichier `.env` à la racine :

| Variable | Description | Valeur par défaut |
|----------|-------------|-------------------|
| `POSTGRES_DB` | Nom de la base de données PostgreSQL | `studyai` |
| `POSTGRES_USER` | Utilisateur de la base de données | `studyai` |
| `POSTGRES_PASSWORD` | Mot de passe de la base de données | `changeme_strong_password_here` |
| `JWT_SECRET` | Clé secrète de signature des tokens JWT (min 256 bits) | *Clé de test par défaut* |
| `JWT_EXPIRATION_MS` | Durée de validité de l'access token en millisecondes (900000 = 15 min) | `900000` |
| `REFRESH_TOKEN_EXPIRATION_DAYS` | Durée de validité du refresh token en jours | `7` |
| `FRONTEND_URL` | URL source autorisée pour le partage de ressources CORS | `http://localhost:4200` |
| `GEMINI_API_KEY` | Clé d'API Google Gemini pour les traitements d'IA | *(Vide par défaut)* |
| `GEMINI_MODEL` | Modèle Google Gemini utilisé pour la génération | `gemini-1.5-flash` |
| `AI_SERVICE_URL` | URL interne du service IA appelée par le backend | `http://localhost:8000` en local |

---

## 📁 Structure du projet

Le projet est organisé sous la forme d'un monorepo modulaire :

```text
studyai/
├── docker-compose.yml         # Définition et orchestration des conteneurs Docker
├── .env.example               # Modèle des variables d'environnement
├── .gitignore                 # Fichiers et dossiers exclus du suivi Git
├── README.md                  # Documentation technique principale du projet
│
├── backend/                   # Microservice Backend Spring Boot (Java 17)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/studyai/
│   │   │   │   ├── config/    # Sécurité, JWT, CORS, OpenAPI
│   │   │   │   ├── controller/# Contrôleurs REST
│   │   │   │   ├── model/     # Entités JPA
│   │   │   │   ├── repository/# Interfaces Spring Data JPA
│   │   │   │   └── service/   # Logique métier et intégration IA
│   │   │   └── resources/     # application.yml, scripts Flyway/SQL
│   ├── pom.xml                # Dépendances Maven
│   └── Dockerfile             # Image Docker du backend (multi-stage build)
│
├── frontend/                  # Application Frontend Angular 17+
│   ├── src/
│   │   ├── app/
│   │   │   ├── core/          # Services d'authentification, gardes, intercepteurs HTTP
│   │   │   ├── features/      # Modules métiers (dashboard, cours, tuteur, quiz, admin)
│   │   │   └── shared/        # Composants réutilisables, pipes, modèles
│   │   ├── assets/            # Images, styles globaux, icônes
│   │   └── index.html
│   ├── package.json           # Dépendances Node.js
│   ├── angular.json           # Configuration Angular CLI
│   ├── nginx.conf             # Configuration du serveur web Nginx
│   └── Dockerfile             # Image Docker frontend Nginx
│
└── ai-service/                # Microservice IA (Python 3.11 / FastAPI)
    ├── main.py                # Point d'entrée de l'application FastAPI
    ├── requirements.txt       # Dépendances Python (FastAPI, uvicorn, httpx, etc.)
    └── Dockerfile             # Image Docker du service d'intelligence artificielle
```

---

## 📄 Licence

Ce projet est sous licence MIT — voir le fichier `LICENSE` pour plus de détails.
