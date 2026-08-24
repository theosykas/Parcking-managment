package fr.theosykas.parking.dto;

public interface ParkingSourceLine {
	String getNameOfParking();
	Integer getEmptySpace();
	Integer getTotalSpace();
	String getGeoPoint();
}
