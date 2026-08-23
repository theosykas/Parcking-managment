package fr.theosykas.parking.dto;
import lombok.Data;

// Format public de l'API : la seule chose que voit l'app mobile.
// Le modifier casse le contrat, c'est justement ce que le sujet interdit
@Data
public class ParkingDto {
	private String Name;
	private Integer avaiable_space;
	private Integer capacity;
	private Integer occupied;
	private Double longitude;
	private Double latitude;
	private Double distance_metre;
}
