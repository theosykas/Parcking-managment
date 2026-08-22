package fr.theosykas.parking.provider.cannes;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper; 
import java.io.IOException;
import fr.theosykas.parking.dto.ParkingDto;
import fr.theosykas.parking.dto.cannes.CannesParkingLine;
import fr.theosykas.parking.dto.cannes.CannesResponse;
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
import org.slf4j.LoggerFactory;

@Component
public class CannesProvider implements ParkingProvider{
	@Value("${parking.provider.cannes.url}")
	private String providerUrlApi;

	private final HttpClient client;
	private final ObjectMapper mapper;
	private static final Logger log = LoggerFactory.getLogger(CannesProvider.class);

	public CannesProvider(ObjectMapper mapper) {
		this.mapper = mapper;
		this.client = HttpClient.newBuilder()
						.connectTimeout(Duration.ofSeconds(3))
						.build();
		}

	@Override
	public String getCity() {
		return "Cannes";
	}

	@Override
	public List<ParkingDto> retrieveParkings() {
		List<ParkingDto> finalParkingList = new ArrayList<>();

		try {
			HttpRequest request = HttpRequest.newBuilder()
							.uri(URI.create(providerUrlApi))
							.timeout(Duration.ofSeconds(5))
							.GET()
							.build();
	
			HttpResponse<String> response = client.send(
				request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200) {
				throw new ProviderInterrupted("Le serveur Cannes repond: " + response.statusCode(), null);
			}
			CannesResponse dataCity = mapper.readValue(response.body(), CannesResponse.class);
			if (dataCity.getResults() != null) {
					for (CannesParkingLine line: dataCity.getResults()) {

						ParkingDto parking = new ParkingDto();
						parking.setName(line.getNameOfParking());
						parking.setAvaiable_space(line.getEmptySpace());
						parking.setCapacity(line.getTotalSpace());
						if (line.getGeoPoint() != null) {
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
				throw new ProviderInterrupted("Erreur le serveur Cannes est injoignable", e);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new ProviderInterrupted("Erreur le serveur Cannes est interrompue", e);
			}
			catch (JacksonException e) {
				throw new ProviderInterrupted("Réponse illisible du serveur de Cannes", e);
			}
			return finalParkingList;
		}
}


// Oui, l'arborescence a nettement progressé — et surtout elle raconte la bonne histoire : un DTO public (ParkingDto) séparé des DTO bruts par ville (dto/poitiers, dto/cannes), une interface au niveau provider/ avec une implémentation par ville dans son propre sous-package. C'est exactement la structure qu'attend la consigne « l'API exposée ne doit pas évoluer » : le format crade de chaque ville est confiné dans son coin, il ne remonte jamais jusqu'au controller.

// C'est ça, à une nuance près sur le « Spring va chercher ».

// Ce n'est pas le if qui déclenche Spring — c'est ton throw. Le if ne fait que détecter l'anomalie ; c'est toi qui décides d'en faire une exception :

// java
// if (response.statusCode() != 200) {          // détection
//     throw new ProviderInterrupted(...);      // décision ← c'est ça qui part
// }

// Ensuite l'exception remonte toute seule jusqu'à la frontière du controller. Là, Spring regarde ses @RestControllerAdvice et cherche un @ExceptionHandler dont le type correspond à la classe de l'exception. Il prend le plus spécifique : ProviderInterrupted d'abord, et seulement s'il n'en trouve aucun, il retombe sur ton @ExceptionHandler(Exception.class).