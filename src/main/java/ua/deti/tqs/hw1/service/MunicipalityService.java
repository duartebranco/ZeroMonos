package ua.deti.tqs.hw1.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MunicipalityService {

    private static final Logger logger = LoggerFactory.getLogger(
        MunicipalityService.class
    );
    private static final String API_URL = "https://json.geoapi.pt/municipios";
    private final RestTemplate restTemplate = new RestTemplate();
    private List<String> cache;

    public List<String> getMunicipalities() {
        if (cache != null && !cache.isEmpty()) return cache;

        try {
            ResponseEntity<List<String>> response = restTemplate.exchange(
                API_URL,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<String>>() {}
            );
            List<String> responseBody = response.getBody();

            // Handle null or empty response body
            if (responseBody == null || responseBody.isEmpty()) {
                logger.warn("API returned null or empty response");
                return List.of("Aveiro", "Lisboa", "Porto");
            }

            cache = responseBody;
            return cache;
        } catch (Exception e) {
            logger.error("Failed to load municipalities: " + e.getMessage(), e);
            return List.of("Aveiro", "Lisboa", "Porto");
        }
    }
}
