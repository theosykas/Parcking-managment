package fr.theosykas.parking.exception;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import fr.theosykas.parking.provider.ParkingProviderRegistry;

@RestControllerAdvice  // format JSON/XML
public class GlobalExceptionHandler {

	private final ParkingProviderRegistry registry;

	public GlobalExceptionHandler(ParkingProviderRegistry registry) {
		this.registry = registry;
	}

	@ExceptionHandler(ProviderInterrupted.class)
	public ResponseEntity<Map<String, String>> InvalidProvider(ProviderInterrupted e) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
	}

	@ExceptionHandler(CityNotSupportedException.class)
	public ResponseEntity<Map<String, Object>> InvalidCity(CityNotSupportedException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
			"error", e.getMessage(), "supported city:", registry.getValidCity())); // e.getSupportedCity()
	}
}