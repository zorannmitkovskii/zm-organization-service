package zm.organization.audit;

import zm.organization.common.BaseRepository;

import java.util.List;
import java.util.UUID;

public interface AuditEntryRepository extends BaseRepository<AuditEntry> {

    List<AuditEntry> findByTargetIdOrderByCreatedAtDesc(UUID targetId);
}
