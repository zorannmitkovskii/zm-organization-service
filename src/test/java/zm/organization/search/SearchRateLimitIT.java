package zm.organization.search;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import zm.organization.AbstractPostgresIT;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kept apart from {@link SearchApiIT} because a rate limit and a suite of
 * functional searches cannot share a context: either the limit is high enough
 * to be untestable, or the functional tests start tripping it.
 */
@TestPropertySource(properties = {
        "organization.search.rate-limit.max-per-window=5",
        "organization.search.rate-limit.window-seconds=60"
})
class SearchRateLimitIT extends AbstractPostgresIT {

    @Test
    @DisplayName("A caller past the window limit gets 429 with a stable error code")
    void burstIsRateLimited() {
        int limit = 5;
        for (int i = 0; i < limit; i++) {
            assertThat(get("/internal/organizations/search?taxId=4080011111111&realm=menu-app",
                    JsonNode.class).getStatusCode().value())
                    .as("call %d should be within the limit", i + 1)
                    .isEqualTo(200);
        }

        ResponseEntity<JsonNode> refused = get(
                "/internal/organizations/search?taxId=4080011111111&realm=menu-app", JsonNode.class);

        assertThat(refused.getStatusCode().value()).isEqualTo(429);
        assertThat(refused.getBody().get("error").asText()).isEqualTo("RATE_LIMITED");
    }
}
