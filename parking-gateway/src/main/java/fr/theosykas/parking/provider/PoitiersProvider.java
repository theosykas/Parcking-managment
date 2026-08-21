package fr.theosykas.parking.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.IOException;
import fr.theosykas.parking.dto.PoitiersResponse;
import fr.theosykas.parking.dto.ParkingDto;
import fr.theosykas.parking.exception.ProviderInterrupted;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

// doit contenir le contrat interface /dto/Response
@Component // java va cree et instencier l'objet
// declaration class == signature du contrat implements ParkingProvider check si RetrivalParkingData est la et au bon return
public class PoitiersProvider implements ParkingProvider {
	@Value("${parking.provider.potiers.url}")
	private String providerUrlApi;

	private final HttpClient client;

	// on instencie client une seule fois
	public PoitiersProvider() {
		this.client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(3))
			.build();
	}

	@Override
	public List<ParkingDto> retrieveParkings() {
		List<ParkingDto> finalParkingList = new ArrayList<>();
		ObjectMapper mapper = new ObjectMapper();

		try {
			HttpRequest request = HttpRequest.newBuilder()
						.uri(URI.create(providerUrlApi))
						.timeout(Duration.ofSeconds(9))
						.GET()
						.build();
		
			HttpResponse<String> reponse = client.send(
				request, HttpResponse.BodyHandlers.ofString());
			
			PoitiersResponse dataCity = mapper.readValue(reponse.body(), PoitiersResponse.class);
			if (dataCity.getResults() != null) {
				for (PoitiersResponse line: dataCity.getResults()) {

					ParkingDto parking = new ParkingDto();
					parking.setNameOfParking(line.getNameOfParking());
					parking.setEmptySpace(line.getEmptySpace());
					parking.setTotalSpace(line.getTotalSpace());
					if (line.getEmptySpace() != null && line.getTotalSpace() != null) {
						parking.setOccupied(line.getTotalSpace() - line.getEmptySpace());
					}

					finalParkingList.add(parking);
				}
			}
		}
		catch (IOException | InterruptedException e) {
			throw new ProviderInterrupted("Erreur le serveur Poitier est injoignable");
		}
		return finalParkingList;
	}
}

// La règle stricte de Java : Dans une classe Java, l'espace global (le corps de la classe) ne sert qu'à déclarer des variables (l'état). Toute l'action, la logique, les calculs ou la création d'objets complexes (comme ta requête) doivent obligatoirement se passer à l'intérieur d'une méthode (le comportement).


// global handler le try catch l'erreur donc tout s'arrete
// le throw dit que c'est tel erreur ProviderInteruped Spring va chercher
// si quelq'un sait reagir et trouve globalHandler qui va donc raise le message erreur correspondant @ExceptionHandler(ProviderInterrupted.class)