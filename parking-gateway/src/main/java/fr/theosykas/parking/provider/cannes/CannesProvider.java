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

// Seul endroit du projet qui connait le format de cette ville.
@Component
public class CannesProvider implements ParkingProvider{
	@Value("${parking.provider.cannes.url}")
	private String providerUrlApi;

	private final HttpClient client;
	private final ObjectMapper mapper;
	private final CannesMapper cannesMapper;

	// Instancie une seule fois : HttpClient gere son propre pool de connexions et de threads, en recreer un par appel
	public CannesProvider(ObjectMapper mapper, CannesMapper cannesMapper) {
		this.mapper = mapper;
		this.cannesMapper = cannesMapper;
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

			// JSON Cannes ->
			CannesResponse dataCity = mapper.readValue(response.body(), CannesResponse.class);
			//  objets Java (cannesMapper) ->
			if (dataCity.getResults() != null) {
					for (CannesParkingLine line: dataCity.getResults()) {
						// ParkingDto
						finalParkingList.add(cannesMapper.mapper(line));
					}
				}
			}
			catch (IOException e) {
				throw new ProviderInterrupted(" le serveur Cannes est injoignable", e);
			}
			 // on masque l'intrruption du flag thread que IInterruptedException raise lui meme .interupt()
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new ProviderInterrupted(" le serveur Cannes est interrompue", e);
			}
			catch (JacksonException e) {
				throw new ProviderInterrupted(" Réponse illisible du serveur de Cannes", e);
			}
			return finalParkingList;
		}
}
