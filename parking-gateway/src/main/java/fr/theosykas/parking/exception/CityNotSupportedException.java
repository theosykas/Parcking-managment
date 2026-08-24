package fr.theosykas.parking.exception;

public class CityNotSupportedException extends RuntimeException {
	public CityNotSupportedException(String msg) {
		super(msg);
	}
}
