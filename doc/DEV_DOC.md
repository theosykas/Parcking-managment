# DEV_DOC — Fonctionnement interne de parking-gateway

Documentation technique : structure du code, rôle de chaque couche, mécanismes internes.
Pour l'utilisation, voir [USER_DOC.md](USER_DOC.md). Pour les choix de conception et le
raisonnement qui y a mené, voir [README.md](../README.md).

---

## Structure du projet

```
fr.theosykas.parking
├── ParkingGatewayApplication.java   point d'entrée Spring Boot
├── config/
│   └── HttpClientConfig             bean HttpClient partagé (pool + timeout connexion)
├── controller/
│   └── ParkingController            couche HTTP : reçoit, valide, délègue
├── service/
│   └── ParkingService               orchestration : sélection, agrégation, distance, tri
├── provider/
│   ├── ParkingProvider              le contrat que toute ville implémente
│   ├── AbstractHttpProvider<R>      mécanique HTTP + désérialisation, mutualisée
│   ├── ParkingMapper                ligne source (générique) → ParkingDto
│   ├── ParkingProviderRegistry      annuaire ville → provider
│   ├── poitiers/PoitiersProvider    ce qui est propre à Poitiers, et rien d'autre
│   └── cannes/CannesProvider        ce qui est propre à Cannes, et rien d'autre
├── dto/
│   ├── ParkingDto                   format public, stable, renvoyé au client
│   ├── ParkingSourceLine            interface pivot : vocabulaire commun des lignes source
│   ├── request/NearbyRequest        paramètres d'entrée de /proximity, validés
│   ├── poitiers/                    PoitierResponse + PoitierParkingLine
│   └── cannes/                      CannesResponse + CannesParkingLine
├── exception/
│   ├── ProviderUnavailableException source injoignable, en erreur ou illisible
│   ├── CityNotSupportedException    ville absente du registry
│   └── GlobalExceptionHandler       exception → code HTTP, en un seul endroit
└── utils/
    └── CoordUtils                   calcul de distance géographique
```

---

## Vue d'ensemble d'une requête

```
GET /api/parking/proximity?lat=..&lon=..&radiusMetre=..
        │
        ▼
ParkingController          @Valid → NearbyRequest ; rejet en 400 avant tout appel réseau
        │
        ▼
ParkingService             getNearby() : agrège, calcule, filtre, trie
        │
        ▼
ParkingProviderRegistry    getAllProviders() → toutes les villes connues
        │
        ├──────────────┬──────────────┐
        ▼              ▼              ▼
 PoitiersProvider  CannesProvider   (…)          extends AbstractHttpProvider<R>
        │              │
        │  1. HTTP GET (URL injectée, timeout 5 s)
        │  2. statut != 200 → ProviderUnavailableException
        │  3. Jackson → PoitierResponse / CannesResponse
        │  4. extractLines() → List<? extends ParkingSourceLine>
        │  5. ParkingMapper.toParkingDto() ligne par ligne
        ▼
   List<ParkingDto>        format public, identique quelle que soit la ville
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
registry ; `retrieveParkings()` renvoie **déjà** le format public, jamais le format brut de la
ville.

C'est ce contrat qui permet le polymorphisme : le service manipule un `ParkingProvider` sans
savoir s'il s'agit de Poitiers ou de Cannes. Aucun `switch`, aucun `if` sur le nom de la ville
n'existe dans le code métier.

---

## La mécanique mutualisée : `AbstractHttpProvider<R>`

C'est la pièce centrale du projet. Elle implémente `ParkingProvider` **sans savoir de quelle
ville il s'agit**, en laissant exactement deux trous à combler.

```java
public abstract class AbstractHttpProvider<R> implements ParkingProvider {

    protected abstract Class<R> responseType();                              // trou n°1
    protected abstract List<? extends ParkingSourceLine> extractLines(R r);  // trou n°2

