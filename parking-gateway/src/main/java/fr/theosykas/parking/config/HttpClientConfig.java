package fr.theosykas.parking.config;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientConfig {
	@Bean
	HttpClient httpClient() {
		return HttpClient.newBuilder()
						.connectTimeout(Duration.ofSeconds(3))
						.build();
	}
}
