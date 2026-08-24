# parking-gateway

API REST d'agrégation des parkings en temps réel, multi-villes.

## Démarrage rapide

```bash
cd parking-gateway
make up            # source Cannes simulée (nginx) sur le port 8081
make run-serveur   # API sur le port 8080
```

```bash
curl "localhost:8080/api/parking?city=poitiers"
curl "localhost:8080/api/parking/proximity?lat=46.5836&lon=0.3348&radiusMetre=1000"
```

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
- **factoriser la mécanique commune** (appel HTTP, désérialisation, traduction) dans une classe
  abstraite, pour qu'un nouveau provider n'ait plus à la réécrire ;
- **exposer un format de sortie stable** (`ParkingDto`), indépendant de la source interrogée ;
- **router dynamiquement** vers la bonne source via un registry, sans aucune condition en dur
  dans le code métier.

Résultat : ajouter une ville se limite à créer trois classes et une ligne de
configuration. Aucun fichier existant n'est modifié, et l'URL consommée par l'application
mobile reste identique.

---

## 1. Architecture et choix techniques

Le projet est construit avec **Spring Boot 4.1** sur **Java 21**. Spring Boot m'apporte
l'essentiel de l'infrastructure sans configuration : serveur embarqué, injection de
dépendances, sérialisation JSON et gestion centralisée des erreurs. Je me concentre ainsi
sur la logique du sujet plutôt que sur la plomberie.

J'ai retenu une organisation **package-by-feature** : chaque ville possède son propre
package, aussi bien côté `provider/` que côté `dto/`. Le format spécifique d'une source
n'existe que dans son dossier et ne remonte jamais dans le reste de l'application.

### Trois niveaux d'abstraction, et pourquoi

| Niveau | Classe | Rôle |
|---|---|---|
| Contrat | `ParkingProvider` (interface) | ce que le métier sait faire d'une ville : la nommer, en tirer des parkings |
| Mécanique | `AbstractHttpProvider<R>` (abstraite) | tout ce qui est identique d'une ville à l'autre : requête HTTP, timeout, contrôle du statut, désérialisation, boucle de traduction |
| Spécificité | `PoitiersProvider`, `CannesProvider` | uniquement ce qui diffère : le nom de la ville, la classe cible de Jackson, l'endroit où trouver les lignes dans le JSON |

`AbstractHttpProvider` applique un **patron de méthode** : `retrieveParkings()` est `final`,
la séquence est verrouillée, et seuls deux points de variation sont laissés aux sous-classes
(`responseType()` et `extractLines()`). Un provider concret tient en une trentaine de lignes
et ne peut pas se tromper sur la gestion d'erreur — elle est écrite une fois pour toutes au
niveau du dessus.

### Deux niveaux de DTO, et pourquoi

- les DTO **par ville** (`dto/poitiers`, `dto/cannes`) reflètent fidèlement le JSON reçu ;
- le DTO **public** (`ParkingDto`) définit le format que mon API renvoie, identique quelle
  que soit la source.

Entre les deux, une interface pivot : `ParkingSourceLine`. Chaque ligne de ville s'engage à
savoir répondre à quatre questions (nom, places libres, capacité, géopoint), quel que soit le
nom réel de ses champs JSON. `ParkingMapper` ne travaille que sur cette interface : il est
donc **totalement générique**, écrit une seule fois, et n'a rien à apprendre quand une ville
s'ajoute.

C'est cette frontière qui garantit que le contrat exposé au client reste stable quand une
ville change son format.

### Le client HTTP

`HttpClientConfig` expose un `HttpClient` unique en bean Spring, partagé par tous les
providers. Ce n'est pas un détail : `HttpClient` gère son propre pool de connexions et son
propre exécuteur de threads. En instancier un par provider, ou pire un par requête, revient à
jeter le pool à chaque appel. Un seul bean, deux garde-fous temporels :

- **3 s** de timeout de connexion (`HttpClientConfig`) ;
- **5 s** de timeout sur la requête complète (`AbstractHttpProvider`).

Une source externe lente ne doit jamais faire attendre une requête mobile indéfiniment.

---

## 2. Configuration et isolation des sources

