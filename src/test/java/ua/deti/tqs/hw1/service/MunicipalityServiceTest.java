package ua.deti.tqs.hw1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class MunicipalityServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private MunicipalityService service;

    private static final String API_URL = "https://json.geoapi.pt/municipios";
    private static final ParameterizedTypeReference<List<String>> TYPE_REF =
        new ParameterizedTypeReference<List<String>>() {};

    @BeforeEach
    void setUp() {
        service = new MunicipalityService();
        // Use reflection to inject the mocked RestTemplate
        try {
            var field = MunicipalityService.class.getDeclaredField(
                "restTemplate"
            );
            field.setAccessible(true);
            field.set(service, restTemplate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock", e);
        }
    }

    // test successfull api call
    @Test
    void whenApiWorks_thenReturnMunicipalities() {
        // Arrange
        List<String> mockMunicipalities = List.of("Aveiro", "Porto", "Lisboa");
        ResponseEntity<List<String>> mockResponse = ResponseEntity.ok(
            mockMunicipalities
        );

        when(
            restTemplate.exchange(
                eq(API_URL),
                eq(HttpMethod.GET),
                isNull(),
                eq(TYPE_REF)
            )
        ).thenReturn(mockResponse);

        // Act
        List<String> result = service.getMunicipalities();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).containsExactly("Aveiro", "Porto", "Lisboa");
    }

    // test fallback
    @Test
    void whenApiFails_thenReturnFallbackList() {
        // Arrange
        when(
            restTemplate.exchange(
                eq(API_URL),
                eq(HttpMethod.GET),
                isNull(),
                eq(TYPE_REF)
            )
        ).thenThrow(new RestClientException("API is down"));

        // Act
        List<String> result = service.getMunicipalities();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).containsExactly("Aveiro", "Lisboa", "Porto");
    }

    // test empty response handling
    @Test
    void whenApiReturnsEmpty_thenReturnFallbackList() {
        // Arrange
        ResponseEntity<List<String>> mockResponse = ResponseEntity.ok(
            List.of()
        );

        when(
            restTemplate.exchange(
                eq(API_URL),
                eq(HttpMethod.GET),
                isNull(),
                eq(TYPE_REF)
            )
        ).thenReturn(mockResponse);

        // Act
        List<String> result = service.getMunicipalities();

        // Assert
        assertThat(result).containsExactly("Aveiro", "Lisboa", "Porto");
    }
}
