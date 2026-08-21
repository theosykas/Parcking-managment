package fr.theosykas.parking.exception;

import java.security.ProviderException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice  // format JSON/XML
public class GlobalHandlerException {
	@ExceptionHandler(ProviderException.class)
	public ResponseEntity<Map<String, String>> InvalidProvider(ProviderInterrupted e) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
	}
}