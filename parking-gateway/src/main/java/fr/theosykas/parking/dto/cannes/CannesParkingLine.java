package fr.theosykas.parking.dto.cannes;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import fr.theosykas.parking.dto.ParkingSourceLine;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CannesParkingLine implements ParkingSourceLine {

	@JsonProperty("name_parking")
	private String parkingName;

	@JsonProperty("Places_disponible")
	private Integer emptySpaces;
	
	@JsonProperty("Capacite_max")
	private Integer totalSpaces;

	@JsonProperty("_geopoint_coord")
	private String geoPoint;
}
