package zm.organization.membership;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import zm.organization.common.BaseEntity;
import zm.organization.organization.Organization;

/** One person's membership of one organization, within one product realm. */
@Entity
@Table(name = "organization_members")
@Getter
@Setter
public class OrganizationMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    /** Keycloak realm, e.g. {@code menu-app}. */
    @Column(nullable = false)
    private String realm;

    /** Keycloak {@code sub} within that realm. */
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;
}
