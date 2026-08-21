package fr.theosykas.parking.dto;

import lombok.Data;

@Data  // == Getter/setter
public class ParkingDto {
	private String nameOfParking;
	private Integer emptySpace;
	private Integer totalSpace;
	private Integer Occupied;
}

// ajouter un id pour la version de la api refresh
// lastUpdated
// ville
// logitude et lat en double pour Haversine position
