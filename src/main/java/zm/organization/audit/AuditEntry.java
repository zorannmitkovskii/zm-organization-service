package zm.organization.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import zm.organization.common.BaseEntity;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "audit_entries")
@Getter
@Setter
public class AuditEntry extends BaseEntity {

    /** The {@code azp} claim — which service account made the call. */
    @Column(nullable = false)
    private String caller;

    @Column(nullable = false)
    private String operation;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private AuditTargetType targetType;

    @Column(name = "target_id")
    private UUID targetId;

    /** Field names only — never their values. See the V4 migration. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> detail;

    @Column(nullable = false)
    private boolean success;
}
