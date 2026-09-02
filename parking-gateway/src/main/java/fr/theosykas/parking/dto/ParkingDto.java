package fr.theosykas.parking.dto;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
public class ParkingDto {
	private String name;
	private Integer available;
	private Integer capacity;
	private Integer occupied;
	private Double longitude;
	private Double latitude;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Double distanceMeters;
}
