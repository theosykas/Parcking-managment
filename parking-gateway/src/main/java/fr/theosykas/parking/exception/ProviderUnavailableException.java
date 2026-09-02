package fr.theosykas.parking.exception;

public class ProviderUnavailableException extends RuntimeException{
	public ProviderUnavailableException(String msg, Throwable cause) {
		super(msg, cause);
	}
}