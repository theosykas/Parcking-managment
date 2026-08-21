---
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
