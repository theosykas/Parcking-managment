package fr.theosykas.parking.exception;

// je gere mes propres exception meme si IOexcept existe pour ce cas pour que le user ne vois pas de log incomphrensible ou de crash de notre application
public class ProviderInterrupted extends RuntimeException{
	public ProviderInterrupted(String msg, Throwable cause) {
		super(msg, cause);
	}
}