    @Override
    public final List<ParkingDto> retrieveParkings() { ... }
}
```

### Pourquoi `final` sur `retrieveParkings()`

La séquence — appeler, vérifier le statut, désérialiser, traduire — est identique pour toute
source HTTP/JSON. La verrouiller garantit qu'un futur provider ne pourra pas la court-circuiter
et, par exemple, oublier le contrôle de statut. C'est un **patron de méthode** : le squelette
est fixe, les points de variation sont explicites et minimaux.

Conséquence directe : `CannesProvider` fait 38 lignes, `PoitiersProvider` 42, et aucun des
deux ne contient un seul `try`.

### Pourquoi le paramètre de type `<R>`

Jackson a besoin d'un type concret pour désérialiser. `R` est le type de l'enveloppe de la
ville (`PoitierResponse`, `CannesResponse`), et `responseType()` en fournit le `Class<R>` à
l'exécution. Le compilateur vérifie ensuite que `extractLines(R)` reçoit bien le même type :
impossible de brancher le `CannesResponse` d'une ville sur l'`extractLines` d'une autre.

### La séquence, étape par étape

```
1. construire la requête    URL injectée depuis application.yaml, timeout 5 s
2. envoyer                  HttpClient partagé (bean)
3. vérifier le statut       != 200 → ProviderUnavailableException
4. désérialiser             mapper.readValue(body, responseType())
5. extraire                 extractLines(response) → lignes au format de la ville
6. traduire                 ParkingMapper.toParkingDto() sur chaque ligne
```

**L'étape 3 est essentielle.** `HttpClient` ne lève *aucune* exception sur un 404 ou un 500 :
il renvoie une réponse normale. Sans cette vérification, un corps d'erreur serait transmis à
Jackson, qui échouerait sur un message trompeur — ou pire, désérialiserait un objet vide,
produisant une liste vide avec un code `200` : une panne totalement invisible.

**L'étape 6 tolère `null`.** Si `extractLines()` renvoie `null` (clé `results` absente du
JSON), la boucle est simplement sautée et une liste vide remonte. Pas de `NullPointerException`
sur une source malformée.

### Les trois `catch`

| Exception attrapée | Cause réelle | Traduite en |
|---|---|---|
| `JacksonException` | corps illisible : HTML, JSON tronqué, type incompatible | `ProviderUnavailableException` |
| `IOException` | DNS, connexion refusée, coupure réseau, timeout | `ProviderUnavailableException` |
| `InterruptedException` | arrêt demandé pendant l'attente | `ProviderUnavailableException`, **après** restauration du drapeau |

Sur `InterruptedException`, `Thread.currentThread().interrupt()` est appelé avant de relancer.
Attraper cette exception efface le drapeau d'interruption du thread ; sans restauration, le
pool de threads de Tomcat ne saurait jamais qu'un arrêt a été demandé et le thread continuerait
à travailler.

### Un provider concret

```java
@Component
public class CannesProvider extends AbstractHttpProvider<CannesResponse> {

    public CannesProvider(HttpClient client, ObjectMapper mapper, ParkingMapper parkingMapper,
                          @Value("${parking.provider.cannes.url}") String providerUrlApi) {
        super(client, mapper, parkingMapper, providerUrlApi);
    }

    @Override public String getCity() { return "Cannes"; }

    @Override protected Class<CannesResponse> responseType() { return CannesResponse.class; }

