package fr.theosykas.parking.dto;

public interface ParkingSourceLine {
	String getParkingName();
	Integer getEmptySpaces();
	Integer getTotalSpaces();
	String getGeoPoint();
}
