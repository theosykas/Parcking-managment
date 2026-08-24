package fr.theosykas.parking.dto.cannes;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CannesResponse {
	@JsonProperty("results")
	private List<CannesParkingLine> results;
}
