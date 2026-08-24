package fr.theosykas.parking.dto.poitiers;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

// entry point de notre json api (envellope)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PoitiersResponse {
	@JsonProperty("results")
	private List<PoitiersParkingLine> results;
}