    @Override protected List<CannesParkingLine> extractLines(CannesResponse r) {
        return r.getResults();
    }
}
```

Trois méthodes, aucune logique. Tout ce qui est risqué vit un cran au-dessus.

---

## L'interface pivot : `ParkingSourceLine`

```java
public interface ParkingSourceLine {
    String  getNameOfParking();
    Integer getEmptySpace();
    Integer getTotalSpace();
    String  getGeoPoint();
}
```

Le problème qu'elle résout : Poitiers publie `Nom` / `Places` / `Capacite` / `_geopoint`,
Cannes publie `name_parking` / `Places_disponible` / `Capacite_max` / `_geopoint_coord`. Sans
vocabulaire commun, le mapper devrait connaître les deux — et une troisième ville l'obligerait
à le modifier.

Chaque DTO de ville implémente l'interface. Les `@JsonProperty` portent les noms réels du JSON,
les getters (générés par Lombok `@Data`) exposent le vocabulaire commun :

```java
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CannesParkingLine implements ParkingSourceLine {
    @JsonProperty("name_parking")      private String  NameOfParking;
    @JsonProperty("Places_disponible") private Integer emptySpace;
    @JsonProperty("Capacite_max")      private Integer totalSpace;
    @JsonProperty("_geopoint_coord")   private String  geoPoint;
}
```

`@JsonIgnoreProperties(ignoreUnknown = true)` est là pour une raison précise : les sources
publient bien plus de champs que les quatre qui m'intéressent (`tarif_horaire`,
`gestionnaire`, etc.). Sans cette annotation, **une ville qui ajoute un champ à son flux
casserait la désérialisation** du jour au lendemain. Avec, elle est absorbée sans rien changer.

Les types sont `Integer` et non `int` : une source peut publier `null` sur une place ou une
capacité (le jeu Cannes le fait volontairement), et un type primitif ferait échouer la
désérialisation.

---

## Le mapper : `ParkingMapper`

`@Component`, injecté dans `AbstractHttpProvider`. Il ne travaille que sur `ParkingSourceLine` :
il est donc écrit une seule fois et ne bougera plus, quel que soit le nombre de villes.

```java
public ParkingDto toParkingDto(ParkingSourceLine line) {
    ParkingDto parking = new ParkingDto();
    parking.setName(line.getNameOfParking());
    parking.setAvailable(line.getEmptySpace());
    parking.setCapacity(line.getTotalSpace());
    if (line.getEmptySpace() != null && line.getTotalSpace() != null) {
        parking.setOccupied(line.getTotalSpace() - line.getEmptySpace());
    }
    applyCoordinates(parking, line);
    return parking;
}
```

`occupied` est un champ **calculé**, pas lu : aucune source ne le publie. Le garde `!= null`
évite l'auto-unboxing d'un `Integer` nul ; en son absence, le parking « Croisette Grand Hotel »
du jeu Cannes (dont `Places_disponible` vaut `null`) ferait tomber toute la réponse. Ici, il
sort simplement avec `available: null` et `occupied: null`.

### Le parsing des coordonnées

```java
Optional<Coordinates> parseGeoPoint(String geoPoint) {
    if (geoPoint == null)            return Optional.empty();
    String[] part = geoPoint.split(",");
    if (part.length != 2)            return Optional.empty();
    try {
        return Optional.of(new Coordinates(Double.parseDouble(part[0].trim()),
                                           Double.parseDouble(part[1].trim())));
    } catch (NumberFormatException e) {
        return Optional.empty();
    }
}
```

Le choix de l'`Optional` plutôt que de l'exception est délibéré : **une coordonnée illisible
sur un parking ne doit pas faire échouer les trente autres.** Trois cas d'échec sont couverts,
et tous les trois existent réellement dans les données :

| Cas | Exemple réel | Résultat |
|---|---|---|
| champ absent | `"_geopoint_coord": null` (Marche Gambetta) | `Optional.empty()` |
| texte non numérique | `"coordonnees indisponibles"` (Suquet) | `Optional.empty()` |
| format inattendu | découpage ≠ 2 composantes | `Optional.empty()` |

`ifPresentOrElse` renseigne latitude/longitude si présentes, sinon trace un `log.warn` nommant
le parking. Le parking reste dans la liste, sans position. C'est une **dégradation partielle,
pas un échec** — la distinction structure toute la gestion d'erreur du projet.

`Coordinates` est un `record` imbriqué : porteur de deux valeurs, sans identité, sans
comportement. Exactement ce pour quoi les records existent.

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
    return city.trim().toLowerCase();
}
```

Appelée **aux deux moments** : à l'insertion et à la recherche. C'est cette symétrie qui rend
`?city=CANNES`, `?city=Cannes` et `?city=%20cannes%20` équivalents. Normaliser d'un seul côté
produirait des `404` incompréhensibles — une `HashMap` compare ses clés avec `equals()`, donc
sensible à la casse.

