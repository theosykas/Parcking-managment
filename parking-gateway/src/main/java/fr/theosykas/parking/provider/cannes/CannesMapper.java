package fr.theosykas.parking.provider.cannes;

import fr.theosykas.parking.dto.ParkingDto;
import fr.theosykas.parking.dto.cannes.CannesParkingLine;
import fr.theosykas.parking.provider.poitiers.PoitiersProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CannesMapper {
	private static final Logger log = LoggerFactory.getLogger(PoitiersProvider.class);

	public ParkingDto mapper(CannesParkingLine line) {
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

	public void coordinateRetrive(ParkingDto parking, CannesParkingLine line) {
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