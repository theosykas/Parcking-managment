 premier jour : 16h51 -> 21h44

## Démarrage rapide

​```bash
cd parking-gateway
docker compose up -d      # source Cannes simulée (nginx)
./mvnw spring-boot:run    # API sur le port 8080
​```

​```bash
curl "localhost:8080/api/parking?city=poitiers"
curl "localhost:8080/api/parking/proximity?lat=46.5836&lon=0.3348&radiusMetre=1000"
​```

Documentation détaillée : [USER_DOC.md](doc/USER_DOC.md) · [DEV_DOC.md](doc/DEV_DOC.md)

---

## Description

**parking-gateway** est une API REST qui sert d'intermédiaire entre une application cliente
(mobile ou web) et les données de stationnement publiées en open data par les villes.

Le sujet demande d'exposer la liste des parkings à proximité d'une position, avec leur nombre
de places disponibles en temps réel, tout en respectant une contrainte forte : **l'API exposée
au client ne doit jamais évoluer**, alors que les sources de données, elles, changent
complètement d'une ville à l'autre — URL différente, format JSON différent, noms de champs
différents.

C'est cette contrainte qui a structuré toute ma conception. J'ai donc découpé le problème en
étapes distinctes, que détaille la suite de ce document :

- **isoler chaque source** dans un *provider* dédié, seul endroit du code qui connaît le format
  d'une ville donnée ;
- **définir un contrat unique** (`ParkingProvider`) que toutes les villes implémentent ;
- **exposer un format de sortie stable** (`ParkingDto`), indépendant de la source interrogée ;
- **router dynamiquement** vers la bonne source via un registry, sans aucune condition en dur
  dans le code métier.

Résultat : ajouter une ville se limite à créer une classe et une ligne de configuration.
Aucun fichier existant n'est modifié, et l'URL consommée par l'application mobile reste
identique.

---

### 1. Architecture et choix techniques

Le projet est construit avec **Spring Boot 4.1** sur **Java 21**. Spring Boot m'apporte
l'essentiel de l'infrastructure sans configuration : serveur embarqué, injection de
dépendances, sérialisation JSON et gestion centralisée des erreurs. Je me concentre ainsi
sur la logique du sujet plutôt que sur la plomberie.

J'ai retenu une organisation **package-by-feature** : chaque ville possède son propre
package, aussi bien côté `provider/` que côté `dto/`. Le format spécifique d'une source
n'existe que dans son dossier et ne remonte jamais dans le reste de l'application. Ajouter
une ville revient à ajouter un dossier, sans toucher à l'existant.

Deux niveaux de DTO cohabitent, et c'est volontaire :

- les DTO **par ville** (`dto/poitiers`, `dto/cannes`) reflètent fidèlement le JSON reçu ;
- le DTO **public** (`ParkingDto`) définit le format que mon API renvoie, identique quelle
  que soit la source.

La traduction entre les deux se fait dans le provider. C'est cette frontière qui garantit
que le contrat exposé au client reste stable quand une ville change son format.

### 2. Configuration et isolation des sources

Les URL des différentes villes sont externalisées dans `application.yaml` et injectées dans
le provider correspondant. Aucune adresse n'est codée en dur.

Pour démontrer que l'application fonctionne réellement avec des sources hétérogènes, j'ai
mis en place une seconde source via **Docker et nginx** : elle expose des données de Cannes
sur une URL et dans un format totalement différents de ceux de Poitiers. Seul le provider
concerné en a connaissance — le controller, le service et le format de réponse restent
inchangés. C'est la preuve concrète que la contrainte du sujet est tenue.

### 3. Gestion des erreurs

J'ai créé des exceptions métier dédiées (`ProviderInterrupted`, `CityNotSupportedException`)
que les providers lèvent à la place des erreurs techniques Java. Elles remontent sans être
interceptées jusqu'au `GlobalExceptionHandler` de Spring, seul endroit qui décide du code
HTTP renvoyé :

| Situation | Code | Réponse |
|---|---|---|
| Source injoignable ou illisible | 503 | message explicite |
| Ville inconnue | 404 | message + liste des villes disponibles |
| Paramètres invalides | 400 | détail de la contrainte non respectée |
| Erreur imprévue | 500 | message générique, détail dans les logs |

