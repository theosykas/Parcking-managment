package fr.theosykas.parking.provider.poitiers;

import org.springframework.stereotype.Component;
import fr.theosykas.parking.dto.ParkingDto;
import fr.theosykas.parking.dto.poitiers.PoitierParkingLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class PoitiersMapper {

	private static final Logger log = LoggerFactory.getLogger(PoitiersProvider.class);

	public ParkingDto mapper(PoitierParkingLine line) {
		ParkingDto parking = new ParkingDto();
					parking.setName(line.getNameOfParking());
					parking.setAvaiable_space(line.getEmptySpace());
					parking.setCapacity(line.getTotalSpace());
		if (line.getEmptySpace() != null && line.getTotalSpace() != null) {
				parking.setOccupied(line.getTotalSpace() - line.getEmptySpace());
		}
		coordinateRetrive(parking, line);
		return parking;
	}

	public void coordinateRetrive(ParkingDto parking, PoitierParkingLine line) {
		if (line.getGeoPoint() != null) {  // renvoie un "null" au lieu d'une exception et crash on laisse el qeul
			String[] formatPoint = line.getGeoPoint().split(",");
				if (formatPoint.length == 2) {
					try {
						parking.setLatitude(Double.parseDouble(formatPoint[0].trim()));  // reupere les potions pour les return a la list
						parking.setLongitude(Double.parseDouble(formatPoint[1].trim()));
					}
					catch (NumberFormatException e){
						log.warn("Coordonnées illisibles pour le parking {} : {}",
									line.getNameOfParking(), line.getGeoPoint()
						);
					}
				}
		}
	}
}