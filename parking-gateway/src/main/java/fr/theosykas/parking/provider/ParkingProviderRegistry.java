package	fr.theosykas.parking.provider;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import fr.theosykas.parking.exception.CityNotSupportedException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

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

	public ParkingProvider getProviderCity(String city) {
		ParkingProvider provider = providerByCity.get(normalize(city));
		if (provider == null) {
			throw new CityNotSupportedException(
				"city not supported " + city);
		}
		return provider;
	}

	public Set<String> getValidCity() {
		return providerByCity.keySet();
	}

	private String normalize(String city) {
		return city.trim().toLowerCase();
	}
}