En cas d'absence, `getProviderCity()` lève `CityNotSupportedException` avec la liste des clés
connues dans le message : le client sait immédiatement ce qu'il peut demander.

---

## Les deux modes de recherche

### Par ville — strict

```java
public List<ParkingDto> getParking(String city) {
    return registry.getProviderCity(city).retrieveParkings();
}
```

Une recherche dans la map, un appel à une seule source. Exhaustif : tous les parkings de la
ville sont renvoyés, **y compris ceux dont les coordonnées sont absentes**. Aucun `try` : si la
source est morte, le `ProviderUnavailableException` remonte jusqu'au handler et devient un
`503`. C'est le comportement voulu — le client a nommé une ville précise, lui renvoyer une
liste vide en `200` serait un mensonge.

### Par position — tolérant

```java
public List<ParkingDto> getNearby(Double lat, Double lon, Double radiusMeters) {
    return collectAllParkings().stream()
        .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
        .map(p -> {
            p.setDistanceMetre((double) Math.round(
                CoordUtils.calculateDistance(lat, lon, p.getLatitude(), p.getLongitude())));
            return p;
        })
        .filter(p -> p.getDistanceMetre() <= radiusMeters)
        .sorted(Comparator.comparingDouble(ParkingDto::getDistanceMetre))
        .toList();
}
```

L'ordre des étapes est contraint : on ne peut pas filtrer sur une distance non calculée, ni
calculer une distance sans coordonnées.

Le filtre sur les coordonnées nulles n'est pas défensif par principe : l'open data de Poitiers
contient réellement deux parkings sans géolocalisation, et le jeu Cannes en contient deux
autres par construction. Sans ce filtre, le déballage du `Double` provoquerait une
`NullPointerException` sur des données de production.

`map(...)` renvoie le même objet après mutation plutôt que d'en construire un neuf : les
`ParkingDto` viennent d'être créés par le mapper, ils ne sont partagés avec personne, et les
copier n'apporterait rien ici.

### La dégradation partielle

```java
private List<ParkingDto> collectAllParkings() {
    List<ParkingDto> parking = new ArrayList<>();
    for (ParkingProvider provider : registry.getAllProviders()) {
        try {
            parking.addAll(provider.retrieveParkings());
        } catch (ProviderUnavailableException e) {
            log.warn("Provider {} indisponible, ignore pour cette requete", provider.getCity(), e);
        }
    }
    return parking;
}
```

C'est le **seul `try` du code métier**, et il est là pour une raison précise : chercher des
parkings « autour de moi » à Poitiers n'a aucune raison d'échouer parce que le serveur de
Cannes est tombé. La panne est tracée côté serveur et absorbée côté client.

Deux niveaux, deux politiques : strict quand le client désigne une ville, tolérant quand il
demande un agrégat. La démonstration se fait en une commande — voir *Démonstration de la
résilience* dans [USER_DOC.md](USER_DOC.md).

---

## Calcul de distance

`CoordUtils` applique une **approximation équirectangulaire** : conversion en radians, écart
nord-sud et écart est-ouest, correction de ce dernier par le cosinus de la latitude moyenne
(les méridiens se resserrent vers les pôles), puis Pythagore comme si la zone était plane. Le
résultat est multiplié par le rayon terrestre (6 371 000 m) pour passer des radians aux mètres.

Moins précise que la formule de Haversine sur de longues distances, mais nettement moins
coûteuse et d'une erreur inférieure à 0,1 % à l'échelle d'une ville — le seul cas d'usage ici.

Constructeur privé et méthode `static` : c'est une classe utilitaire pure, sans état, qu'on
n'instancie pas.

Le résultat est arrondi au mètre par `Math.round` dans le service, puis reconverti en `double`
pour tenir dans le champ `distanceMetre` : le JSON sort donc `"distanceMetre": 312.0`.

---

## Le client HTTP partagé

```java
@Configuration
public class HttpClientConfig {
    @Bean
    HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }
}
```

