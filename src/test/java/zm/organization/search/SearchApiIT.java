package zm.organization.search;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import zm.organization.AbstractPostgresIT;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The rate limit is raised for this class: the functional assertions issue far
 * more searches than a real onboarding would, and the limit itself is covered
 * by {@link SearchRateLimitIT} against the production default.
 */
@TestPropertySource(properties = "organization.search.rate-limit.max-per-window=1000")
class SearchApiIT extends AbstractPostgresIT {

    private static final AtomicInteger TAX_ID_SEQUENCE = new AtomicInteger(1);

    @Autowired
    private OrganizationSearchRepository searchRepository;

    private String givenOrganization(String name, String taxId, String email, String phone) {
        return post("/internal/organizations", """
                {
                  "name": "%s",
                  "taxId": "%s",
                  "contactEmail": "%s",
                  "contactPhone": "%s",
                  "createdByApp": "menu-app"
                }
                """.formatted(name, taxId, email, phone), JsonNode.class).getBody().get("id").asText();
    }

    private void givenLocation(String orgId, String city) {
        post("/internal/organizations/" + orgId + "/locations", """
                { "name": "Главна", "city": "%s", "country": "MK" }
                """.formatted(city), JsonNode.class);
    }

    private static String nextTaxId() {
        return String.format("41200%08d", TAX_ID_SEQUENCE.getAndIncrement());
    }

    private ResponseEntity<JsonNode> search(String query) {
        return get("/internal/organizations/search?" + query, JsonNode.class);
    }

    @Test
    @DisplayName("AC1 — an exact tax id finds the organization")
    void exactTaxIdMatch() {
        String taxId = nextTaxId();
        String orgId = givenOrganization("Панорама Тест", taxId, "kontakt@panorama.mk", "+38970123456");

        ResponseEntity<JsonNode> found = search("taxId=" + taxId + "&realm=menu-app");

        assertThat(found.getStatusCode().value()).isEqualTo(200);
        assertThat(found.getBody()).hasSize(1);
        assertThat(found.getBody().get(0).get("orgId").asText()).isEqualTo(orgId);
    }

    @Test
    @DisplayName("AC1 — an unknown tax id is an empty 200, not a 404")
    void unknownTaxIdIsEmpty() {
        ResponseEntity<JsonNode> found = search("taxId=9999999999999&realm=menu-app");

        assertThat(found.getStatusCode().value()).isEqualTo(200);
        assertThat(found.getBody()).isEmpty();
    }

    @Test
    @DisplayName("AC2 — a Cyrillic name is found by a Cyrillic query with the city")
    void cyrillicQueryFindsCyrillicName() {
        String orgId = givenOrganization("Ресторан Панорама Скопска",
                nextTaxId(), "kontakt@panorama.mk", "+38970123456");
        givenLocation(orgId, "Скопје");

        ResponseEntity<JsonNode> found = search("name=панорама&city=Скопје&realm=menu-app");

        assertThat(found.getBody()).isNotEmpty();
        // Containment, not position: the suite shares a database and several
        // seeded organizations legitimately match "панорама".
        assertThat(found.getBody().findValuesAsText("orgId")).contains(orgId);
        assertThat(matching(found.getBody(), orgId).get("city").asText()).isEqualTo("Скопје");
    }

    @Test
    @DisplayName("AC2 — the same organization is found by a Latin query")
    void latinQueryFindsCyrillicName() {
        String orgId = givenOrganization("Ресторан Панорамик",
                nextTaxId(), "kontakt@panoramik.mk", "+38970123456");

        ResponseEntity<JsonNode> found = search("name=restoran panoramik&realm=menu-app");

        assertThat(found.getBody()).isNotEmpty();
        assertThat(found.getBody().findValuesAsText("orgId")).contains(orgId);
    }

    @Test
    @DisplayName("AC2 — a typo still lands the organization in the results")
    void typoStillMatches() {
        String orgId = givenOrganization("Кафетерија Морнарица",
                nextTaxId(), "info@mornarica.mk", "+38970123456");

        ResponseEntity<JsonNode> found = search("name=kafeterija mornarica&realm=menu-app");

        assertThat(found.getBody().findValuesAsText("orgId")).contains(orgId);
    }

