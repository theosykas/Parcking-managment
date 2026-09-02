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

public abstract class AbstractHttpProvider<R> implements ParkingProvider {
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

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

	protected abstract Class<R> responseType();
	protected abstract List<? extends ParkingSourceLine> extractLines(R response);

	@Override
	public final List<ParkingDto> retrieveParkings() {
		R response;
		try {
			response = mapper.readValue(fetchResponseBody(), responseType());
		}
		catch (JacksonException e) {
			throw new ProviderUnavailableException("Réponse illisible du serveur de " + getCity(), e);
		}
		List<ParkingDto> finalParkingList = new ArrayList<>();
		List<? extends ParkingSourceLine> lines = extractLines(response);
		if (lines != null) {
			for (ParkingSourceLine line : lines) {
				finalParkingList.add(parkingMapper.toParkingDto(line));
			}
		}
		return finalParkingList;
	}

	private String fetchResponseBody() {
		HttpRequest request = HttpRequest.newBuilder()
							.uri(URI.create(providerUrlApi))
							.timeout(REQUEST_TIMEOUT)
							.GET()
							.build();
	
		try {
			HttpResponse<String> response = client.send(
				request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				throw new ProviderUnavailableException("Le serveur répond : " + response.statusCode(), null);
			}
			return response.body();
		}
		catch (IOException e) {
			throw new ProviderUnavailableException("Le serveur " + getCity() + " est injoignable", e);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ProviderUnavailableException("Le serveur " + getCity() + " est interrompu", e);
		}
	}
}
