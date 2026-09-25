package zm.organization.audit;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import zm.organization.AbstractPostgresIT;
import zm.organization.TestJwtDecoderConfig;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** ORG-04 AC3 and AC4. */
class AuditIT extends AbstractPostgresIT {

    private static final AtomicInteger TAX_ID_SEQUENCE = new AtomicInteger(1);

    @Autowired
    private AuditEntryRepository auditEntries;

    private String createOrganization() {
        String taxId = String.format("41100%08d", TAX_ID_SEQUENCE.getAndIncrement());
        return post("/internal/organizations", """
                { "name": "Аудит тест", "taxId": "%s", "createdByApp": "menu-app" }
                """.formatted(taxId), JsonNode.class).getBody().get("id").asText();
    }

    private List<AuditEntry> awaitEntriesFor(String targetId) {
        await().atMost(Duration.ofSeconds(5)).until(
                () -> !auditEntries.findByTargetIdOrderByCreatedAtDesc(UUID.fromString(targetId)).isEmpty());
        return auditEntries.findByTargetIdOrderByCreatedAtDesc(UUID.fromString(targetId));
    }

    @Test
    @DisplayName("AC3 — a create leaves an entry naming the calling service account")
    void createIsAudited() {
        String orgId = createOrganization();

        List<AuditEntry> entries = awaitEntriesFor(orgId);

        assertThat(entries).hasSize(1);
        AuditEntry entry = entries.getFirst();
        assertThat(entry.getOperation()).isEqualTo("CREATE_ORGANIZATION");
        assertThat(entry.getTargetType()).isEqualTo(AuditTargetType.ORG);
        assertThat(entry.getCaller()).isEqualTo(TestJwtDecoderConfig.CALLER);
        assertThat(entry.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("AC3 — an update records which fields changed, never their values")
    void updateRecordsFieldNamesOnly() {
        String orgId = createOrganization();

        patch("/internal/organizations/" + orgId, """
                { "contactPhone": "+38971999888", "website": "https://audit.mk" }
                """, JsonNode.class);

        await().atMost(Duration.ofSeconds(5)).until(
                () -> auditEntries.findByTargetIdOrderByCreatedAtDesc(UUID.fromString(orgId)).size() >= 2);

        AuditEntry update = auditEntries.findByTargetIdOrderByCreatedAtDesc(UUID.fromString(orgId))
                .stream()
                .filter(e -> e.getOperation().equals("UPDATE_ORGANIZATION"))
                .findFirst()
                .orElseThrow();

        assertThat(update.getDetail()).containsExactlyInAnyOrder("contactPhone", "website");
        assertThat(update.getDetail()).doesNotContain("+38971999888", "https://audit.mk");
    }

    @Test
    @DisplayName("AC3 — reads are not audited; members/check alone would swamp the table")
    void readsAreNotAudited() {
        String orgId = createOrganization();
        awaitEntriesFor(orgId);
        long afterCreate = auditEntries.count();

        get("/internal/organizations/" + orgId, JsonNode.class);
        get("/internal/organizations/" + orgId + "/members", JsonNode.class);
        get("/internal/members/check?orgId=%s&realm=menu-app&userId=nobody".formatted(orgId),
                JsonNode.class);

        assertThat(auditEntries.count()).isEqualTo(afterCreate);
    }

    @Test
    @DisplayName("AC3 — suspending records the status change against the organization")
    void suspendIsAudited() {
        String orgId = createOrganization();

        delete("/internal/organizations/" + orgId, Void.class);

        await().atMost(Duration.ofSeconds(5)).until(
                () -> auditEntries.findByTargetIdOrderByCreatedAtDesc(UUID.fromString(orgId)).stream()
                        .anyMatch(e -> e.getOperation().equals("SUSPEND_ORGANIZATION")));
    }

    @Test
    @DisplayName("AC4 — a failing audit write does not fail the operation")
    void auditFailureDoesNotBreakTheOperation() {
        // Make the audit insert impossible without touching anything the
        // business path needs: the operation column stops accepting the values
        // AuditService writes.
        // NOT VALID so the constraint applies to new inserts only — earlier
        // tests in this class have already written perfectly good rows.
        jdbc().execute("ALTER TABLE audit_entries ADD CONSTRAINT audit_always_fails "
                + "CHECK (operation = 'impossible') NOT VALID");
        try {
            String orgId = createOrganization();

            assertThat(orgId).isNotBlank();
            assertThat(get("/internal/organizations/" + orgId, JsonNode.class)
                    .getStatusCode().value()).isEqualTo(200);
        } finally {
            jdbc().execute("ALTER TABLE audit_entries DROP CONSTRAINT audit_always_fails");
        }
    }

    @Test
    @DisplayName("Membership writes are audited as MEMBER operations")
    void membershipWritesAreAudited() {
        String orgId = createOrganization();

        String memberId = post("/internal/organizations/" + orgId + "/members", """
                { "realm": "menu-app", "userId": "audited-user", "role": "OWNER" }
                """, JsonNode.class).getBody().get("id").asText();

        List<AuditEntry> entries = awaitEntriesFor(memberId);

        assertThat(entries).extracting(AuditEntry::getOperation).contains("ADD_MEMBER");
        assertThat(entries).allSatisfy(entry ->
                assertThat(entry.getTargetType()).isEqualTo(AuditTargetType.MEMBER));
        assertThat(entries.getFirst().getDetail())
                .as("field names only, never the userId itself")
                .isEqualTo(List.of("realm", "role"));
    }

    @Test
    @DisplayName("Every audited operation names a real caller, never anonymous")
    void callerIsAlwaysAttributed() {
        String orgId = createOrganization();
        awaitEntriesFor(orgId);

        Map<String, Object> anonymous = jdbc().queryForMap(
                "SELECT COUNT(*) AS anon FROM audit_entries WHERE caller = 'anonymous'");

        assertThat(((Number) anonymous.get("anon")).intValue()).isZero();
    }
}