Un seul `HttpClient` pour toute l'application, injecté dans chaque provider par le constructeur
d'`AbstractHttpProvider`. `HttpClient` gère son propre pool de connexions et son propre
exécuteur de threads : en créer un par provider — ou pire, un par requête — reviendrait à jeter
le pool à chaque appel et à multiplier les threads.

Deux timeouts distincts, souvent confondus :

| Timeout | Où | Valeur | Ce qu'il borne |
|---|---|---|---|
| `connectTimeout` | `HttpClientConfig` | 3 s | l'établissement de la connexion TCP |
| `HttpRequest.timeout` | `AbstractHttpProvider` | 5 s | la requête complète, réponse comprise |

Le second est indispensable : sans lui, une source qui accepte la connexion puis ne répond
jamais bloquerait le thread indéfiniment.

### Choix de `java.net.http`

L'API native introduite avec Java 11, plutôt qu'une bibliothèque tierce : aucune dépendance
supplémentaire, et largement suffisante pour des `GET` simples. `RestClient` ou `WebClient`
auraient apporté de la configuration sans bénéfice à cette échelle.

À noter : l'`ObjectMapper` importé est celui de `tools.jackson.databind` (Jackson 3, embarqué
par Spring Boot 4), pas `com.fasterxml.jackson.databind` (Jackson 2). Les annotations, elles,
restent sous `com.fasterxml.jackson.annotation` — c'est déroutant mais correct.

---

## Pipeline d'erreurs

```
Provider / AbstractHttpProvider     Registry                    Controller
   │                                   │                            │
   ├ IOException                       ├ ville inconnue             ├ paramètre manquant
   ├ InterruptedException              │                            ├ type invalide
   ├ statut HTTP != 200                │                            ├ hors bornes / vide
   ├ JacksonException                  │                            │
   ▼                                   ▼                            ▼
ProviderUnavailableException   CityNotSupportedException    ConstraintViolation…
   │                                   │                     MethodArgumentNotValid…
   │                                   │                     MissingServletRequestParameter…
   │                                   │                     MethodArgumentTypeMismatch…
   └──────────── aucune couche intermédiaire n'intercepte ──────────┘
                        (sauf collectAllParkings, volontairement)
                                       │
                                       ▼
                          GlobalExceptionHandler
                            503 · 404 · 400 · 500
```

Le principe : **les exceptions traversent le service et le controller sans être attrapées.**
Elles héritent de `RuntimeException`, donc rien n'oblige les couches intermédiaires à les
déclarer ou à les gérer. C'est ce qui permet à `getParking()` de tenir en une ligne.

### Les sept handlers

| Handler | Déclencheur | Code | Corps |
|---|---|---|---|
| `invalidProvider` | `ProviderUnavailableException` | 503 | message de l'exception (nomme la ville) |
| `invalidCity` | `CityNotSupportedException` | 404 | message + villes prises en charge |
| `handleBadParam` | `MethodArgumentTypeMismatchException` | 400 | `"Parametres type invalid"` |
| `handleConstraint` | `ConstraintViolationException` | 400 | `chemin : contrainte`, joints par virgule |
| `handleMissingParam` | `MissingServletRequestParameterException` | 400 | `"Parametre obligatoire manquant : city"` |
| `handleValidation` | `MethodArgumentNotValidException` | 400 | `champ : contrainte`, joints par virgule |
| `globalException` | `Exception` | 500 | `"Une erreur interne est survenue"` + `log.error` |

Le dernier est le filet de sécurité : il attrape tout ce qui n'a pas été prévu. La stacktrace
part dans les logs serveur, le client ne reçoit qu'un message générique — un détail
d'implémentation dans une réponse HTTP est une fuite d'information.

Les trois handlers de validation ne se recouvrent pas, ils couvrent trois chemins différents de
Spring : `@RequestParam` validé au niveau classe (`ConstraintViolationException`), objet lié
et validé (`MethodArgumentNotValidException`), paramètre requis absent avant toute validation
(`MissingServletRequestParameterException`).

La traduction exception → code HTTP se fait à un seul endroit. Changer un code de retour, c'est
modifier une ligne.

