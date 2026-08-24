package fr.theosykas.parking.provider.cannes;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper; 
import fr.theosykas.parking.dto.cannes.CannesParkingLine;
import fr.theosykas.parking.dto.cannes.CannesResponse;
import java.util.List;
import fr.theosykas.parking.provider.AbstractHttpProvider;
import fr.theosykas.parking.provider.ParkingMapper;
import java.net.http.HttpClient;

@Component
public class CannesProvider extends AbstractHttpProvider<CannesResponse> {

	public CannesProvider(
		HttpClient client,
		ObjectMapper mapper,
		ParkingMapper parkingMapper,
		@Value("${parking.provider.cannes.url}") String providerUrlApi
	) {
		super(client, mapper, parkingMapper, providerUrlApi);
	}

	@Override
	public String getCity() {
		return "Cannes";
	}

	@Override
	protected Class<CannesResponse> responseType() {
		return CannesResponse.class;
	}

	@Override
	protected List<CannesParkingLine> extractLines(CannesResponse response) {
		return response.getResults();
	}
}
