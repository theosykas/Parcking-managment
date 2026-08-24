package fr.theosykas.parking.provider;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.net.http.HttpRequest;
import fr.theosykas.parking.dto.ParkingDto;
import fr.theosykas.parking.dto.ParkingSourceLine;
import fr.theosykas.parking.exception.ProviderUnavailableException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

// abstraction promet de remplir le contrat (interface) ParkingProvider a condition de recevoir responseType rt extractline
public abstract class AbstractHttpProvider<R> implements ParkingProvider{
	private final static Duration TIME_OUT_REQUEST = Duration.ofSeconds(5);

	private final HttpClient client;
	private final ObjectMapper mapper;
	private final ParkingMapper parkingMapper;
	private final String providerUrlApi;

	public AbstractHttpProvider(
								HttpClient client,
	                            ObjectMapper mapper,
								ParkingMapper parkingMapper,
								String providerUrlApi)
	{
		this.client = client;
		this.mapper = mapper;
		this.parkingMapper = parkingMapper;
		this.providerUrlApi = providerUrlApi;
	}

	// methode du contrat @Overide /PoitierProvider/CannesProvier
	protected abstract Class<R> responseType();
	protected abstract List<? extends ParkingSourceLine> extractLines(R response);

	// retrieveParkings() appeler dans le service == list des parking en format dto
	@Override
	public final List<ParkingDto> retrieveParkings() {
		R response;
		try {
			// responseType() lui donne PoitierResponse.class Jackson inspecte cette classe et va voir PoitierParkingLine
			response = mapper.readValue(fetchBodyResponse(), responseType());
		}
		catch (JacksonException e) {
			throw new ProviderUnavailableException(" Réponse illisible du serveur de " + getCity(), e);
		}
		List<ParkingDto> finalParkingList = new ArrayList<>();
		List<? extends ParkingSourceLine> lines = extractLines(response);  // format de ville 
		if (lines != null) {
			for (ParkingSourceLine line : lines) {
				finalParkingList.add(parkingMapper.toParkingDto(line));
			}
		}
		return finalParkingList; // format ParkingDto/ return finalParkingList remonte au ParkingService
	}

	// récupère le JSON en texte (la ligne est cachée dans l'appel imbriqué) == readValue(fetchBodyResponse(), responseType());
	private String fetchBodyResponse() {
		HttpRequest request = HttpRequest.newBuilder()
							.uri(URI.create(providerUrlApi))
							.timeout(TIME_OUT_REQUEST)
							.GET()
							.build();
	
		try {
			HttpResponse<String> response = client.send(
				request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				// serveur provider url invalid
				throw new ProviderUnavailableException("Le serveur repond: " + response.statusCode(), null);
			}
			return response.body();
		}
		catch (IOException e) {
			throw new ProviderUnavailableException(" le serveur " + getCity() + " est injoignable" , e);
		}
		catch (InterruptedException e) {
			// on masque l'intrruption du flag thread que IInterruptedException raise lui meme .interupt()
			Thread.currentThread().interrupt();
			throw new ProviderUnavailableException(" le serveur est " + getCity() + " interrompue", e);
			}
	}
}
