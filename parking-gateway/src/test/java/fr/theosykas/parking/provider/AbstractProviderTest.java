package fr.theosykas.parking.provider;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.http.HttpClient;

import org.junit.jupiter.api.Test;
import fr.theosykas.parking.exception.ProviderUnavailableException;
import fr.theosykas.parking.provider.poitiers.PoitiersProvider;
import tools.jackson.databind.ObjectMapper;

public class AbstractProviderTest {

	@Test
	void unreachableServerThrowsProviderUnavailable() {
		PoitiersProvider provider = new PoitiersProvider(HttpClient.newHttpClient(), new ObjectMapper(),
                      new ParkingMapper(),
                      "http://localhost:4242/nonexistent");
		assertThrows(ProviderUnavailableException.class, provider::retrieveParkings);
	}
}