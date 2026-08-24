# USER_DOC — Utilisation de parking-gateway

Documentation d'utilisation : installation, lancement, endpoints, codes d'erreur.
Pour la conception et les choix d'architecture, voir [README.md](../README.md).
Pour le fonctionnement interne, voir [DEV_DOC.md](DEV_DOC.md).

---

## Prérequis

| Outil | Version | Vérification |
|---|---|---|
| Java (JDK) | 21+ | `java -version` |
| Docker | 20+ avec Compose v2 | `docker compose version` |
| Maven | fourni via le wrapper | rien à installer |

Docker n'est nécessaire que pour la source de données simulée de Cannes. L'API fonctionne
sans lui, mais seule la ville de Poitiers répondra.

---

## Lancement

Deux services à démarrer, depuis le dossier `parking-gateway/`.

### 1. La source de données Cannes (Docker)

```bash
cd parking-gateway
make up
```

Un conteneur nginx expose des données de stationnement de Cannes sur le port **8081**,
sur une URL et dans un format volontairement différents de ceux de Poitiers.

Vérification :

```bash
curl -i "http://localhost:8081/mobilites-stationnement-des-parkings-en-temps-reel/lines"
```

Réponse attendue : `HTTP/1.1 200 OK` et un JSON contenant `"total": 12`.

### 2. L'API

```bash
make run-serveur
```

L'application démarre sur le port **8080**. Attendre la ligne :

```
Started ParkingGatewayApplication in 1.08 seconds
```

`Ctrl+C` pour arrêter. Les logs applicatifs s'affichent dans ce terminal.

### Raccourcis Makefile

```bash
make up          # démarre le conteneur Cannes
make ps             # état du conteneur
make logs           # logs nginx en direct (utile pour voir les appels arriver)
make run-serveur    # démarre le serveur
make down          # arrête et supprime le conteneur
```

---

## Endpoints

Base : `http://localhost:8080/api/parking`

### Tous les parkings d'une ville

```
GET /api/parking?city={ville}
```

| Paramètre | Type | Obligatoire | Description |
|---|---|---|---|
| `city` | texte | oui | nom de la ville, insensible à la casse et aux espaces |

```bash
curl "http://localhost:8080/api/parking?city=poitiers"
curl "http://localhost:8080/api/parking?city=CANNES"
```

### Les parkings à proximité d'une position

```
GET /api/parking/proximity?lat={lat}&lon={lon}&radiusMetre={rayon}
```

| Paramètre | Type | Obligatoire | Bornes | Description |
|---|---|---|---|---|
| `lat` | décimal | oui | -90 à 90 | latitude du point de recherche |
| `lon` | décimal | oui | -180 à 180 | longitude du point de recherche |
| `radiusMetre` | décimal | oui | > 0 | rayon de recherche en mètres |

```bash
# Autour de la gare de Poitiers
curl "http://localhost:8080/api/parking/proximity?lat=46.5836&lon=0.3348&radiusMetre=1000"

# Autour du Palais des Festivals à Cannes
curl "http://localhost:8080/api/parking/proximity?lat=43.5510&lon=7.0177&radiusMetre=1000"
```

Les résultats sont triés par distance croissante. Les parkings dont la source ne fournit pas
de coordonnées sont exclus de cette recherche.

---

## Format de réponse

Identique quelle que soit la ville interrogée.

```json
[
  {
    "name": "GARE TOUMAI",
    "available": 474,
    "capacity": 640,
    "occupied": 166,
    "latitude": 46.5835835310322,
    "longitude": 0.334834883091724,
    "distanceMetre.": 3
  }
]
```

| Champ | Type | Description |
|---|---|---|
| `Name` | texte | nom du parking |
| `avaiable_space` | entier | places libres en temps réel |
| `capacity` | entier | capacité totale |
| `occupied` | entier | places occupées (`capacity - available`) |
| `latitude` / `longitude` | décimal | position, `null` si la source ne la fournit pas |
| `distance_metre` | entier | distance au point demandé, présent uniquement sur `/proximity` |

---

## Codes d'erreur

| Code | Situation | Corps de la réponse |
|---|---|---|
| `200` | succès, y compris si aucun parking ne correspond (liste vide) | tableau JSON |
| `400` | paramètre absent, hors bornes ou mal formé | `{"error": "..."}` |
| `404` | ville inconnue | `{"error":"city not supported lyon","supported city:":[...]}` |
| `503` | source de données injoignable, en erreur ou illisible | `{"error": "..."}` |
| `500` | erreur imprévue | `{"error": "Erreur interne"}` |

Exemples :

```bash
curl -i "http://localhost:8080/api/parking?city=lyon"
# 404 — {"error":"Ville non supportée : lyon","supportedCities":["cannes","poitiers"]}

curl -i "http://localhost:8080/api/parking/proximity?lat=999&lon=0&radiusMetre=100"
# 400 — latitude hors bornes
```

Une liste vide avec un code `200` n'est pas une erreur : elle signifie qu'aucun parking ne se
trouve dans le rayon demandé.

---

## Démonstration de la résilience

Chaque ville est isolée : la panne de l'une n'affecte pas les autres.

```bash

curl -i "http://localhost:8080/api/parking?city=cannes"    # 503, source injoignable
curl -i "http://localhost:8080/api/parking?city=poitiers"  # 200, toujours opérationnel

make up                                              # la source revient
```

Pour les utiliser, pointer temporairement `parking.provider.cannes.url` sur ces URL dans
`application.yaml`.

---

## Configuration

`src/main/resources/application.yaml`

```yaml
server:
  port: 8080

parking:
  provider:
    poitiers:
      url: "https://data.grandpoitiers.fr/data-fair/api/v1/datasets/mobilites-stationnement-des-parkings-en-temps-reel/lines"
    cannes:
      url: "http://localhost:8081/mobilites-stationnement-des-parkings-en-temps-reel/lines"
```

Changer une URL ne demande aucune recompilation du code métier : elle est injectée dans le
provider correspondant au démarrage.

---

## Dépannage

**`Connection refused` sur le port 8081** — le conteneur ne tourne pas. Vérifier avec
`make ps` : la colonne `STATUS` doit indiquer `Up`.

**`404 {"error":"endpoint inconnu"}` sur le port 8081** — l'URL du mock est incorrecte.
Elle ne comporte pas de préfixe `/datasets` et ne se termine pas par un slash.

**`404` avec un corps `{"timestamp","status","error","path"}`** — cette forme est celle de
Spring, pas de l'application : la route demandée n'existe pas. Vérifier le chemin
(`/api/parking`, sans `/v1`).

**`Port 8080 already in use`** — une instance tourne déjà. La terminer, ou changer
`server.port` dans `application.yaml`.

**Le conteneur démarre mais ne sert rien** — un chemin de volume erroné dans
`docker-compose.yaml` amène Docker à créer un dossier vide à la place du fichier. Vérifier :

```bash
docker compose exec cannes-api ls -l /data
```

`cannes.json` doit apparaître comme un fichier avec une taille en octets.