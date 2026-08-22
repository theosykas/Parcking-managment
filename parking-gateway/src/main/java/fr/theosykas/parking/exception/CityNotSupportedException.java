package fr.theosykas.parking.exception;

//seulement cle manquante juste un message
public class CityNotSupportedException extends RuntimeException {
	public CityNotSupportedException(String msg) {
		super(msg);
	}
}