Les URL des différentes villes sont externalisées dans `application.yaml` et injectées par
`@Value` dans le provider correspondant. Aucune adresse n'est codée en dur.

Pour démontrer que l'application fonctionne réellement avec des sources hétérogènes, j'ai
mis en place une seconde source via **Docker et nginx** : elle expose des données de Cannes
sur une URL et dans un format totalement différents de ceux de Poitiers (`name_parking` vs
`Nom`, `Places_disponible` vs `Places`, `_geopoint_coord` vs `_geopoint`). Seul le provider
concerné en a connaissance — le controller, le service et le format de réponse restent
inchangés. C'est la preuve concrète que la contrainte du sujet est tenue.

Le jeu de données Cannes contient volontairement des cas limites : place libre à zéro,
géopoint textuel non numérique, géopoint `null`, nombre de places `null`, et des champs
supplémentaires (`tarif_horaire`, `gestionnaire`) absents du DTO. Aucun de ces cas ne fait
tomber la réponse.

---

## 3. Gestion des erreurs

J'ai créé des exceptions métier dédiées (`ProviderUnavailableException`,
`CityNotSupportedException`) que les providers et le registry lèvent à la place des erreurs
techniques Java. Elles héritent de `RuntimeException` et remontent sans être interceptées
jusqu'au `GlobalExceptionHandler`, seul endroit qui décide du code HTTP renvoyé :

| Situation | Code | Réponse |
|---|---|---|
| Source injoignable, en erreur ou illisible | 503 | message explicite nommant la ville |
| Ville inconnue | 404 | message + liste des villes prises en charge |
| Paramètre absent, mal typé ou hors bornes | 400 | détail de la contrainte non respectée |
| Erreur imprévue | 500 | message générique, stacktrace dans les logs serveur |

L'utilisateur reçoit toujours un JSON lisible, jamais une stacktrace.

### Dégradation partielle sur `/proximity`

La recherche par position interroge **toutes** les villes. Une source en panne ne doit pas
faire échouer la requête entière : `ParkingService.collectAllParkings()` attrape le
`ProviderUnavailableException` **par provider**, trace un `log.warn`, et poursuit avec les
sources restantes. Si Cannes tombe, une recherche autour de Poitiers répond toujours `200`.

La recherche par ville, elle, reste stricte : demander explicitement `?city=cannes` alors que
la source Cannes est morte renvoie `503` (Service Unavailable). C'est cohérent — le client a nommé une ville
précise, lui renvoyer une liste vide en `200` serait un mensonge.

---

## 4. État des tests

### Tests implémentés et actifs

| Classe de Test | Objectif & Comportement vérifié |
|---|---|
| `ParkingGatewayApplicationTests` | **Contexte Spring** : Vérifie que l'application démarre et que l'injection des dépendances (providers, registry, config) est valide. |
| `ParkingMapperTest` | **Logique de transformation (Test Pur)** : Vérifie le parsing des coordonnées géographiques (cas valides, `null`, chaînes non-numériques, format invalide). Valide également le calcul de déduction des places occupées. *Note : Utilise un `record` local (`LigneSource`) pour simuler le contrat de l'interface `ParkingSourceLine` de manière totalement isolée.* |
| `CoordUtilsTests` | **Logique mathématique (Test Pur)** : Vérifie le calcul de distance géospatiale (distance nulle sur deux points identiques, et symétrie parfaite de la distance aller/retour). |
| `AbstractProviderTest` | **Résilience réseau** : Vérifie le comportement du parent HTTP abstrait face à une erreur de connexion (URL invalide/serveur injoignable) et s'assure qu'il lève bien une `ProviderUnavailableException` propre pour le contrôleur. |

### Pourquoi cette stratégie de test ?

