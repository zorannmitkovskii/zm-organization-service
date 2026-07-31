package zm.organization.membership;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import zm.organization.membership.dto.ClaimInviteRequest;
import zm.organization.membership.dto.CreateInviteRequest;
import zm.organization.membership.dto.InviteResponse;
import zm.organization.membership.dto.MemberResponse;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class InviteController {

    private final InviteService service;

    @PostMapping("/internal/organizations/{orgId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    public InviteResponse create(@PathVariable UUID orgId,
                                 @Valid @RequestBody CreateInviteRequest request) {
        return service.create(orgId, request);
    }

    /**
     * Not scoped to an organization in the URL: the code itself identifies
     * which organization is being joined, and the claimant does not know the
     * orgId — that is the whole point of handing them a code.
     */
    @PostMapping("/internal/invites/claim")
    public MemberResponse claim(@Valid @RequestBody ClaimInviteRequest request) {
        return service.claim(request);
    }
}
