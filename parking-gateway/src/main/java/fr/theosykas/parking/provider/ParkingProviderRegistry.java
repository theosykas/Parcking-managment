package	fr.theosykas.parking.provider;
import java.util.*;
import org.springframework.stereotype.Component;
import fr.theosykas.parking.exception.CityNotSupportedException;

@Component
public class ParkingProviderRegistry {
	private final Map<String, ParkingProvider> providerByCity = new HashMap<>();

	public ParkingProviderRegistry(List<ParkingProvider> providers) {
		for (ParkingProvider provider: providers) {
			providerByCity.put(normalize(provider.getCity()), provider);
		}
	}

	public Collection<ParkingProvider> getAllProviders() {
		return providerByCity.values();
	}

	public ParkingProvider getProviderForCity(String city) {
		ParkingProvider provider = providerByCity.get(normalize(city));
		if (provider == null) {
			throw new CityNotSupportedException(
				"Ville non prise en charge : " + city + ". Villes disponibles : " + providerByCity.keySet());
		}
		return provider;
	}

	private String normalize(String city) {
		return city.trim().toLowerCase(Locale.ROOT);
	}
}