    @Test
    @DisplayName("AC3 — contact details come back masked")
    void contactDetailsAreMasked() {
        String taxId = nextTaxId();
        givenOrganization("Маскирана Фирма", taxId, "racunovodstvo@firma.mk", "+38970555444");

        JsonNode result = search("taxId=" + taxId + "&realm=menu-app").getBody().get(0);

        assertThat(result.get("contactEmail").asText()).isEqualTo("r***@firma.mk");
        assertThat(result.get("contactEmail").asText()).doesNotContain("acunovodstvo");
        assertThat(result.get("contactPhone").asText()).startsWith("+389").endsWith("444");
        assertThat(result.get("contactPhone").asText()).doesNotContain("70555");
    }

    @Test
    @DisplayName("AC4 — hasMembersInRealm reflects the realm asked about, not any realm")
    void hasMembersIsPerRealm() {
        String taxId = nextTaxId();
        String orgId = givenOrganization("Со членови", taxId, "a@b.mk", "+38970123456");
        post("/internal/organizations/" + orgId + "/members", """
                { "realm": "event-app", "userId": "ivy-user", "role": "OWNER" }
                """, JsonNode.class);

        JsonNode forEventApp = search("taxId=" + taxId + "&realm=event-app").getBody().get(0);
        JsonNode forMenuApp = search("taxId=" + taxId + "&realm=menu-app").getBody().get(0);

        assertThat(forEventApp.get("hasMembersInRealm").asBoolean()).isTrue();
        assertThat(forMenuApp.get("hasMembersInRealm").asBoolean())
                .as("the Ivy owner is not a menu-app member — that is the whole point of the flag")
                .isFalse();
    }

    @Test
    @DisplayName("AC5 — the trigram index carries the fuzzy query on a populated table")
    void fuzzySearchUsesTheGinIndex() {
        // 100k rather than the 10k the ticket names: at 10k a sequential scan is
        // genuinely cheaper and Postgres is right to choose it, so the assertion
        // would be testing the planner rather than the index.
        seedOrganizations(100_000);

        String plan = searchRepository.explainFuzzySearch("panorama skopje", 0.3, 5, "menu-app");

        assertThat(plan)
                .as("plan was:%n%s", plan)
                .contains("ix_organizations_search_name_trgm");
        assertThat(plan).doesNotContain("Seq Scan on organizations");
    }

    @Test
    @DisplayName("AC5 — fuzzy search over 10k organizations stays well under 50ms")
    void fuzzySearchIsFast() {
        seedOrganizations(10_000);

        // One warm-up so the measurement is not dominated by plan caching.
        searchRepository.findByNameSimilarity("panorama skopje", null, 0.3, 5, "menu-app");

        long startNanos = System.nanoTime();
        searchRepository.findByNameSimilarity("panorama skopje", null, 0.3, 5, "menu-app");
        long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;

        assertThat(elapsedMillis).isLessThan(50);
    }

    @Test
    @DisplayName("Searching with neither a tax id nor a name returns nothing rather than everything")
    void emptyQueryReturnsNothing() {
        ResponseEntity<JsonNode> found = search("realm=menu-app");

        assertThat(found.getStatusCode().value()).isEqualTo(200);
        assertThat(found.getBody()).isEmpty();
    }

    @Test
    @DisplayName("Results are capped, so a broad query cannot walk the registry")
    void resultsAreCapped() {
        seedOrganizations(10_000);

        ResponseEntity<JsonNode> found = search("name=firma&realm=menu-app");

        assertThat(found.getBody().size()).isLessThanOrEqualTo(5);
    }

    private static JsonNode matching(JsonNode results, String orgId) {
        for (JsonNode result : results) {
            if (result.get("orgId").asText().equals(orgId)) {
                return result;
            }
        }
        throw new AssertionError("No result for orgId " + orgId + " in " + results);
    }

    /**
     * Bulk-inserts directly: 10k rows through the API would take minutes and
     * test nothing that the API tests above do not already cover.
     */
    private void seedOrganizations(int count) {
        Integer existing = jdbc().queryForObject(
                "SELECT COUNT(*) FROM organizations WHERE created_by_app = 'seed'", Integer.class);
        if (existing != null && existing >= count) {
            return;
        }

        jdbc().update("""
                INSERT INTO organizations
                    (id, name, search_name, tax_id, default_lang, status, created_by_app, created_at)
                SELECT gen_random_uuid(),
                       'Фирма ' || g,
                       'firma ' || g,
                       '55' || lpad(g::text, 11, '0'),
                       'mk', 'ACTIVE', 'seed', now()
                  FROM generate_series(1, ?) g
                """, count);
        jdbc().execute("ANALYZE organizations");
    }
}
