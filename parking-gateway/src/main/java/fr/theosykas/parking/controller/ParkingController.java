package fr.theosykas.parking.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import fr.theosykas.parking.service.ParkingService;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import fr.theosykas.parking.dto.ParkingDto;
import fr.theosykas.parking.dto.request.NearbyRequest;

@Validated
@RestController
@RequestMapping("/api/parking")
@RequiredArgsConstructor
public class ParkingController {

	private final ParkingService service;

	@GetMapping
	public List<ParkingDto> getParking(@RequestParam @NotBlank String city) {
		return service.getParking(city);
	}

	@GetMapping("/proximity")
	public List<ParkingDto> getProximity(
								@Valid NearbyRequest requets
		) {
		return service.getNearby(requets.getLat(), requets.getLon(), requets.getRadiusMetre());
	}
}
