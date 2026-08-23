package fr.theosykas.parking.exception;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import lombok.RequiredArgsConstructor;

@RestControllerAdvice  // format JSON/XML
@RequiredArgsConstructor
public class GlobalExceptionHandler {

	@ExceptionHandler(ProviderInterrupted.class)
	public ResponseEntity<Map<String, String>> invalidProvider(ProviderInterrupted e) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("Error ", e.getMessage()));
	}

	@ExceptionHandler(CityNotSupportedException.class)
	public ResponseEntity<Map<String, String>> invalidCity(CityNotSupportedException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("Error ", e.getMessage()));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<Map<String, String>> handleBadParam(MethodArgumentTypeMismatchException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("Error ", e.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
    		String champs = e.getBindingResult().getFieldErrors().stream()
            	.map(err -> err.getField() + " : " + err.getDefaultMessage())
        	    .collect(Collectors.joining(", "));
    	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("Error ", champs));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> globalException(Exception e) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("Error ", e.getMessage()));
	}

}