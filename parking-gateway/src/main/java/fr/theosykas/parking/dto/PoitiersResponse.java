package fr.theosykas.parking.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PoitiersResponse {

	@JsonProperty("results")
	private List<PoitiersResponse> results;

	@JsonProperty("Nom")
	private String NameOfParking;

	@JsonProperty("Places")
	private Integer emptySpace;
	
	@JsonProperty("Capacite")
	private Integer totalSpace;

	public PoitiersResponse() {}

	public PoitiersResponse(
		String NameOfParking,
		Integer emptySpace,
		Integer totalSpace
	) {
		this.NameOfParking = NameOfParking;
		this.emptySpace = emptySpace;
		this.totalSpace = totalSpace;
	}
}
