# DEV_DOC — Fonctionnement interne de parking-gateway

Documentation technique : structure du code, rôle de chaque couche, mécanismes internes.
Pour l'utilisation, voir [USER_DOC.md](USER_DOC.md).

---

## Structure du projet

```
fr.theosykas.parking
├── ParkingGatewayApplication.java   point d'entrée Spring Boot
├── config/                          beans partagés (HttpClient)
├── controller/
│   └── ParkingController            couche HTTP : reçoit, valide, délègue
├── service/
│   └── ParkingService               orchestration : sélection puis calcul
├── provider/
│   ├── ParkingProvider              le contrat que toute ville implémente
│   ├── ParkingProviderRegistry      annuaire ville → provider
│   ├── poitiers/PoitiersProvider    appel + traduction du format Poitiers
│   └── cannes/CannesProvider        appel + traduction du format Cannes
├── dto/
│   ├── ParkingDto                   format public, stable, renvoyé au client
│   ├── request/NearbyRequest        paramètres d'entrée de /proximity, validés
│   ├── poitiers/                    reflet fidèle du JSON de Poitiers
│   └── cannes/                      reflet fidèle du JSON de Cannes
├── exception/
│   ├── ProviderInterrupted          source injoignable ou illisible
│   ├── CityNotSupportedException    ville absente du registry
│   └── GlobalExceptionHandler       exception → code HTTP, en un seul endroit
└── utils/
    └── CoordUtils                   calcul de distance géographique
```

---

## Le contrat : `ParkingProvider`

```java
public interface ParkingProvider {
    String getCity();                       // qui je suis
    List<ParkingDto> retrieveParkings();    // ce que je sais faire
}
```

Deux méthodes, deux rôles. `getCity()` donne au provider une identité exploitable par le
registry ; `retrieveParkings()` renvoie déjà le format public, jamais le format brut de la
ville.

C'est ce contrat qui permet le polymorphisme : le service manipule un `ParkingProvider` sans
savoir s'il s'agit de Poitiers ou de Cannes. Aucun `switch`, aucun `if` sur le nom de la ville
n'existe dans le code métier.

---

## Le registry : découverte automatique

```java
public ParkingProviderRegistry(List<ParkingProvider> providers) {
    for (ParkingProvider provider : providers) {
        providerByCity.put(normalize(provider.getCity()), provider);
    }
}
```

Spring injecte dans ce constructeur **toutes** les implémentations de `ParkingProvider`
annotées `@Component`. Aucune inscription manuelle n'est nécessaire : créer une classe suffit
à la rendre accessible.

La map est construite une seule fois au démarrage, jamais reconstruite à l'exécution.

### La normalisation des clés

```java
private String normalize(String city) {
    city.trim().toLowerCase();
}
```

Appelée **aux deux moments** : à l'insertion et à la recherche. C'est cette symétrie qui rend
`?city=CANNES`, `?city=Cannes` et `?city= cannes ` équivalents. Normaliser d'un seul côté
produirait des `404` incompréhensibles, une `HashMap` comparant ses clés avec `equals()`,
sensible à la casse.

---

## Les deux modes de recherche

### Par ville

```java
registry.getProviderCity(city).retrieveParkings();
```

Une recherche dans la map, un appel à une seule source. Exhaustif : tous les parkings de la
ville sont renvoyés, y compris ceux dont les coordonnées sont absentes.

### Par position

```java
registry.getAllProviders().stream()
    .flatMap(provider -> provider.retrieveParkings().stream())
    .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
    .peek(p -> p.setDistanceMeter(Math.round(
            CoordUtils.calculateDistance(lat, lon, p.getLatitude(), p.getLongitude()))))
    .filter(p -> p.getDistanceMeter() <= radiusMeters)
    .sorted(Comparator.comparingLong(ParkingDto::getDistanceMeter))
    .toList();
```

L'ordre des étapes est contraint : on ne peut pas filtrer sur une distance non calculée, ni
calculer une distance sans coordonnées.

Le filtre sur les coordonnées nulles n'est pas défensif par principe : l'open data de Poitiers
contient réellement deux parkings sans géolocalisation (CORDELIERS et GARE EFFIA). Sans ce
filtre, le déballage du `Double` provoquerait une `NullPointerException` sur des données de
production.

---

## Calcul de distance

`CoordUtils` applique une **approximation équirectangulaire** : conversion en radians, écart
nord-sud et écart est-ouest, correction de ce dernier par le cosinus de la latitude moyenne
(les méridiens se resserrent vers les pôles), puis Pythagore comme si la zone était plane. Le
résultat est multiplié par le rayon terrestre pour passer des radians aux mètres.

Moins précise que la formule de Haversine sur de longues distances, mais nettement moins
coûteuse et d'une erreur négligeable à l'échelle d'une ville.

La classe est `final`, son constructeur est privé, sa méthode est `static` : c'est une classe
utilitaire pure, sans état, qu'on n'instancie ni n'hérite.

---

## Appel HTTP et traduction, dans un provider

