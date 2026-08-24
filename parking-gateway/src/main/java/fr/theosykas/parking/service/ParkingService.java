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

// Recuperer les providers des villes correspondante et recuperer les parking avec retrieveParkings()
@Service
@RequiredArgsConstructor
public class ParkingService {

	private static final Logger log = LoggerFactory.getLogger(ParkingProvider.class);

	private final ParkingProviderRegistry registry;

	public List<ParkingDto> getParking(String city) {
		return registry.getProviderCity(city).retrieveParkings();
	}

	// si Cannes crash poitiers doit quand meme repondre on interroge plusieur villes (collectAllParkings())
	public List<ParkingDto> getNearby(Double lat, Double lon, Double radiusMeters) {
		return collectAllParkings().stream()
			.filter(p -> p.getLatitude() != null && p.getLongitude() != null)
        	.map(p -> {
				p.setDistanceMetre((double) Math.round(
					CoordUtils.calculateDistance(lat, lon, p.getLatitude(), p.getLongitude())));
				return p;
			})
        	.filter(p -> p.getDistanceMetre() <= radiusMeters)
        	.sorted(Comparator.comparingDouble(ParkingDto::getDistanceMetre))
        	.toList();
	}

	// je recupere les parking pour catch en cas d'api morte et continuer
	private List<ParkingDto> collectAllParkings() {
		List<ParkingDto> parking = new ArrayList<>();
		for (ParkingProvider provider: registry.getAllProviders()) {
			try {
				parking.addAll(provider.retrieveParkings());
			}
			catch (ProviderUnavailableException e) {
				// si une vile repond pas degrdation partielle mais on boucle toujours
				log.warn("Provider {} indisponible, ignore pour cette requete", provider.getCity(), e);
			}
		}
		return parking;
	}
}
