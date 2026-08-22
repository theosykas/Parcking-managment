package fr.theosykas.parking.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import fr.theosykas.parking.service.ParkingService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import fr.theosykas.parking.dto.ParkingDto;

@RestController
@RequestMapping("/api/parking")
public class ParkingController {

	private final ParkingService service;

	public ParkingController(ParkingService service) {
		this.service = service;
	}

	@GetMapping
	public List<ParkingDto> getParking(@RequestParam String city) {
		return service.getParking(city);
	}
	
	// @GetMapping("/proximity")
	// public List<ParkingDto> getProximity(
	// 							@RequestParam Double lat,
	// 							@RequestParam Double lon,
	// 							@RequestParam Double radiusMetre) {
	// 	return service.getNearby(lon, lat, radiusMetre);
	// }
}