Chaque provider suit la même séquence :

```
1. construire la requête       URL injectée depuis application.yaml
2. envoyer                     HttpClient natif (java.net.http, Java 11+)
3. vérifier le statut          != 200 → ProviderInterrupted
4. désérialiser                Jackson → DTO propre à la ville
5. traduire                    DTO ville → ParkingDto
```

L'étape 3 est essentielle : `HttpClient` ne lève **aucune** exception sur un 404 ou un 500,
il renvoie une réponse normale. Sans cette vérification, un corps d'erreur serait transmis à
Jackson, qui échouerait sur un message trompeur — ou pire, désérialiserait un objet vide,
produisant une liste vide avec un code 200 : une panne totalement invisible.

L'étape 5 est le cœur du découplage. C'est le seul endroit du projet où le format d'une ville
et le format public se croisent.

### Choix de `java.net.http`

L'API native introduite avec Java 11, plutôt qu'une bibliothèque tierce : aucune dépendance
supplémentaire, et suffisante pour des appels GET simples. Le `HttpClient` est instancié une
seule fois par provider (idéalement partagé via un bean dans `config/`), car il gère son
propre pool de connexions et de threads.

---

## Pipeline d'erreurs

```
Provider                          Registry                Controller
   │                                 │                        │
   ├ IOException                     ├ ville inconnue         ├ paramètre invalide
   ├ InterruptedException            │                        │
   ├ statut HTTP != 200              │                        │
   ├ JacksonException                │                        │
   ▼                                 ▼                        ▼
ProviderInterrupted     CityNotSupportedException   HandlerMethodValidationException
   │                                 │                        │
   └─────────────── aucune couche n'intercepte ───────────────┘
                                     │
                                     ▼
                        GlobalExceptionHandler
                          503 · 404 · 400 · 500
```

Le principe : **les exceptions traversent le service et le controller sans être attrapées.**
Elles héritent de `RuntimeException`, donc rien n'oblige les couches intermédiaires à les
déclarer ou à les gérer. C'est ce qui permet à `ParkingService` de tenir en une ligne, sans un
seul `try`.

La traduction exception → code HTTP se fait à un seul endroit. Changer un code de retour, c'est
modifier une ligne.

### Deux points d'attention

`Thread.currentThread().interrupt()` dans le `catch (InterruptedException)` : attraper cette
exception efface le drapeau d'interruption du thread. Sans restauration, le pool de threads de
Tomcat ne saura jamais qu'un arrêt a été demandé.

Le `catch (NumberFormatException)` autour du parsing des coordonnées ne relance pas : une
coordonnée illisible sur un parking ne doit pas faire échouer les vingt-neuf autres. Un
`log.warn` trace l'anomalie, le traitement continue. C'est une dégradation partielle, pas un
échec.

---

## Validation des entrées

Les contraintes sont posées sur `NearbyRequest` (`@NotNull`, `@Min`, `@Max`, `@Positive`) et
sur le paramètre `city` (`@NotBlank`), avec `@Validated` sur le controller.

Spring rejette la requête **avant** d'entrer dans la méthode : aucun appel réseau n'est
déclenché pour une requête invalide, et l'appelant reçoit un `400` qui désigne son erreur
plutôt qu'un `500` qui accuse le serveur.

Le principe appliqué : **valider aux frontières, faire confiance à l'intérieur.** Passé le
controller, le service et les providers travaillent sur des données saines, sans contrôle
défensif redondant.

---

## Configuration

`application.yaml` externalise l'URL de chaque source :

```yaml
parking:
  provider:
    poitiers:
      url: "https://data.grandpoitiers.fr/data-fair/api/v1/datasets/..."
    cannes:
      url: "http://localhost:8081/mobilites-stationnement-des-parkings-en-temps-reel/lines"
```

Elle est injectée par `@Value` dans le provider correspondant. Les deux chemins sont
volontairement différents : c'est la démonstration qu'aucune convention n'est supposée
partagée entre les villes.

La source Cannes est simulée par un conteneur nginx (`docker-compose.yaml` +
`data_city_api/`), qui sert un jeu de données statique sur une URL arbitraire. Le fichier
contient volontairement des cas limites : place libre à zéro, géopoint non numérique, géopoint
absent, nombre de places manquant, et champs supplémentaires non déclarés dans le DTO — ce
dernier cas étant absorbé par `@JsonIgnoreProperties(ignoreUnknown = true)`.

---

## Ajouter une ville

1. `dto/{ville}/` — les classes reflétant le JSON de la source, avec `@JsonProperty` pour
   chaque champ et `@JsonIgnoreProperties(ignoreUnknown = true)` sur la classe
2. `provider/{ville}/{Ville}Provider.java` — `@Component`, implémente `ParkingProvider`,
   `getCity()` renvoie le nom en minuscules
3. `application.yaml` — l'URL de la source

Aucun fichier existant n'est modifié. Le registry détecte le nouveau provider au démarrage.

---

## Journal

- Premier jour : 16h51 → 21h44