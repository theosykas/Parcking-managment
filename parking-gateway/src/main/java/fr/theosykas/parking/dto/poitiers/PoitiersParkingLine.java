package fr.theosykas.parking.dto.poitiers;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import fr.theosykas.parking.dto.ParkingSourceLine;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PoitiersParkingLine implements ParkingSourceLine {

	@JsonProperty("Nom")
	private String parkingName;

	@JsonProperty("Places")
	private Integer emptySpaces;
	
	@JsonProperty("Capacite")
	private Integer totalSpaces;

	@JsonProperty("_geopoint")
	private String geoPoint;
}
