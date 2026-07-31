package zm.organization.membership;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zm.organization.audit.AuditService;
import zm.organization.audit.AuditTargetType;
import zm.organization.common.exception.InviteNotFoundException;
import zm.organization.common.exception.InviteNotUsableException;
import zm.organization.membership.dto.ClaimInviteRequest;
import zm.organization.membership.dto.CreateInviteRequest;
import zm.organization.membership.dto.InviteResponse;
import zm.organization.membership.dto.MemberResponse;
import zm.organization.organization.Organization;
import zm.organization.organization.OrganizationService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InviteService {

    private final MemberInviteRepository repository;
    private final OrganizationService organizationService;
    private final MembershipService membershipService;
    private final InviteCodeGenerator codes;
    private final Clock clock;
    private final AuditService audit;

    @Value("${organization.invites.default-ttl-days:7}")
    private int defaultTtlDays;

    @Transactional
    public InviteResponse create(UUID orgId, CreateInviteRequest request) {
        Organization organization = organizationService.requireOrganization(orgId);

        String plaintext = codes.generate();
        int ttlDays = request.ttlDays() == null ? defaultTtlDays : request.ttlDays();

        MemberInvite invite = new MemberInvite();
        invite.setOrganization(organization);
        invite.setCodeHash(codes.hash(plaintext));
        invite.setRole(request.role());
        invite.setCreatedByMember(request.createdByMember());
        invite.setExpiresAt(LocalDateTime.now(clock).plusDays(ttlDays));
        invite.setMaxUses(request.maxUses() == null ? 1 : request.maxUses());
        invite.setUseCount(0);

        MemberInvite saved = repository.save(invite);
        audit.record("CREATE_INVITE", AuditTargetType.INVITE, saved.getId(), List.of("role", "maxUses", "expiresAt"));

        // The only moment the plaintext is ever returned.
        return new InviteResponse(saved.getId(), orgId, plaintext, saved.getRole(),
                saved.getExpiresAt(), saved.getMaxUses(), saved.getUseCount());
    }

    /**
     * Turns a code into a membership.
     *
     * <p>Not transactional as a whole, and deliberately so: the atomic step is
     * {@link MemberInviteRepository#consumeOneUse}, which decides the winner of
     * a race inside the database. Wrapping everything in one transaction would
     * not make the check-then-act any safer and would hold a row lock across
     * the membership insert.
     *
     * <p>Claiming twice from the same (realm, userId) returns the existing
     * membership without spending a use — people re-submit forms, and a code
     * with {@code maxUses = 1} must not be burned by a double click.
     */
    public MemberResponse claim(ClaimInviteRequest request) {
        MemberInvite invite = repository.findByCodeHash(codes.hash(request.code()))
                .orElseThrow(InviteNotFoundException::new);

        UUID orgId = invite.getOrganization().getId();

        Optional<OrganizationMember> alreadyMember =
                membershipService.findExisting(orgId, request.realm(), request.userId());
        if (alreadyMember.isPresent()) {
            return MembershipService.toResponse(alreadyMember.get());
        }

        consume(invite.getId());

        Organization organization = organizationService.requireOrganization(orgId);
        return MembershipService.toResponse(membershipService.join(
                organization, request.realm(), request.userId(), invite.getRole()));
    }

    /**
     * The transaction lives on the repository method, not here: this is called
     * from within the same bean, so a {@code @Transactional} annotation on it
     * would be ignored by the proxy — the classic self-invocation trap.
     */
    private void consume(UUID inviteId) {
        int updated = repository.consumeOneUse(inviteId, LocalDateTime.now(clock));
        if (updated == 0) {
            throw new InviteNotUsableException();
        }
    }
}
