package fr.theosykas.parking.service;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import org.springframework.stereotype.Service;
import fr.theosykas.parking.dto.ParkingDto;
import fr.theosykas.parking.exception.ProviderUnavailableException;
import fr.theosykas.parking.provider.ParkingProvider;
import fr.theosykas.parking.provider.ParkingProviderRegistry;
import fr.theosykas.parking.utils.CoordUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class ParkingService {

	private static final Logger log = LoggerFactory.getLogger(ParkingService.class);

	private final ParkingProviderRegistry registry;

	public List<ParkingDto> getParking(String city) {
		return registry.getProviderForCity(city).retrieveParkings();
	}

	public List<ParkingDto> getNearby(Double lat, Double lon, Double radiusMeters) {
		return collectAllParkings().stream()
			.filter(p -> p.getLatitude() != null && p.getLongitude() != null)
        	.map(p -> {
				p.setDistanceMeters((double) Math.round(
					CoordUtils.calculateDistance(lat, lon, p.getLatitude(), p.getLongitude())));
				return p;
			})
        	.filter(p -> p.getDistanceMeters() <= radiusMeters)
        	.sorted(Comparator.comparingDouble(ParkingDto::getDistanceMeters))
        	.toList();
	}

	private List<ParkingDto> collectAllParkings() {
		List<ParkingDto> parkings = new ArrayList<>();
		for (ParkingProvider provider: registry.getAllProviders()) {
			try {
				parkings.addAll(provider.retrieveParkings());
			}
			catch (ProviderUnavailableException e) {
				log.warn("Provider {} indisponible, ignoré pour cette requête", provider.getCity(), e);
			}
		}
		return parkings;
	}
}
