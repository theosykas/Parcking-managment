package fr.theosykas.parking.provider;
import java.util.List;
import fr.theosykas.parking.dto.ParkingDto;


// contrat commun via interface seul point pivot
// entre pour que le format exposé soit le bon formt API
public interface ParkingProvider {
	String getCity();
	List<ParkingDto> retrieveParkings();
}
