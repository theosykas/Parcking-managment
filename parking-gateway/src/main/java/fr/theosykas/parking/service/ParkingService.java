package fr.theosykas.parking.service;
import java.util.List;
import java.util.Comparator;
import org.springframework.stereotype.Service;
import fr.theosykas.parking.dto.ParkingDto;
import fr.theosykas.parking.provider.ParkingProviderRegistry;
import fr.theosykas.parking.utils.CoordUtils;
import lombok.RequiredArgsConstructor;

// Recuperer les providers des villes correspondante et recuperer les parking avec retrieveParkings()
@Service
@RequiredArgsConstructor
public class ParkingService {

	private final ParkingProviderRegistry registry;

	public List<ParkingDto> getParking(String city) {
		return registry.getProviderCity(city).retrieveParkings();
	}

	public List<ParkingDto> getNearby(Double lat, Double lon, Double radiusMeters) {
		return registry.getAllProviders().stream()
			.flatMap(provider -> provider.retrieveParkings().stream())
        	.filter(p -> p.getLatitude() != null && p.getLongitude() != null)
        	.peek(p -> p.setDistance_metre((double)
                Math.round(CoordUtils.calculateDistance(lat, lon, p.getLatitude(), p.getLongitude()))))
        	.filter(p -> p.getDistance_metre() <= radiusMeters)
        	.sorted(Comparator.comparingDouble(ParkingDto::getDistance_metre))
        	.toList();
	}
}