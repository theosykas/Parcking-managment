package fr.theosykas.parking.dto;

// peu importe comment la ville data interne s'organise je veux getNameOfParking ..
public interface ParkingSourceLine {
	String getNameOfParking();
	Integer getEmptySpace();
	Integer getTotalSpace();
	String getGeoPoint();
}