---

## Validation des entrées

```java
@Validated
@RestController
@RequestMapping("/api/parking")
public class ParkingController {

    @GetMapping
    public List<ParkingDto> getParking(@RequestParam @NotBlank String city) { ... }

    @GetMapping("/proximity")
    public List<ParkingDto> getProximity(@Valid NearbyRequest request) { ... }
}
```

Deux mécanismes complémentaires :

- `@Validated` sur la classe active la validation des contraintes posées **directement sur les
  paramètres** (`@NotBlank String city`) ;
- `@Valid` sur `NearbyRequest` déclenche la validation des contraintes posées **sur les champs
  de l'objet** (`@NotNull`, `@Min`, `@Max`, `@Positive`).

`NearbyRequest` est un objet lié depuis les paramètres de requête, pas un corps JSON : `lat`,
`lon` et `radiusMetre` arrivent bien dans l'URL. Le regrouper en objet permet de poser les
bornes déclarativement plutôt qu'en `if` dans le controller.

| Champ | Contraintes | Pourquoi |
|---|---|---|
| `lat` | `@NotNull @Min(-90) @Max(90)` | domaine de définition d'une latitude |
| `lon` | `@NotNull @Min(-180) @Max(180)` | domaine de définition d'une longitude |
| `radiusMetre` | `@NotNull @Positive` | un rayon nul ou négatif n'a pas de sens |

Spring rejette la requête **avant** d'entrer dans la méthode : aucun appel réseau n'est
déclenché pour une requête invalide, et l'appelant reçoit un `400` qui désigne son erreur
plutôt qu'un `500` qui accuse le serveur.

Le principe appliqué : **valider aux frontières, faire confiance à l'intérieur.** Passé le
controller, le service et les providers travaillent sur des données saines, sans contrôle
défensif redondant.

---

## Le format public : `ParkingDto`

```java
@Data
public class ParkingDto {
    private String  name;
    private Integer available;
    private Integer capacity;
    private Integer occupied;
    private Double  longitude;
    private Double  latitude;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double distanceMetre;
}
```

**C'est le contrat que le sujet interdit de faire évoluer.** Renommer un champ ici casse
l'application mobile ; en ajouter un est en revanche sans danger.

`@JsonInclude(NON_NULL)` sur `distanceMetre` seulement : la distance n'a de sens que relative à
un point de recherche. Sur `/api/parking?city=…`, elle est absente du JSON plutôt que présente
à `null` — une distance nulle laisserait penser « à 0 m », ce qui serait faux.

Les autres champs, eux, sortent même à `null` : c'est une information utile (« la source ne
publie pas cette valeur »), distincte de l'absence de la notion.

---

## Configuration

`application.yaml` externalise l'URL de chaque source :

```yaml
parking:
  provider:
    poitiers:
      url: "https://data.grandpoitiers.fr/data-fair/api/v1/datasets/mobilites-stationnement-des-parkings-en-temps-reel/lines"
    cannes:
      url: "http://localhost:8081/mobilites-stationnement-des-parkings-en-temps-reel/lines"
```

Injectée par `@Value` dans le constructeur du provider correspondant. Les deux chemins sont
volontairement différents : c'est la démonstration qu'aucune convention n'est supposée
partagée entre les villes.

La source Cannes est simulée par un conteneur nginx (`docker-compose.yaml` + `data_city_api/`),
qui sert un jeu de données statique sur une URL arbitraire. Le fichier contient volontairement
des cas limites, chacun couvrant une branche du code :

| Parking | Anomalie | Branche exercée |
|---|---|---|
| Gare SNCF | `Places_disponible: 0` | zéro n'est pas `null`, `occupied` = capacité |
| Suquet Anciens Combattants | `_geopoint_coord: "coordonnees indisponibles"` | `catch (NumberFormatException)` |
| Marche Gambetta | `_geopoint_coord: null` | garde `geoPoint == null` |
| Croisette Grand Hotel | `Places_disponible: null` | garde `!= null` avant soustraction |
| tous | `tarif_horaire`, `gestionnaire` | `@JsonIgnoreProperties(ignoreUnknown = true)` |

