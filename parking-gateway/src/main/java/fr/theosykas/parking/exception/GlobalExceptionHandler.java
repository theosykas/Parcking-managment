package fr.theosykas.parking.exception;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice  // format JSON/XML
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ProviderUnavailableException.class)
	public ResponseEntity<Map<String, String>> invalidProvider(ProviderUnavailableException e) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
	}

	@ExceptionHandler(CityNotSupportedException.class)
	public ResponseEntity<Map<String, String>> invalidCity(CityNotSupportedException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
	}

	// Valid Not blank city=abc conversion echec / BAD_REQUEST
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<Map<String, String>> handleBadParam(MethodArgumentTypeMismatchException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Parametres type invalid"));
	}

	// chaine vide parametre @NotBlank 
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<Map<String, String>> handleConstraint(ConstraintViolationException e) {
		String champs = e.getConstraintViolations().stream()
			.map(v -> v.getPropertyPath() + " : " + v.getMessage())
			.collect(Collectors.joining(", "));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(Map.of("error", champs));
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<Map<String, String>> handleMissingParam(MissingServletRequestParameterException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(Map.of("error", "Parametre obligatoire manquant : " + e.getParameterName()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
    		String champs = e.getBindingResult().getFieldErrors().stream()
            	.map(err -> err.getField() + " : " + err.getDefaultMessage())
        	    .collect(Collectors.joining(", "));
    	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", champs));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> globalException(Exception e) {
		log.error("Erreur inattendue", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Une erreur interne est survenue"));
	}

}