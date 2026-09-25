package zm.organization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ORG-01: the endpoints the Docker HEALTHCHECK, the deploy workflow and the
 * metrics scraper depend on must be reachable without authentication.
 *
 * <p>{@code @AutoConfigureMetrics} is required because {@code @SpringBootTest}
 * sets {@code management.defaults.metrics.export.enabled=false} by default, which
 * removes the Prometheus registry — and with it {@code /actuator/prometheus} —
 * from the test context only. Production configuration is unaffected.
 */
@AutoConfigureMetrics
@AutoConfigureTestRestTemplate
class ActuatorHealthIT extends AbstractPostgresIT {

    @Autowired
    private TestRestTemplate rest;

    @Test
    @DisplayName("GET /actuator/health returns UP")
    void healthIsUp() {
        ResponseEntity<String> response = rest.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    @DisplayName("GET /actuator/health/liveness returns UP for the container healthcheck")
    void livenessProbeIsUp() {
        ResponseEntity<String> response = rest.getForEntity("/actuator/health/liveness", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("Prometheus metrics are exposed and tagged with the application name")
    void prometheusEndpointExposed() {
        ResponseEntity<String> response = rest.getForEntity("/actuator/prometheus", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("application=\"zm-organization-service\"");
    }
}