---

## Tests

| Test | État | Ce qu'il couvre |
|---|---|---|
| `ParkingGatewayApplicationTests.contextLoads` | actif, **passe** | démarrage du contexte Spring : câblage des providers, résolution des `@Value`, construction du registry |
| `PoitiersProviderTests.invalidServeurRaise` | **entièrement commenté** | — |

Dernier rapport surefire : `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`.

`PoitiersProviderTests` a été écrit **avant** l'extraction d'`AbstractHttpProvider`. Il
instancie `new PoitiersProvider(new ObjectMapper(), new ParkingMapper())` — une signature à
deux arguments qui n'existe plus (le constructeur en prend quatre depuis que le `HttpClient`
et l'URL sont injectés). Il utilisait aussi `ReflectionTestUtils.setField(provider,
"providerUrlApi", …)`, qui ne fonctionnerait plus : le champ appartient désormais à la classe
mère et il est `final`.

La bonne façon de le réécrire n'est pas la réflexion mais l'injection : passer l'URL bidon au
constructeur, qui l'accepte déjà en quatrième argument. C'est un effet de bord bienvenu du
refactoring — le code est devenu plus testable en même temps qu'il se factorisait.

Le plan de reprise, par ordre de rentabilité, est détaillé dans la section « État des tests »
du [README](../README.md).

---

## Ajouter une ville

Trois fichiers, aucune modification de l'existant.

**1. `dto/{ville}/` — le reflet du JSON de la source**

```java
@Data @JsonIgnoreProperties(ignoreUnknown = true)
public class LyonParkingLine implements ParkingSourceLine {
    @JsonProperty("<nom réel>") private String  NameOfParking;
    @JsonProperty("<nom réel>") private Integer emptySpace;
    @JsonProperty("<nom réel>") private Integer totalSpace;
    @JsonProperty("<nom réel>") private String  geoPoint;
}

@Data @JsonIgnoreProperties(ignoreUnknown = true)
public class LyonResponse {
    @JsonProperty("<clé de la liste>") private List<LyonParkingLine> results;
}
```

**2. `provider/{ville}/{Ville}Provider.java` — `@Component`, étend `AbstractHttpProvider<R>`**

Trois méthodes : `getCity()`, `responseType()`, `extractLines()`. Rien d'autre.

**3. `application.yaml` — l'URL**

```yaml
parking:
  provider:
    lyon:
      url: "https://…"
```

Le registry détecte le nouveau provider au démarrage. Le controller, le service, le mapper, le
format de réponse et l'URL consommée par l'application mobile restent **strictement
inchangés** — c'est précisément ce que demandait le sujet.

Si une future source n'est pas du JSON sur HTTP (CSV, SOAP, base de données), elle implémente
directement `ParkingProvider` sans passer par `AbstractHttpProvider`. Le registry ne fait pas
la différence : il n'indexe que le contrat.

---

## Points connus

Relevés à la relecture, sans incidence fonctionnelle, notés pour mémoire :

- `ParkingService` déclare son logger avec `LoggerFactory.getLogger(ParkingProvider.class)` :
  les `log.warn` de dégradation partielle apparaissent donc sous le nom de catégorie
  `ParkingProvider` et non `ParkingService`.
- Le paramètre de `getProximity` s'appelle `requets` (inversion), et `PoitierParkingLine` /
  `PoitierResponse` sont au singulier alors que le package est `poitiers`.
- `@RequiredArgsConstructor` sur `GlobalExceptionHandler` ne génère rien : la classe n'a aucun
  champ `final` à injecter.
- `CoordUtils` a un constructeur privé mais la classe n'est pas `final` ; son constructeur privé
  se termine par un `;` superflu.
- Le `.PHONY` du Makefile déclare `start-serveur`, `clean` et `re`, qui ne correspondent à
  aucune cible existante.
- `handleBadParam` reçoit l'exception mais renvoie un message fixe : le nom du paramètre fautif
  (`e.getName()`) serait plus utile au client.