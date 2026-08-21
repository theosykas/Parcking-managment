package fr.theosykas.parking.dto;

import lombok.Data;

@Data  // == Getter/setter
public class ParkingDto {
	private String nameOfParking;
	private Integer emptySpace;
	private Integer totalSpace;
	private Integer Occupied;
}
