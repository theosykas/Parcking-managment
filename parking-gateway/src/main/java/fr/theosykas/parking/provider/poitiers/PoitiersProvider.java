package fr.theosykas.parking.provider.poitiers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import fr.theosykas.parking.dto.poitiers.PoitiersParkingLine;
import fr.theosykas.parking.dto.poitiers.PoitiersResponse;
import java.net.http.HttpClient;
import fr.theosykas.parking.provider.ParkingMapper;
import fr.theosykas.parking.provider.AbstractHttpProvider;
import java.util.List;

@Component
public class PoitiersProvider extends AbstractHttpProvider<PoitiersResponse> {

	public PoitiersProvider(
		HttpClient client,
		ObjectMapper mapper,
		ParkingMapper parkingMapper,
		@Value("${parking.provider.poitiers.url}") String providerUrlApi
	) {
		super(client, mapper, parkingMapper, providerUrlApi);
	}

	@Override
	public String getCity() {
		return "Poitiers";
	}

	@Override
	protected Class<PoitiersResponse> responseType() {
		return PoitiersResponse.class;
	}

	@Override
	protected List<PoitiersParkingLine> extractLines(PoitiersResponse response) {
		return response.getResults();
	}
}
