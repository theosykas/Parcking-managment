package fr.theosykas.parking.provider;

import org.springframework.stereotype.Component;
import fr.theosykas.parking.dto.ParkingDto;
import fr.theosykas.parking.dto.ParkingSourceLine;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ParkingMapper {
	private static final Logger log = LoggerFactory.getLogger(ParkingMapper.class);

	public record Coordinates(double latitude, double longitude) {}

	public ParkingDto toParkingDto(ParkingSourceLine line) {
		ParkingDto parking = new ParkingDto();
			parking.setName(line.getParkingName());
			parking.setAvailable(line.getEmptySpaces());
			parking.setCapacity(line.getTotalSpaces());
		if (line.getEmptySpaces() != null && line.getTotalSpaces() != null) {
				parking.setOccupied(line.getTotalSpaces() - line.getEmptySpaces());
		}
		applyCoordinates(parking, line);
		return parking;
	}

	private void applyCoordinates(ParkingDto parking, ParkingSourceLine line) {
		parseGeoPoint(line.getGeoPoint()).ifPresentOrElse(
				coord -> {
				parking.setLatitude(coord.latitude());
				parking.setLongitude(coord.longitude());
			},
			() -> log.warn("Coordonnées absentes ou illisibles pour le parking {} : {}",
			line.getParkingName(), line.getGeoPoint())
			);
		}
	
		Optional<Coordinates> parseGeoPoint(String geoPoint) {
			if (geoPoint == null) {
				return Optional.empty();
			}
			String[] part = geoPoint.split(",");
			if (part.length != 2) {
				return Optional.empty();
			}
			try {
				return Optional.of(new Coordinates(
					Double.parseDouble(part[0].trim()),
					Double.parseDouble(part[1].trim())));
			}
			catch (NumberFormatException e) {
				return Optional.empty();
		}
	}
}