package fr.theosykas.parking.dto;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

// Format public de l'API : la seule chose que voit l'app mobile.
// Le modifier casse le contrat, c'est justement ce que le sujet interdit
@Data
public class ParkingDto {
	private String name;
	private Integer available;
	private Integer capacity;
	private Integer occupied;
	private Double longitude;
	private Double latitude;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Double distanceMetre;
}
