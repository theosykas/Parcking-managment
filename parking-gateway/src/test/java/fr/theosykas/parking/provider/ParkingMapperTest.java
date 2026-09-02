package fr.theosykas.parking.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import fr.theosykas.parking.dto.ParkingDto;
import fr.theosykas.parking.dto.ParkingSourceLine;

public class ParkingMapperTest {
	private final ParkingMapper mapper = new ParkingMapper();

	private record FakeSourceLine(String getParkingName, Integer getEmptySpaces,
                                 Integer getTotalSpaces, String getGeoPoint) implements ParkingSourceLine {}
	
	@Test
	void geoPointValid() {
		var coord = mapper.parseGeoPoint("46.5836, 0.3348").orElseThrow();
		assertEquals(46.5836, coord.latitude());
		assertEquals(0.3348, coord.longitude());
	}

	@Test
	void geoPointTextual() {
		assertTrue(mapper.parseGeoPoint("coordonnées indisponibles").isEmpty());
	}

	@Test
	void geoPointNull() {
		assertTrue(mapper.parseGeoPoint(null).isEmpty());
	}

	@Test
	void geoPointInvalid() {
		assertTrue(mapper.parseGeoPoint("46.5836,0.3348,12").isEmpty());
		assertTrue(mapper.parseGeoPoint("abc,def").isEmpty());
	}

	@Test
	void calculateOccupiedFromCapacityAndAvailable() {
		ParkingDto dto = mapper.toParkingDto(new FakeSourceLine("GARE TOUMAI", 416, 640, "46.5836,0.3348"));
		assertEquals(224, dto.getOccupied());
		assertEquals(416, dto.getAvailable());
	}

	@Test
	void coordinateInvalid() {
		ParkingDto dto = mapper.toParkingDto(
		new FakeSourceLine("Suquet", 14, 145, "coordonnees indisponibles"));

		assertNull(dto.getLatitude());
		assertNull(dto.getLongitude());
		assertEquals("Suquet", dto.getName());
	}
}