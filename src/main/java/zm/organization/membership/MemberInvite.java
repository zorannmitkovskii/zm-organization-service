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

import java.time.LocalDateTime;

/**
 * A code an existing member hands to someone so they can join the
 * organization from any product realm.
 *
 * <p>Only the hash is stored. The plaintext is returned exactly once, in the
 * response to the request that created it.
 */
@Entity
@Table(name = "member_invites")
@Getter
@Setter
public class MemberInvite extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;

    @Column(name = "created_by_member")
    private java.util.UUID createdByMember;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "max_uses", nullable = false)
    private int maxUses;

    @Column(name = "use_count", nullable = false)
    private int useCount;
}
