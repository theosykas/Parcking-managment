package fr.theosykas.parking.provider.poitiers;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper; 
import java.io.IOException;
import fr.theosykas.parking.dto.ParkingDto;
import fr.theosykas.parking.dto.poitiers.PoitierParkingLine;
import fr.theosykas.parking.dto.poitiers.PoitierResponse;
import fr.theosykas.parking.exception.ProviderInterrupted;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import fr.theosykas.parking.provider.ParkingProvider;
import org.slf4j.Logger;

// doit contenir le contrat interface /dto/Response
@Component // java va cree et instencier l'objet
// declaration class == signature du contrat implements ParkingProvider check si RetrivalParkingData est la et au bon return
public class PoitiersProvider implements ParkingProvider {
	@Value("${parking.provider.poitiers.url}")
	private String providerUrlApi;

	private final HttpClient client;
	// le moteur de traduction est construit une fois au démarrage, puis réutilisé à chaque appel.
	private final ObjectMapper mapper;
	private static final Logger log = LoggerFactory.getLogger(PoitiersProvider.class);

	// on instencie client une seule fois
	public PoitiersProvider(ObjectMapper mapper) {
		this.mapper = mapper;
		this.client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(3))
			.build();
	}

	@Override
	public String getCity() {
		return "Poitiers";
	}

	@Override
	public List<ParkingDto> retrieveParkings() {
		List<ParkingDto> finalParkingList = new ArrayList<>();

		try {
			HttpRequest request = HttpRequest.newBuilder()
						.uri(URI.create(providerUrlApi))
						.timeout(Duration.ofSeconds(9))
						.GET()
						.build();
		
			HttpResponse<String> reponse = client.send(
				request, HttpResponse.BodyHandlers.ofString());
			
			if (reponse.statusCode() != 200) {
				throw new ProviderInterrupted("Le serveur Poitiers repond" + reponse.statusCode(), null);
			}
			PoitierResponse dataCity = mapper.readValue(reponse.body(), PoitierResponse.class);
			if (dataCity.getResults() != null) {
				for (PoitierParkingLine line: dataCity.getResults()) {

					ParkingDto parking = new ParkingDto();
					parking.setNameOfParking(line.getNameOfParking());
					parking.setEmptySpace(line.getEmptySpace());
					parking.setTotalSpace(line.getTotalSpace());
					if (line.getGeoPoint() != null) {  // renvoie un "null" au lieu d'une exception et crash on laisse el qeul
						String[] formatPoint = line.getGeoPoint().split(",");
						if (formatPoint.length == 2) {
							try {
								parking.setLatitude(Double.parseDouble(formatPoint[0].trim()));  // reupere les potions pour les return a la list
								parking.setLongitude(Double.parseDouble(formatPoint[1].trim()));
							}
							catch (NumberFormatException e){
								log.warn("Coordonnées illisibles pour le parking {} : {}",
											line.getNameOfParking(), line.getGeoPoint()
								);
							}
						}
					}
					if (line.getEmptySpace() != null && line.getTotalSpace() != null) {
						parking.setOccupied(line.getTotalSpace() - line.getEmptySpace());
					}

					finalParkingList.add(parking);
				}
			}
		}
		catch (IOException e) {
			throw new ProviderInterrupted("Erreur le serveur Poitier est injoignable", e);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();  // obligatoire on return flag interuption
			throw new ProviderInterrupted("Erreur le serveur Poitier est interrompue", e);
		}
		catch (JacksonException e) {
			throw new ProviderInterrupted("Réponse illisible du serveur de Cannes", e);
		}
		return finalParkingList;
	}
}


// Un package par provider, c'est du package-by-feature, et c'est exactement ce que ton sujet réclame. Le critère : « ajouter une ville = ajouter un dossier, sans toucher au reste ». Avec un package plat, provider/ finit avec 15 classes mélangées de 5 villes.


// La règle stricte de Java : Dans une classe Java, l'espace global (le corps de la classe) ne sert qu'à déclarer des variables (l'état). Toute l'action, la logique, les calculs ou la création d'objets complexes (comme ta requête) doivent obligatoirement se passer à l'intérieur d'une méthode (le comportement).


// global handler le try catch l'erreur donc tout s'arrete
// le throw dit que c'est tel erreur ProviderInteruped Spring va chercher
// si quelq'un sait reagir et trouve globalHandler qui va donc raise le message erreur correspondant @ExceptionHandler(ProviderInterrupted.class)

// parking.setNameOfParking( line.getName() );
// //  ↑         ↑                ↑     ↑
// //  |         |                |     └── ON LIT ici (source)
// //  |         |                └──────── dans l'objet "line" (PoitiersParkingLine)
// //  |         └───────────────────────── ON ÉCRIT ici (destination)
// //  └─────────────────────────────────── dans l'objet "parking" (ParkingDto)