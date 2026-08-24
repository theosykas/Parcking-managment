package fr.theosykas.parking.provider;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.http.HttpClient;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import fr.theosykas.parking.exception.ProviderUnavailableException;
import fr.theosykas.parking.provider.poitiers.PoitiersProvider;
import tools.jackson.databind.ObjectMapper;

public class AbstractProviderTest {

	@Test
	// check si l'exception est bien lever status en vert
	void invalidServeurRaise() {
		PoitiersProvider provider = new PoitiersProvider(HttpClient.newHttpClient(), new ObjectMapper(),
                      new ParkingMapper(),
                      "http://localhost:4242/notExisting");
		ReflectionTestUtils.setField(provider, "providerUrlApi", "http://localhost:4242/notExisting");
		assertThrows(ProviderUnavailableException.class, provider::retrieveParkings);
	}
}