1. **Isolation de la logique métier (`ParkingMapper` / `CoordUtils`) :** Ce sont des tests dits "purs". Ils n'ont besoin d'aucune connexion réseau ni de base de données. Ils s'exécutent en quelques millisecondes et garantissent que le cœur de l'application (le calcul des places et des distances) est infaillible, peu importe ce que renvoient les API.
2. **Simulation du contrat (Le `record LigneSource`) :** Plutôt que d'instancier un DTO de ville complexe (`PoitiersParkingLine`), le test du mapper utilise un simple `record` implémentant `ParkingSourceLine`. Cela prouve que le mapper dépend uniquement du *contrat* et non de l'implémentation d'une ville spécifique.
3. **Réparation du test HTTP (`AbstractProviderTest`) :** Le test désactivé a été réparé en ciblant la classe parente. Cela garantit que **toutes** les villes hériteront de cette gestion d'erreur robuste si leur serveur plante.
4. **Simulation normalisation des clés et exception handle** verifie que`CityNotSupportedException` est bien levée si on demande une ville inconnue, et verification de la normalisation des clés de recherche

### Ce qu'il reste à tester (couverture tests)

Avec le temps, voici les derniers éléments qui viendraient clôturer la couverture :

*   **Les erreurs HTTP spécifiques (`AbstractHttpProvider`)** : Bouchonner (`Mock`) le `HttpClient` pour simuler une erreur `500 Internal Server Error` ou un corps de réponse tronqué (JSON invalide).

---

## Journal trace de mon temps de developpement et de recherche

Concernant le temps passé sur ce problème : j'ai commencé vendredi 16, suite à mon entretien
avec Sarah. J'ai reçu le test à 16h et j'ai immédiatement pris connaissance du sujet, noté où
m'orienter, puis anticipé chaque partie du code. J'ai passé en tout plus de 10h sur ce
problème, ce qui m'a permis de prendre le temps d'optimiser et d'obtenir un code lisible et
efficace.

Le découpage s'est fait par branches successives, chacune correspondant à une étape de
conception : `api/providerHttpClient`, `json/returnFinalList`, `dto/positionData`,
`nearby/park`. La dernière passe a consisté à remonter la mécanique commune dans
`AbstractHttpProvider` et à rendre le mapper générique.

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

### La duplication entre providers

Une fois Cannes ajoutée, `PoitiersProvider` et `CannesProvider` étaient deux copies presque
identiques : même construction de requête, même contrôle de statut, mêmes trois `catch`, même
boucle de traduction. Ajouter une troisième ville aurait signifié copier une troisième fois la
gestion d'erreur — et introduire une troisième occasion de l'écrire de travers.

J'ai extrait `AbstractHttpProvider<R>`, générique sur le type de réponse de la ville. La
séquence complète y est écrite une fois et rendue `final`. Les sous-classes ne fournissent
plus que leur `Class<R>` cible et la façon d'atteindre les lignes dans leur structure. La
duplication est tombée à zéro, et la gestion d'erreur est devenue impossible à oublier.

### Rendre le mapper indépendant des villes

Même problème un cran plus bas : la traduction ligne source → `ParkingDto` était écrite dans
chaque provider, avec les noms de champs de sa ville. J'ai introduit `ParkingSourceLine`, une
interface que chaque DTO de ville implémente. Les `@JsonProperty` continuent de porter les
noms réels du JSON, mais les getters exposent un vocabulaire commun. `ParkingMapper` ne
connaît plus que ce vocabulaire : il est unique et ne bougera plus.

### Router vers la bonne ville sans condition en dur

Une suite de `if (city.equals("poitiers"))` aurait fonctionné, mais chaque nouvelle ville
aurait imposé de modifier du code existant — exactement ce que l'énoncé cherche à éviter.

J'ai donc mis en place un registry construit à partir de la liste des providers injectée par
Spring. Chaque provider déclare la ville qu'il couvre, le registry indexe, et le service ne
connaît plus que l'interface.

### Distinguer les erreurs métier des erreurs techniques

Au départ, une source injoignable remontait sous forme d'`IOException` brute jusqu'au client.
Le message était illisible et le code HTTP inadapté. J'ai introduit des exceptions métier
levées par les providers, traduites en codes HTTP par un unique `GlobalExceptionHandler`. La
logique de gestion d'erreur est regroupée à un seul endroit plutôt que dispersée.

Le cas de `InterruptedException` m'a fait chercher : l'attraper efface silencieusement le
drapeau d'interruption du thread. Je restaure ce drapeau avec
`Thread.currentThread().interrupt()` avant de relancer mon exception métier, sinon le pool de
threads de Tomcat ne saurait jamais qu'un arrêt a été demandé.

