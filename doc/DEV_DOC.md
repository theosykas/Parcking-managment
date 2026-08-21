*application.yaml*
permet de lancer sur le serveur au port 8080 (localhost)
on vas donc posseder une partie provider pour pouvoir ajouter les data des villes que l'on souhaite traiter

```bash
 parking:
     provider:
      potiers:
         url: "https://data.grandpoitiers.fr/data-fair/api/v1/datasets/mobilites-stationnement-des-parkings-en-temps-reel/lines"
```bash

---
## VALIDATION ET CONTRACT (interface)

/dto
le dto permet de pouvoir valider des donnés pour etre renvoyer j'ai donc implementer une class *ParkingDto* qui prend 3 parametres le total de place libre mais aussi les places prise et les noms de parking , j'ai donc par la suite crée un *ParkingProvider* qui applique une interface qui va donc etre un contrat pour determiner que chaques appels provider pour la ville corespondante je renvoie donc cette liste generique et non assigné a Poitiers seulement mais pour tout les villes et providers que l'application.yaml contiendras

---

### Implémentation du Provider (Appels HTTP)

Pour interroger l'API de la ville de Poitiers, je récupère l'URL injectée depuis mon fichier `application.yaml` via `@Value`. 

J'ai fait le choix d'utiliser l'API native **`java.net.http`** (introduite avec Java 11) plutôt que des librairies tierces :
* **Le Client :** Un `HttpClient` est instancié au démarrage du composant.
* **Le Contrat (`@Override`) :** J'implémente la méthode de l'interface `ParkingProvider` pour construire et envoyer ma requête `HttpRequest`.
* **Gestion des erreurs (Wrapping) :** Si la requête échoue (API morte, coupure réseau), j'intercepte l'erreur technique native de Java (`IOException`). Je lève à la place ma propre exception métier. Mon `GlobalExceptionHandler` prend alors le relais pour masquer les logs complexes à l'utilisateur et lui renvoyer une erreur propre et compréhensible au format JSON.
---