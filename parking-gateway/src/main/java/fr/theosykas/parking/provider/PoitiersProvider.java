package fr.theosykas.parking.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fr.theosykas.parking.dto.ParkingDto;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

// doit contenir le contrat interface /dto/Response
@Component // java va cree et instencier l'objet
public class PoitiersProvider implements ParkingProvider{
	// value recupere le prov souhaiter de la ville corespondante (/ressources/template/application.yaml)
	@Value("${parking.provider.potiers.url}")
	private String providerUrlApi;

	private final HttpClient client;

	// in instencie client une seule fois
	public PoitiersProvider() {
		this.client = HttpClient.newHttpClient();
	}

	@Override
	public List<ParkingDto> RetrivalParkingData() {
		HttpRequest request = HttpRequest.newBuilder()
						.uri(URI.create(providerUrlApi))
						.GET()
						.build()
					);
	}

}

// La règle stricte de Java : Dans une classe Java, l'espace global (le corps de la classe) ne sert qu'à déclarer des variables (l'état). Toute l'action, la logique, les calculs ou la création d'objets complexes (comme ta requête) doivent obligatoirement se passer à l'intérieur d'une méthode (le comportement).
