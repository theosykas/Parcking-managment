package fr.theosykas.parking.dto.request;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class NearbyRequest {
	@NotNull
	@Min(-90)  @Max(90)
	private Double lat;
	
	@NotNull
	@Min(-180) @Max(180)
	private Double lon;
	
	@NotNull
	@Positive
	private Double radiusMeters;
}