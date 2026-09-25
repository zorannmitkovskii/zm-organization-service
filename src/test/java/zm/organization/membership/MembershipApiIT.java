package zm.organization.membership;

import tools.jackson.databind.JsonNode;
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

class MembershipApiIT extends AbstractPostgresIT {

    private static final AtomicInteger TAX_ID_SEQUENCE = new AtomicInteger(1);
    private static final String OWNER_USER = "f47ac10b-58cc-4372-a567-0e02b2c3d479";

    private String givenOrganization() {
        String taxId = String.format("41000%08d", TAX_ID_SEQUENCE.getAndIncrement());
        return post("/internal/organizations", """
                { "name": "Членство тест", "taxId": "%s", "createdByApp": "menu-app" }
                """.formatted(taxId), JsonNode.class).getBody().get("id").asText();
    }

    private String addMember(String orgId, String realm, String userId, String role) {
        return post("/internal/organizations/" + orgId + "/members", """
                { "realm": "%s", "userId": "%s", "role": "%s" }
                """.formatted(realm, userId, role), JsonNode.class).getBody().get("id").asText();
    }

    private ResponseEntity<JsonNode> check(String orgId, String realm, String userId) {
        return get("/internal/members/check?orgId=%s&realm=%s&userId=%s"
                .formatted(orgId, realm, userId), JsonNode.class);
    }

    private String createInvite(String orgId, String role, int maxUses, int ttlDays) {
        return post("/internal/organizations/" + orgId + "/invites", """
                { "role": "%s", "maxUses": %d, "ttlDays": %d }
                """.formatted(role, maxUses, ttlDays), JsonNode.class).getBody().get("code").asText();
    }

    private ResponseEntity<JsonNode> claim(String code, String realm, String userId) {
        return post("/internal/invites/claim", """
                { "code": "%s", "realm": "%s", "userId": "%s" }
                """.formatted(code, realm, userId), JsonNode.class);
    }

    @Test
    @DisplayName("AC1 — onboarding: organization plus first OWNER, then check returns OWNER")
    void onboardingCreatesOwner() {
        String orgId = givenOrganization();

        addMember(orgId, "menu-app", OWNER_USER, "OWNER");

        ResponseEntity<JsonNode> checked = check(orgId, "menu-app", OWNER_USER);
        assertThat(checked.getStatusCode().value()).isEqualTo(200);
        assertThat(checked.getBody().get("role").asText()).isEqualTo("OWNER");
    }

    @Test
    @DisplayName("AC2 — one code, two realms: two membership rows and a correct useCount")
    void inviteClaimedFromTwoRealms() {
        String orgId = givenOrganization();
        addMember(orgId, "menu-app", OWNER_USER, "OWNER");
        String code = createInvite(orgId, "ADMIN", 2, 7);

        ResponseEntity<JsonNode> first = claim(code, "menu-app", "user-x");
        ResponseEntity<JsonNode> second = claim(code, "presmetko", "user-y");

        assertThat(first.getStatusCode().value()).isEqualTo(200);
        assertThat(first.getBody().get("role").asText()).isEqualTo("ADMIN");
        assertThat(second.getStatusCode().value()).isEqualTo(200);
        assertThat(second.getBody().get("realm").asText()).isEqualTo("presmetko");

        ResponseEntity<JsonNode> members =
                get("/internal/organizations/" + orgId + "/members", JsonNode.class);
        assertThat(members.getBody()).hasSize(3);
    }

    @Test
    @DisplayName("AC3 — a spent code is 410, an unknown code is 404")
    void spentAndUnknownCodes() {
        String orgId = givenOrganization();
        addMember(orgId, "menu-app", OWNER_USER, "OWNER");
        String code = createInvite(orgId, "MEMBER", 1, 7);

        assertThat(claim(code, "menu-app", "first-user").getStatusCode().value()).isEqualTo(200);

        ResponseEntity<JsonNode> spent = claim(code, "menu-app", "second-user");
        assertThat(spent.getStatusCode().value()).isEqualTo(410);
        assertThat(spent.getBody().get("error").asText()).isEqualTo("INVITE_NOT_USABLE");

        ResponseEntity<JsonNode> unknown = claim("ZZZZZZZZ", "menu-app", "third-user");
        assertThat(unknown.getStatusCode().value()).isEqualTo(404);
        assertThat(unknown.getBody().get("error").asText()).isEqualTo("INVITE_NOT_FOUND");
    }

    @Test
    @DisplayName("AC3 — an expired code is 410 even with uses left")
    void expiredCodeIsGone() {
        String orgId = givenOrganization();
        addMember(orgId, "menu-app", OWNER_USER, "OWNER");
        String code = createInvite(orgId, "MEMBER", 5, 1);

        expireInvites();

        assertThat(claim(code, "menu-app", "late-user").getStatusCode().value()).isEqualTo(410);
    }

