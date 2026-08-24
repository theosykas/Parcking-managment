package fr.theosykas.parking.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import fr.theosykas.parking.dto.ParkingDto;
import fr.theosykas.parking.exception.CityNotSupportedException;
import lombok.RequiredArgsConstructor;

public class ParkingProviderTest {

	@RequiredArgsConstructor
	private static class CityMockProvider implements ParkingProvider {
		private final String nameCity;

	@Override
	public String getCity() {
		return nameCity;
	}

	@Override
	public List<ParkingDto> retrieveParkings() {
		return List.of();
		}
	}

	@Test
	void shouldThrowExceptionWhenCityIsUnknown() {
		ParkingProviderRegistry registry = new ParkingProviderRegistry(List.of(new CityMockProvider("Poitiers")));
		assertThrows(CityNotSupportedException.class, () -> {
			registry.getProviderCity("Bordeaux");
		});
	}

	@Test
	void AssertNormalizeSearchParametre() {
		CityMockProvider cityMockProvider = new CityMockProvider("Poitiers");
		ParkingProviderRegistry registry = new ParkingProviderRegistry(List.of(cityMockProvider));

		// strictement equal a POitiers mok
		assertEquals(cityMockProvider, registry.getProviderCity("Poitiers"));
		assertEquals(cityMockProvider, registry.getProviderCity("poiTiers"));
		assertEquals(cityMockProvider, registry.getProviderCity("POITIERS"));
	}
}