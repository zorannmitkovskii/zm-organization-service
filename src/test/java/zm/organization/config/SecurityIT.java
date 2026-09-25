package zm.organization.config;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import zm.organization.AbstractPostgresIT;
import zm.organization.TestJwtDecoderConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ORG-04 AC1 and AC2, and AC5 for the endpoints that must stay open.
 *
 * <p>{@code @AutoConfigureMetrics} is needed for the metrics assertion:
 * {@code @SpringBootTest} disables metrics export, which removes
 * {@code /actuator/prometheus} from the test context entirely — it would 404
 * for reasons that have nothing to do with security.
 */
@AutoConfigureMetrics
class SecurityIT extends AbstractPostgresIT {

    private static final String SOME_ORG = "/internal/organizations/00000000-0000-0000-0000-000000000000";

    @Test
    @DisplayName("AC1 — no token at all is a 401")
    void anonymousIsUnauthorized() {
        ResponseEntity<JsonNode> response =
                exchange(HttpMethod.GET, SOME_ORG, null, null, JsonNode.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @DisplayName("AC1 — an unverifiable token is a 401, not a 403")
    void garbageTokenIsUnauthorized() {
        ResponseEntity<JsonNode> response =
                exchange(HttpMethod.GET, SOME_ORG, null, "not-a-real-token", JsonNode.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @DisplayName("AC1 — a valid zm-services token without org-client is a 403")
    void tokenWithoutOrgClientRoleIsForbidden() {
        ResponseEntity<JsonNode> response = exchange(
                HttpMethod.GET, SOME_ORG, null, TestJwtDecoderConfig.NO_ROLE_TOKEN, JsonNode.class);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    @DisplayName("AC2 — a token carrying org-client reaches the endpoint")
    void tokenWithOrgClientRoleIsAccepted() {
        ResponseEntity<JsonNode> response = exchange(
                HttpMethod.GET, SOME_ORG, null, TestJwtDecoderConfig.ORG_CLIENT_TOKEN, JsonNode.class);

        // 404 rather than 200: the organization does not exist. Reaching the
        // handler at all is the point — authorisation passed.
        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @DisplayName("AC1 — the hottest endpoint is protected like every other")
    void membersCheckRequiresTheRole() {
        String path = "/internal/members/check"
                + "?orgId=00000000-0000-0000-0000-000000000000&realm=menu-app&userId=someone";

        assertThat(exchange(HttpMethod.GET, path, null, null, JsonNode.class)
                .getStatusCode().value()).isEqualTo(401);
        assertThat(exchange(HttpMethod.GET, path, null, TestJwtDecoderConfig.NO_ROLE_TOKEN, JsonNode.class)
                .getStatusCode().value()).isEqualTo(403);
    }

    @Test
    @DisplayName("AC5 — health and metrics stay open for the healthcheck and the scraper")
    void operationalEndpointsStayOpen() {
        for (String path : new String[]{"/actuator/health", "/actuator/health/liveness", "/actuator/prometheus"}) {
            ResponseEntity<String> response =
                    exchange(HttpMethod.GET, path, null, null, String.class);

            assertThat(response.getStatusCode().value())
                    .as("%s must not require a token", path)
                    .isEqualTo(200);
        }
    }

    @Test
    @DisplayName("Anything outside /internal and /actuator is denied outright")
    void unknownPathsAreDenied() {
        ResponseEntity<JsonNode> response = exchange(
                HttpMethod.GET, "/some/other/path", null,
                TestJwtDecoderConfig.ORG_CLIENT_TOKEN, JsonNode.class);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }
}
