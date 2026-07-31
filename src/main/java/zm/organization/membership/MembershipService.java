package zm.organization.membership;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zm.organization.audit.AuditService;
import zm.organization.audit.AuditTargetType;
import zm.organization.common.exception.LastOwnerException;
import zm.organization.common.exception.ResourceNotFoundException;
import zm.organization.membership.dto.AddMemberRequest;
import zm.organization.membership.dto.MemberResponse;
import zm.organization.membership.dto.UpdateMemberRequest;
import zm.organization.organization.Organization;
import zm.organization.organization.OrganizationService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final OrganizationMemberRepository repository;
    private final OrganizationService organizationService;
    private final MembershipCache cache;
    private final AuditService audit;

    @Transactional
    public MemberResponse add(UUID orgId, AddMemberRequest request) {
        Organization organization = organizationService.requireOrganization(orgId);

        OrganizationMember member = repository
                .findByOrganizationIdAndRealmAndUserId(orgId, request.realm(), request.userId())
                .orElseGet(OrganizationMember::new);

        member.setOrganization(organization);
        member.setRealm(request.realm());
        member.setUserId(request.userId());
        member.setRole(request.role());

        OrganizationMember saved = repository.save(member);
        cache.invalidate(orgId, saved.getRealm(), saved.getUserId());
        audit.record("ADD_MEMBER", AuditTargetType.MEMBER, saved.getId(), List.of("realm", "role"));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listByOrganization(UUID orgId) {
        organizationService.requireOrganization(orgId);
        return repository.findByOrganizationIdOrderByCreatedAtAsc(orgId).stream()
                .map(MembershipService::toResponse)
                .toList();
    }

    @Transactional
    public MemberResponse changeRole(UUID memberId, UpdateMemberRequest request) {
        OrganizationMember member = require(memberId);

        if (member.getRole() == MemberRole.OWNER && request.role() != MemberRole.OWNER) {
            rejectIfLastOwner(member, "demote");
        }

        member.setRole(request.role());
        OrganizationMember saved = repository.save(member);
        cache.invalidate(saved.getOrganization().getId(), saved.getRealm(), saved.getUserId());
        audit.record("CHANGE_MEMBER_ROLE", AuditTargetType.MEMBER, memberId, List.of("role"));
        return toResponse(saved);
    }

    @Transactional
    public void remove(UUID memberId) {
        OrganizationMember member = require(memberId);

        if (member.getRole() == MemberRole.OWNER) {
            rejectIfLastOwner(member, "remove");
        }

        UUID orgId = member.getOrganization().getId();
        String realm = member.getRealm();
        String userId = member.getUserId();

        repository.delete(member);
        cache.invalidate(orgId, realm, userId);
        audit.record("REMOVE_MEMBER", AuditTargetType.MEMBER, memberId, List.of());
    }

    /**
     * The authorisation check every product calls before each protected
     * operation. Returns empty rather than throwing so the controller can map
     * it to 404 without an exception on a hot path.
     */
    public Optional<MemberRole> checkRole(UUID orgId, String realm, String userId) {
        return cache.get(orgId, realm, userId, () -> repository
                .findByOrganizationIdAndRealmAndUserId(orgId, realm, userId)
                .map(OrganizationMember::getRole));
    }

    /** Used by {@link InviteService} once a claim has consumed a use. */
    @Transactional
    OrganizationMember join(Organization organization, String realm, String userId, MemberRole role) {
        OrganizationMember member = new OrganizationMember();
        member.setOrganization(organization);
        member.setRealm(realm);
        member.setUserId(userId);
        member.setRole(role);

        OrganizationMember saved = repository.save(member);
        cache.invalidate(organization.getId(), realm, userId);
        audit.record("JOIN_VIA_INVITE", AuditTargetType.MEMBER, saved.getId(), List.of("realm", "role"));
        return saved;
    }

    Optional<OrganizationMember> findExisting(UUID orgId, String realm, String userId) {
        return repository.findByOrganizationIdAndRealmAndUserId(orgId, realm, userId);
    }

    private void rejectIfLastOwner(OrganizationMember member, String action) {
        long owners = repository.countByOrganizationIdAndRole(
                member.getOrganization().getId(), MemberRole.OWNER);
        if (owners <= 1) {
            throw new LastOwnerException(action);
        }
    }

    private OrganizationMember require(UUID memberId) {
        return repository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));
    }

    static MemberResponse toResponse(OrganizationMember member) {
        return new MemberResponse(
                member.getId(),
                member.getOrganization().getId(),
                member.getRealm(),
                member.getUserId(),
                member.getRole(),
                member.getCreatedAt());
    }
}
