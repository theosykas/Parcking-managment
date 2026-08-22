package fr.theosykas.parking.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class NearbyRequest {
	@Min(-90)  @Max(90)
	private Double lat;
	
	@Min(-180) @Max(180)
	private Double lon;
	
	@Positive
	private Double radiusMetre;
}