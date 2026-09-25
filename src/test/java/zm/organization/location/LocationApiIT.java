package zm.organization.location;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import zm.organization.AbstractPostgresIT;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class LocationApiIT extends AbstractPostgresIT {

    private static final AtomicInteger TAX_ID_SEQUENCE = new AtomicInteger(1);

    private String givenOrganization() {
        String taxId = String.format("40900%08d", TAX_ID_SEQUENCE.getAndIncrement());
        return post("/internal/organizations", """
                { "name": "Локациски тест", "taxId": "%s", "createdByApp": "menu-app" }
                """.formatted(taxId), JsonNode.class).getBody().get("id").asText();
    }

    private String givenLocation(String orgId) {
        return post("/internal/organizations/" + orgId + "/locations", """
                {
                  "name": "Центар",
                  "address": "Македонија 12",
                  "city": "Скопје",
                  "country": "MK",
                  "contactPhone": "+38970111222",
                  "workingHours": { "mon-fri": "08:00-23:00", "sat-sun": "09:00-01:00" }
                }
                """, JsonNode.class).getBody().get("id").asText();
    }

    @Test
    @DisplayName("PATCH on a location is partial and preserves the jsonb opening hours")
    void patchIsPartial() {
        String locationId = givenLocation(givenOrganization());

        ResponseEntity<JsonNode> patched = patch("/internal/locations/" + locationId, """
                { "address": "Партизанска 5" }
                """, JsonNode.class);

        assertThat(patched.getStatusCode().value()).isEqualTo(200);
        assertThat(patched.getBody().get("address").asText()).isEqualTo("Партизанска 5");
        assertThat(patched.getBody().get("name").asText()).isEqualTo("Центар");
        assertThat(patched.getBody().get("city").asText()).isEqualTo("Скопје");
        assertThat(patched.getBody().get("workingHours").get("sat-sun").asText())
                .isEqualTo("09:00-01:00");
    }

    @Test
    @DisplayName("A location is removed outright, unlike an organization")
    void deleteRemovesTheLocation() {
        String orgId = givenOrganization();
        String locationId = givenLocation(orgId);

        ResponseEntity<Void> deleted = delete("/internal/locations/" + locationId, Void.class);
        assertThat(deleted.getStatusCode().value()).isEqualTo(204);

        ResponseEntity<JsonNode> remaining =
                get("/internal/organizations/" + orgId + "/locations", JsonNode.class);
        assertThat(remaining.getBody()).isEmpty();
    }

    @Test
    @DisplayName("Deleting a location twice is a 404 the second time")
    void deletingTwiceIs404() {
        String locationId = givenLocation(givenOrganization());
        delete("/internal/locations/" + locationId, Void.class);

        ResponseEntity<JsonNode> second = delete("/internal/locations/" + locationId, JsonNode.class);

        assertThat(second.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @DisplayName("Listing locations of an organization that does not exist is a 404")
    void listingForMissingOrganizationIs404() {
        ResponseEntity<JsonNode> response = get(
                "/internal/organizations/00000000-0000-0000-0000-000000000000/locations",
                JsonNode.class);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @DisplayName("Out-of-range coordinates and a malformed country code are rejected")
    void coordinateAndCountryValidation() {
        String orgId = givenOrganization();

        ResponseEntity<JsonNode> response = post(
                "/internal/organizations/" + orgId + "/locations", """
                        { "name": "Невалидна", "country": "MKD", "geoLat": 120.0, "geoLng": -500.0 }
                        """, JsonNode.class);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        JsonNode fieldErrors = response.getBody().get("fieldErrors");
        assertThat(fieldErrors.has("country")).isTrue();
        assertThat(fieldErrors.has("geoLat")).isTrue();
        assertThat(fieldErrors.has("geoLng")).isTrue();
    }
}
