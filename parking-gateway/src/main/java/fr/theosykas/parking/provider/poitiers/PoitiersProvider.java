package fr.theosykas.parking.provider.poitiers;

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

@Component // java va cree et instencier l'objet
public class PoitiersProvider implements ParkingProvider {
	@Value("${parking.provider.poitiers.url}")
	private String providerUrlApi;

	private final HttpClient client;
	private final ObjectMapper mapper;
	 private final PoitiersMapper poitiersMapper;

	public PoitiersProvider(ObjectMapper mapper, PoitiersMapper poitiersMapper) {
		this.mapper = mapper;
		this.poitiersMapper = poitiersMapper;
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
				throw new ProviderInterrupted(" Le serveur Poitiers ne repond pas ", null);
			}

			PoitierResponse dataCity = mapper.readValue(reponse.body(), PoitierResponse.class);
			if (dataCity.getResults() != null) {
				for (PoitierParkingLine line: dataCity.getResults()) {
					finalParkingList.add(poitiersMapper.mapper(line));
				}
			}
		}
		catch (IOException e) {
			throw new ProviderInterrupted("Erreur le serveur Poitier est injoignable", e);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ProviderInterrupted("Erreur le serveur Poitier est interrompue", e);
		}
		catch (JacksonException e) {
			throw new ProviderInterrupted("Réponse illisible du serveur de Poitier", e);
		}
		return finalParkingList;
	}
}
