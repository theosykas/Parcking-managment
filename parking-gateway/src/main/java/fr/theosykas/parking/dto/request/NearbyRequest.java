package fr.theosykas.parking.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

// applique des version sur les parametres entres par le user directement
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
	private Double radiusMetre;
}