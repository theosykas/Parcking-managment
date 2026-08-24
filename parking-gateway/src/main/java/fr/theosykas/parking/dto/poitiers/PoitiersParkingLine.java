package fr.theosykas.parking.dto.poitiers;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import fr.theosykas.parking.dto.ParkingSourceLine;
import lombok.Data;

// La source publie plus de champs que ceux qui m'interessent : je n'en declare
// que 4. Un champ ajoute par la ville ne cassera pas la deserialisation.
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PoitiersParkingLine implements ParkingSourceLine{

	@JsonProperty("Nom")
	private String nameOfParking;

	@JsonProperty("Places")
	private Integer emptySpace;
	
	@JsonProperty("Capacite")
	private Integer totalSpace;

	@JsonProperty("_geopoint")
	private String geoPoint;
}
