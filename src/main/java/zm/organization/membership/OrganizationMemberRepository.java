package zm.organization.membership;

import zm.organization.common.BaseRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationMemberRepository extends BaseRepository<OrganizationMember> {

    Optional<OrganizationMember> findByOrganizationIdAndRealmAndUserId(
            UUID organizationId, String realm, String userId);

    List<OrganizationMember> findByOrganizationIdOrderByCreatedAtAsc(UUID organizationId);

    /**
     * Counts owners across every realm. An organization's last owner is the
     * last one anywhere — losing the only owner in {@code menu-app} while one
     * remains in {@code event-app} is fine.
     */
    long countByOrganizationIdAndRole(UUID organizationId, MemberRole role);
}
