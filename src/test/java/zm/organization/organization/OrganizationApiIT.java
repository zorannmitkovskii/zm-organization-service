package zm.organization.organization;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import zm.organization.AbstractPostgresIT;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizationApiIT extends AbstractPostgresIT {

    private static final AtomicInteger TAX_ID_SEQUENCE = new AtomicInteger(1);

    /** Each test needs its own tax id — the unique index spans the whole table. */
    private static String nextTaxId() {
        return String.format("40800%08d", TAX_ID_SEQUENCE.getAndIncrement());
    }

    private ResponseEntity<JsonNode> createOrganization(String taxId, String name) {
        return post("/internal/organizations", """
                {
                  "name": "%s",
                  "legalName": "%s ДООЕЛ Скопје",
                  "taxId": "%s",
                  "contactEmail": "kontakt@panorama.mk",
                  "contactPhone": "+38970123456",
                  "createdByApp": "menu-app"
                }
                """.formatted(name, name, taxId), JsonNode.class);
    }

    @Test
    @DisplayName("AC1 — full CRUD cycle for an organization and two locations")
    void fullCrudCycle() {
        ResponseEntity<JsonNode> created = createOrganization(nextTaxId(), "Ресторан Панорама");
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        String orgId = created.getBody().get("id").asText();
        assertThat(created.getBody().get("status").asText()).isEqualTo("ACTIVE");
        assertThat(created.getBody().get("defaultLang").asText()).isEqualTo("mk");

        for (String name : List.of("Панорама Центар", "Панорама Дебар Маало")) {
            ResponseEntity<JsonNode> location = post(
                    "/internal/organizations/" + orgId + "/locations", """
                            {
                              "name": "%s",
                              "address": "Македонија 12",
                              "city": "Скопје",
                              "country": "MK",
                              "geoLat": 41.996200,
                              "geoLng": 21.431600,
                              "workingHours": { "mon-fri": "08:00-23:00" }
                            }
                            """.formatted(name), JsonNode.class);

            assertThat(location.getStatusCode().value()).isEqualTo(201);
            assertThat(location.getBody().get("orgId").asText()).isEqualTo(orgId);
            assertThat(location.getBody().get("active").asBoolean()).isTrue();
            assertThat(location.getBody().get("workingHours").get("mon-fri").asText())
                    .isEqualTo("08:00-23:00");
        }

        ResponseEntity<JsonNode> locations =
                get("/internal/organizations/" + orgId + "/locations", JsonNode.class);
        assertThat(locations.getBody()).hasSize(2);
    }

    @Test
    @DisplayName("AC1 — PATCH changes only the fields that were sent")
    void patchIsPartial() {
        String orgId = createOrganization(nextTaxId(), "Кафе Уно").getBody().get("id").asText();

        ResponseEntity<JsonNode> patched = patch("/internal/organizations/" + orgId, """
                { "contactPhone": "+38971999888" }
                """, JsonNode.class);

        assertThat(patched.getStatusCode().value()).isEqualTo(200);
        assertThat(patched.getBody().get("contactPhone").asText()).isEqualTo("+38971999888");
        assertThat(patched.getBody().get("name").asText()).isEqualTo("Кафе Уно");
        assertThat(patched.getBody().get("contactEmail").asText()).isEqualTo("kontakt@panorama.mk");
        assertThat(patched.getBody().get("legalName").asText()).isEqualTo("Кафе Уно ДООЕЛ Скопје");
    }

    @Test
    @DisplayName("AC2 — a second organization with the same tax id is a 409 naming the first")
    void duplicateTaxIdIsRejected() {
        String taxId = nextTaxId();
        String firstId = createOrganization(taxId, "Прва").getBody().get("id").asText();

        ResponseEntity<JsonNode> second = createOrganization(taxId, "Втора");

        assertThat(second.getStatusCode().value()).isEqualTo(409);
        assertThat(second.getBody().get("error").asText()).isEqualTo("TAX_ID_EXISTS");
        assertThat(second.getBody().get("existingOrgId").asText()).isEqualTo(firstId);
    }

    @Test
    @DisplayName("AC3 — DELETE suspends; the record stays readable with its new status")
    void deleteSuspendsInsteadOfRemoving() {
        String orgId = createOrganization(nextTaxId(), "За суспендирање").getBody().get("id").asText();

        ResponseEntity<Void> deleted = delete("/internal/organizations/" + orgId, Void.class);
        assertThat(deleted.getStatusCode().value()).isEqualTo(204);

        ResponseEntity<JsonNode> afterDelete = get("/internal/organizations/" + orgId, JsonNode.class);
        assertThat(afterDelete.getStatusCode().value()).isEqualTo(200);
        assertThat(afterDelete.getBody().get("status").asText()).isEqualTo("SUSPENDED");
    }

    @Test
    @DisplayName("AC4 — an invalid tax id and email come back field by field")
    void validationErrorsAreFieldByField() {
        ResponseEntity<JsonNode> response = post("/internal/organizations", """
                {
                  "name": "Невалидна",
                  "taxId": "12345",
                  "contactEmail": "not-an-email",
                  "createdByApp": "menu-app"
                }
                """, JsonNode.class);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().get("error").asText()).isEqualTo("VALIDATION_FAILED");
        JsonNode fieldErrors = response.getBody().get("fieldErrors");
        assertThat(fieldErrors.has("taxId")).isTrue();
        assertThat(fieldErrors.has("contactEmail")).isTrue();
    }

    @Test
    @DisplayName("AC4 — a missing required field is reported too")
    void missingRequiredFieldIsReported() {
        ResponseEntity<JsonNode> response = post("/internal/organizations", """
                { "taxId": "4080099999999" }
                """, JsonNode.class);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        JsonNode fieldErrors = response.getBody().get("fieldErrors");
        assertThat(fieldErrors.has("name")).isTrue();
        assertThat(fieldErrors.has("createdByApp")).isTrue();
    }

    @Test
    @DisplayName("AC5 — a location cannot be created on an organization that does not exist")
    void locationOnMissingOrganizationIs404() {
        ResponseEntity<JsonNode> response = post(
                "/internal/organizations/00000000-0000-0000-0000-000000000000/locations", """
                        { "name": "Никаде", "city": "Скопје" }
                        """, JsonNode.class);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().get("error").asText()).isEqualTo("NOT_FOUND");
    }

    @Test
    @DisplayName("Reading an organization that does not exist is a 404")
    void missingOrganizationIs404() {
        ResponseEntity<JsonNode> response =
                get("/internal/organizations/00000000-0000-0000-0000-000000000000", JsonNode.class);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @DisplayName("Two concurrent creates with the same tax id: exactly one wins")
    void concurrentCreatesWithSameTaxIdLeaveOneWinner() throws Exception {
        String taxId = nextTaxId();
        Callable<ResponseEntity<JsonNode>> attempt = () -> createOrganization(taxId, "Трка");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<ResponseEntity<JsonNode>>> results = pool.invokeAll(List.of(attempt, attempt));

            long created = 0;
            long conflicts = 0;
            for (Future<ResponseEntity<JsonNode>> future : results) {
                int status = future.get().getStatusCode().value();
                if (status == 201) {
                    created++;
                } else if (status == 409) {
                    conflicts++;
                }
            }

            assertThat(created).as("exactly one insert survives the unique index").isEqualTo(1);
            assertThat(conflicts).as("the loser is translated into a 409").isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }
}
