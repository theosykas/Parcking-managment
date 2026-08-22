package fr.theosykas.parking.dto;
import lombok.Data;

@Data
public class ParkingDto {
	private String Name;
	private Integer avaiable_space;
	private Integer capacity;
	private Integer occupied;
	private Double longitude;
	private Double latitude;
	private Double distance_metre;
}