L'utilisateur reçoit toujours un JSON lisible, jamais une stacktrace.

---

## Journal
concernant le temps passer sur ce probleme j'ai donc commencer vendredi 16 suite a mon entretient avec Sarah j'ai donc recus le test a 16h j'ai donc intentanement pris connaissance du sujet noter ou m'orienté puis anticipé chaques parti de code j'ai donc passer en tout pour tout plus de 10h sur ce probleme posé et pouvoir prendre le temps d'optimiser et d'avoir un code lisible et efficace 

---

## Difficultés rencontrées

### Concilier des sources hétérogènes et un contrat figé

C'est le point qui m'a demandé le plus de réflexion. Il fallait qu'une URL et un format JSON
totalement différents d'une ville à l'autre débouchent sur une réponse strictement identique
côté client. Mes premières versions laissaient remonter la structure de Poitiers jusqu'au
controller : tant qu'il n'y avait qu'une source, cela fonctionnait, mais rien n'aurait tenu à
l'ajout d'une seconde ville.

J'ai repris la conception en posant une interface `ParkingProvider` comme contrat commun, et
en confinant la traduction du format source vers `ParkingDto` à l'intérieur de chaque
provider. La frontière est devenue explicite : au-dessus, un format stable ; en dessous,
autant de formats que de villes.

### Router vers la bonne ville sans condition en dur

Une fois deux sources en place, la question était de savoir laquelle interroger. Une suite de
`if (city.equals("poitiers"))` aurait fonctionné, mais chaque nouvelle ville aurait imposé de
modifier du code existant — exactement ce que l'énoncé cherche à éviter.

J'ai donc mis en place un registry construit à partir de la liste des providers injectée par
Spring. Chaque provider déclare la ville qu'il couvre, le registry indexe, et le service ne
connaît plus que l'interface. Ajouter une ville se réduit à créer une classe : aucun fichier
existant n'est touché.

### Distinguer les erreurs métier des erreurs techniques

Au départ, une source injoignable remontait sous forme d'`IOException` brute jusqu'au client.
Le message était illisible et le code HTTP inadapté. J'ai introduit des exceptions métier
(`ProviderInterrupted`, `CityNotSupportedException`) levées par les providers, traduites en
codes HTTP par un unique `GlobalExceptionHandler`. La logique de gestion d'erreur est ainsi
regroupée à un seul endroit plutôt que dispersée dans chaque provider.

### Robustesse des données sources

Deux parkings de Poitiers ne publient aucune coordonnée dans le flux open data. Un parsing
naïf du champ `_geopoint` faisait échouer toute la réponse à cause de ces deux lignes.

J'ai traité le cas explicitement : une coordonnée absente ou illisible n'écarte pas le
parking, elle le laisse simplement sans position. Le même raisonnement m'a conduit à poser
des timeouts sur les appels sortants — une source externe lente ne doit pas bloquer une
requête mobile.

## Pistes d'amélioration identifiées

Ces points n'ont pas été implémentés faute de temps, mais ils sont identifiés et chiffrés.

### Fraîcheur de la donnée

La source de Poitiers publie un champ `Dernière_mise_à_jour_Base` que je ne remonte pas
aujourd'hui. Sur un écran de places disponibles, savoir si le chiffre date de trente secondes
ou de deux heures change son interprétation. L'exposer sous forme d'un champ `lastUpdate`
dans `ParkingDto` 

### Dégradation partielle en cas de source indisponible

Lorsque plusieurs villes sont agrégées, une source en panne interrompt actuellement toute la
réponse. Un traitement par provider permettrait de renvoyer les villes disponibles et de
signaler les autres, par exemple via un champ listant les sources non jointes. Une réponse
partielle reste plus utile au client qu'une erreur globale.

### Pagination des sources

L'API de Poitiers renvoie l'ensemble de ses parkings dans une seule réponse, ce qui convient
à son volume. Une source plus fournie imposerait de gérer la pagination côté provider, sans
impact sur le contrat exposé — la logique resterait confinée à la classe concernée.