package fr.theosykas.parking.dto;

import lombok.Data;

@Data  // == Getter/setter
public class ParkingDto {
	private String name_of_parking;
	private String emptySpace;
	private String totalSpace;
}
