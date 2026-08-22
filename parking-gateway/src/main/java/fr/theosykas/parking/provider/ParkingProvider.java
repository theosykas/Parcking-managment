package fr.theosykas.parking.provider;
import java.util.List;
import fr.theosykas.parking.dto.ParkingDto;

// contrat generique pour pouvoir recuperer les data de n'importe quelle parking en appelant RetrivalParkingData()
public interface ParkingProvider {
	String getCity();  // methode abstract * 2
	List<ParkingDto> retrieveParkings();
}
