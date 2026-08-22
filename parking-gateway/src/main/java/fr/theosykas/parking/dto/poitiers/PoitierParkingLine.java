package fr.theosykas.parking.dto.poitiers;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


// permet de lire les valeur du json pour les ecrire dans le ParkingDto
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PoitierParkingLine {

	@JsonProperty("Nom")
	private String NameOfParking;

	@JsonProperty("Places")
	private Integer emptySpace;
	
	@JsonProperty("Capacite")
	private Integer totalSpace;

	@JsonProperty("_geopoint")
	private String geoPoint; // "46.5793235337795, 0.3385507838016221"
}
