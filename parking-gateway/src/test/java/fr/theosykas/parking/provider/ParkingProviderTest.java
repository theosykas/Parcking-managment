package fr.theosykas.parking.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import fr.theosykas.parking.dto.ParkingDto;
import fr.theosykas.parking.exception.CityNotSupportedException;

public class ParkingProviderTest {

	private record CityMockProvider(String nameCity) implements ParkingProvider {
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
			registry.getProviderForCity("Bordeaux");
		});
	}

	@Test
	void shouldFindProviderWhateverTheCase() {
		CityMockProvider cityMockProvider = new CityMockProvider("Poitiers");
		ParkingProviderRegistry registry = new ParkingProviderRegistry(List.of(cityMockProvider));

		assertEquals(cityMockProvider, registry.getProviderForCity("Poitiers"));
		assertEquals(cityMockProvider, registry.getProviderForCity("PoItieRs"));
		assertEquals(cityMockProvider, registry.getProviderForCity("POITIERS"));
	}
}