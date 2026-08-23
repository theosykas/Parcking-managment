package	fr.theosykas.parking.provider;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import fr.theosykas.parking.exception.CityNotSupportedException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;


//Spring injecte ici toutes les implementations de ParkingProvider // Ajouter une ville = creer une classe @Component, aucun enregistrement manuel.
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
				"city not supported " + city + " ville prise en charges " + providerByCity.keySet());
		}
		return provider;
	}

	public Set<String> getValidCity() {
		return providerByCity.keySet();
	}

	// appeler pendant l'insertion ca permet de pouvoir ?city="CANNES" || ?city"cannes"
	private String normalize(String city) {
		return city.trim().toLowerCase();
	}
}