    @Test
    @DisplayName("AC4 — the last owner can be neither removed nor demoted")
    void lastOwnerIsProtected() {
        String orgId = givenOrganization();
        String ownerId = addMember(orgId, "menu-app", OWNER_USER, "OWNER");
        addMember(orgId, "menu-app", "regular-user", "MEMBER");

        ResponseEntity<JsonNode> demoted = patch("/internal/members/" + ownerId, """
                { "role": "MEMBER" }
                """, JsonNode.class);
        assertThat(demoted.getStatusCode().value()).isEqualTo(409);
        assertThat(demoted.getBody().get("error").asText()).isEqualTo("LAST_OWNER");

        ResponseEntity<JsonNode> removed = delete("/internal/members/" + ownerId, JsonNode.class);
        assertThat(removed.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    @DisplayName("AC4 — with a second owner present, the first may be demoted")
    void ownerCanBeDemotedWhenAnotherExists() {
        String orgId = givenOrganization();
        String firstOwner = addMember(orgId, "menu-app", OWNER_USER, "OWNER");
        addMember(orgId, "event-app", "second-owner", "OWNER");

        ResponseEntity<JsonNode> demoted = patch("/internal/members/" + firstOwner, """
                { "role": "ADMIN" }
                """, JsonNode.class);

        assertThat(demoted.getStatusCode().value()).isEqualTo(200);
        assertThat(demoted.getBody().get("role").asText()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("AC5 — claiming twice from the same identity is idempotent and costs no use")
    void repeatClaimIsIdempotent() {
        String orgId = givenOrganization();
        addMember(orgId, "menu-app", OWNER_USER, "OWNER");
        String code = createInvite(orgId, "MEMBER", 1, 7);

        String firstId = claim(code, "menu-app", "double-clicker").getBody().get("id").asText();
        ResponseEntity<JsonNode> again = claim(code, "menu-app", "double-clicker");

        assertThat(again.getStatusCode().value()).isEqualTo(200);
        assertThat(again.getBody().get("id").asText()).isEqualTo(firstId);

        ResponseEntity<JsonNode> members =
                get("/internal/organizations/" + orgId + "/members", JsonNode.class);
        assertThat(members.getBody()).hasSize(2);
    }

    @Test
    @DisplayName("AC6 — a removed member stops passing the check immediately")
    void removingAMemberInvalidatesTheCachedCheck() {
        String orgId = givenOrganization();
        addMember(orgId, "menu-app", OWNER_USER, "OWNER");
        String memberId = addMember(orgId, "menu-app", "temporary-user", "MEMBER");

        assertThat(check(orgId, "menu-app", "temporary-user").getStatusCode().value()).isEqualTo(200);

        delete("/internal/members/" + memberId, Void.class);

        assertThat(check(orgId, "menu-app", "temporary-user").getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @DisplayName("A role change is visible to the very next check")
    void roleChangeInvalidatesTheCachedCheck() {
        String orgId = givenOrganization();
        addMember(orgId, "menu-app", OWNER_USER, "OWNER");
        String memberId = addMember(orgId, "menu-app", "promoted-user", "MEMBER");

        assertThat(check(orgId, "menu-app", "promoted-user").getBody().get("role").asText())
                .isEqualTo("MEMBER");

        patch("/internal/members/" + memberId, """
                { "role": "ADMIN" }
                """, JsonNode.class);

        assertThat(check(orgId, "menu-app", "promoted-user").getBody().get("role").asText())
                .isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Checking a non-member is 404, which products map to their own 403")
    void checkForNonMemberIs404() {
        String orgId = givenOrganization();

        assertThat(check(orgId, "menu-app", "never-joined").getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @DisplayName("Two concurrent claims on a single-use code: exactly one becomes a member")
    void concurrentClaimsOnSingleUseCode() throws Exception {
        String orgId = givenOrganization();
        addMember(orgId, "menu-app", OWNER_USER, "OWNER");
        String code = createInvite(orgId, "MEMBER", 1, 7);

        Callable<ResponseEntity<JsonNode>> racerA = () -> claim(code, "menu-app", "racer-a");
        Callable<ResponseEntity<JsonNode>> racerB = () -> claim(code, "menu-app", "racer-b");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<ResponseEntity<JsonNode>>> results = pool.invokeAll(List.of(racerA, racerB));

            long joined = 0;
            long refused = 0;
            for (Future<ResponseEntity<JsonNode>> future : results) {
                int status = future.get().getStatusCode().value();
                if (status == 200) {
                    joined++;
                } else if (status == 410) {
                    refused++;
                }
            }

            assertThat(joined).as("the conditional UPDATE picks one winner").isEqualTo(1);
            assertThat(refused).as("the loser sees a spent code").isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * Ages every invite past its expiry. Cheaper and less brittle than moving
     * the application clock for a single case, and it exercises the same
     * WHERE clause the database evaluates.
     */
    private void expireInvites() {
        jdbc().update("UPDATE member_invites SET expires_at = expires_at - INTERVAL '30 days'");
    }
}
