// package fr.theosykas.parking.provider;

// import static org.junit.jupiter.api.Assertions.assertThrows;
// import org.junit.jupiter.api.Test;
// import org.springframework.test.util.ReflectionTestUtils;
// import fr.theosykas.parking.exception.ProviderUnavailableException;
// import fr.theosykas.parking.provider.ParkingMapper;
// import fr.theosykas.parking.provider.poitiers.PoitiersProvider;
// import tools.jackson.databind.ObjectMapper;

// public class PoitiersProviderTests {

// 	@Test
// 	// check si l'exception est bien lever status en vert
// 	void invalidServeurRaise() {
// 		PoitiersProvider provider = new PoitiersProvider(new ObjectMapper(), new ParkingMapper());
// 		ReflectionTestUtils.setField(provider, "providerUrlApi", "http://localhost:4242/notExisting");
// 		assertThrows(ProviderUnavailableException.class, provider::retrieveParkings);
// 	}
// }