package fr.theosykas.parking.provider;
import java.util.List;
import fr.theosykas.parking.dto.ParkingDto;

public interface ParkingProvider {
	String getCity();
	List<ParkingDto> retrieveParkings();
}
