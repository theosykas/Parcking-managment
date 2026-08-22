package fr.theosykas.parking.dto.cannes;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CannesParkingLine {

	@JsonProperty("name_parking")
	private String NameOfParking;

	@JsonProperty("Places_disponible")
	private Integer emptySpace;
	
	@JsonProperty("Capacite_max")
	private Integer totalSpace;

	@JsonProperty("_geopoint_coord")
	private String geoPoint;
}