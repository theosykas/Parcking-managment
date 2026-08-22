package fr.theosykas.parking.service;
import java.util.List;

import org.springframework.stereotype.Service;
import fr.theosykas.parking.dto.ParkingDto;
import fr.theosykas.parking.provider.ParkingProviderRegistry;

@Service
public class ParkingService {
	private final ParkingProviderRegistry registry;

	public ParkingService(ParkingProviderRegistry registry) {
		this.registry = registry;
	}

	// sinon ca aurait donner ca sans le getCity
	// public List<ParkingDto> getParkings(String city) {
	//     if (city.equals("poitiers")) return poitiers.retrieveParkings();
	//     if (city.equals("cannes"))   return cannes.retrieveParkings();
	//     throw new CityNotSupportedException(...);
	// }
	public List<ParkingDto> getParking(String city) {
		return registry.getProviderCity(city).retrieveParkings();
	}

	// public List<ParkingDto> getNearby(Double lat, Double lon, Double radiusMeters) {
	// 	return registry.getNearbyParking(lat, lon, radiusMeters);
	// }
}