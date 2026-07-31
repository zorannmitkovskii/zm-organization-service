package zm.organization.membership;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import zm.organization.common.exception.ResourceNotFoundException;
import zm.organization.membership.dto.AddMemberRequest;
import zm.organization.membership.dto.MemberResponse;
import zm.organization.membership.dto.UpdateMemberRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MembershipService service;

    @PostMapping("/internal/organizations/{orgId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse add(@PathVariable UUID orgId,
                              @Valid @RequestBody AddMemberRequest request) {
        return service.add(orgId, request);
    }

    @GetMapping("/internal/organizations/{orgId}/members")
    public List<MemberResponse> list(@PathVariable UUID orgId) {
        return service.listByOrganization(orgId);
    }

    @PatchMapping("/internal/members/{id}")
    public MemberResponse changeRole(@PathVariable UUID id,
                                     @Valid @RequestBody UpdateMemberRequest request) {
        return service.changeRole(id, request);
    }

    @DeleteMapping("/internal/members/{id}")
    public ResponseEntity<Void> remove(@PathVariable UUID id) {
        service.remove(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * The hot path: products call this before every protected operation.
     *
     * <p>404 means "not a member" — the calling product turns that into its own
     * 403. A body would be misleading here, so the answer is the status plus
     * the role when there is one.
     */
    @GetMapping("/internal/members/check")
    public Map<String, String> check(@RequestParam UUID orgId,
                                     @RequestParam String realm,
                                     @RequestParam String userId) {
        return service.checkRole(orgId, realm, userId)
                .map(role -> Map.of("role", role.name()))
                .orElseThrow(() -> new ResourceNotFoundException("Membership", orgId));
    }
}
