package zm.organization.common;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Shared JPA superclass: UUID primary key, creation + update timestamps.
 * Same shape as ivy-events-be and zm-iam-service; when {@code zm-commons}
 * is extracted this becomes a dependency instead of a copy.
 *
 * <p>The UUID is deliberately the public identifier too — products store
 * {@code orgId} as a foreign reference, so it must never be reused.
 */
@Data
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
