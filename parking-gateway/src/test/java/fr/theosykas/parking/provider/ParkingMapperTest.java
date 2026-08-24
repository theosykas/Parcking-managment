package fr.theosykas.parking.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import fr.theosykas.parking.dto.ParkingDto;
import fr.theosykas.parking.dto.ParkingSourceLine;

// Le record LigneSource marche parce que ParkingSourceLine déclare getNameOfParking() / getEmptySpace() / getTotalSpace() / getGeoPoint() : un record dont les composants portent ces noms génère exactement ces accesseurs. C'est légal mais inhabituel — si tu préfères ne pas avoir à l'expliquer en entretien, remplace-le par une petite classe statique avec quatre champs et quatre getters, c'est plus banal à lire.
public class ParkingMapperTest {
	private final ParkingMapper mapper = new ParkingMapper();

	private record LigneSource(String getNameOfParking, Integer getEmptySpace,
                                 Integer getTotalSpace, String getGeoPoint)
                      implements ParkingSourceLine {}
	
	@Test
	void geoPointEst() {
		var coord = mapper.parseGeoPoint("46.5836, 0.3348").orElseThrow();
		assertEquals(46.5836, coord.latitude());
		assertEquals(0.3348, coord.longitude());
	}

	@Test
	void geoPointConvertTextual() {
		assertTrue(mapper.parseGeoPoint("coordonnees indisponibles").isEmpty());
	}

	@Test
	void geoPointNull() {
		assertTrue(mapper.parseGeoPoint(null).isEmpty());
	}

	@Test
	void geoPointInvalide() {
		assertTrue(mapper.parseGeoPoint("46.5836,0.3348,12").isEmpty());
		assertTrue(mapper.parseGeoPoint("abc,def").isEmpty());
	}

	@Test
	void calculateOccupiedFromCapacityAndAvaiable() {
		ParkingDto dto = mapper.toParkingDto(new LigneSource("GARE TOUMAI", 416, 640, "46.5836,0.3348"));
		assertEquals(224, dto.getOccupied());
		assertEquals(416, dto.getAvailable());
	}

	@Test
	void CoordinateInvalide() {
		ParkingDto dto = mapper.toParkingDto(
		new LigneSource("Suquet", 14, 145, "coordonnees indisponibles"));

		assertNull(dto.getLatitude());
		assertNull(dto.getLongitude());
		assertEquals("Suquet", dto.getName());
	}
}