### Robustesse des données sources

Deux parkings de Poitiers ne publient aucune coordonnée dans le flux open data. Un parsing
naïf du champ `_geopoint` faisait échouer toute la réponse à cause de ces deux lignes.

J'ai traité le cas explicitement : `parseGeoPoint()` renvoie un `Optional<Coordinates>` vide
au lieu de lever. Une coordonnée absente ou illisible n'écarte pas le parking, elle le laisse
simplement sans position, avec un `log.warn`. Il reste retourné par `?city=`, et seulement
exclu de `/proximity` — où l'on ne peut évidemment pas calculer une distance sans position.

---

## Pistes d'amélioration identifiées

Ces points ne sont pas implémentés, mais ils sont identifiés.

### Fraîcheur de la donnée

La source de Poitiers publie un champ de dernière mise à jour que je ne remonte pas. Sur un
écran de places disponibles, savoir si le chiffre date de trente secondes ou de deux heures
change son interprétation. L'exposer sous forme d'un champ `lastUpdate` dans `ParkingDto`
serait un ajout **additif** : un champ en plus ne casse aucun client existant, contrairement
à un renommage. C'est la seule forme d'évolution que le contrat figé autorise.

### Signaler les sources manquantes sur `/proximity`

La dégradation partielle fonctionne, mais elle est silencieuse côté client : une ville en
panne disparaît simplement des résultats. Un champ listant les sources non jointes rendrait
la réponse honnête. Cela suppose d'envelopper la liste dans un objet racine — donc un
changement de contrat, à peser.

### Appels en parallèle

`collectAllParkings()` interroge les villes séquentiellement. Avec deux sources c'est
indolore ; avec dix, la latence est la somme des dix appels. `HttpClient.sendAsync()` et un
`CompletableFuture.allOf()` ramèneraient le coût à celui de la source la plus lente. Le pool
de connexions partagé est déjà en place pour ça.

### Pagination des sources

L'API de Poitiers renvoie l'ensemble de ses parkings dans une seule réponse, ce qui convient
à son volume. Une source plus fournie imposerait de gérer la pagination côté provider, sans
impact sur le contrat exposé — la logique resterait confinée à `AbstractHttpProvider`.

### Ingestion asynchrone et mise en cache (Architecture de type Batch)

Dans l'implémentation actuelle, l'API interroge les serveurs des villes "à la volée" (de manière synchrone) lors de la requête du client. Bien que le temps de réponse soit encadré par des `timeout` stricts, la latence globale subie par l'utilisateur dépend toujours de la vitesse de réponse du fournisseur externe.

Pour un passage à l'échelle (ex: des milliers d'utilisateurs simultanés sur une application mobile), la prochaine étape architecturale consisterait à découpler complètement la collecte des données de leur restitution :

1. **Worker en arrière-plan (`@Scheduled`) :** Mettre en place un processus métier planifié qui interroge tous les `ParkingProvider` à intervalle régulier (ex: toutes les 2 minutes), de manière asynchrone.
2. **Stockage (Redis / BDD) :** Sauvegarder l'état consolidé (les `ParkingDto`) dans un datastore ultra-rapide comme **Redis** ou une base de données relationnelle, en y ajoutant systématiquement un champ `lastUpdate`.
3. **Restitution immédiate :** Le `ParkingController` ne ferait plus aucun appel HTTP vers l'extérieur. Il se contenterait de lire le dernier état connu dans la base de données pour le servir au client.

**Bénéfices d'une telle architecture :**
*   **Latence nulle :** Le client reçoit ses données quasi-instantanément, quelle que soit la lenteur du serveur de la ville.
*   **Résilience maximale :** Si l'API de Cannes tombe en panne pendant une heure, l'application mobile affiche toujours les parkings avec les données vieilles d'une heure (accompagnées d'un indicateur visuel de fraîcheur), au lieu d'une liste vide ou d'une erreur.
*   **Protection des fournisseurs (Rate Limiting) :** Si 10 000 utilisateurs ouvrent l'application simultanément, les serveurs des villes ne reçoivent toujours qu'une seule requête toutes les 2 minutes de notre part. Cela évite le bannissement de notre IP pour abus.