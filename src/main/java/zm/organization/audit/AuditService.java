package zm.organization.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Records write operations.
 *
 * <p><strong>Fail-open.</strong> A failure to write the audit row is logged
 * and swallowed: losing the trail for one operation is bad, but refusing a
 * legitimate write because the audit table is unavailable is worse. The ERROR
 * log is the alerting signal.
 *
 * <p>Asynchronous so the caller never waits on it. The consequence is that the
 * row appears shortly after the response — tests must poll rather than assert
 * immediately.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private static final String UNKNOWN_CALLER = "anonymous";

    private final AuditEntryRepository repository;

    @Async
    public void record(String operation, AuditTargetType targetType, UUID targetId, List<String> detail) {
        write(operation, targetType, targetId, detail, true);
    }

    @Async
    public void recordFailure(String operation, AuditTargetType targetType, UUID targetId, List<String> detail) {
        write(operation, targetType, targetId, detail, false);
    }

    private void write(String operation, AuditTargetType targetType, UUID targetId,
                       List<String> detail, boolean success) {
        try {
            AuditEntry entry = new AuditEntry();
            entry.setCaller(currentCaller());
            entry.setOperation(operation);
            entry.setTargetType(targetType);
            entry.setTargetId(targetId);
            entry.setDetail(detail);
            entry.setSuccess(success);
            repository.save(entry);
        } catch (RuntimeException e) {
            log.error("[Audit] Failed to record {} on {} {}: {}",
                    operation, targetType, targetId, e.getMessage());
        }
    }

    /**
     * Resolved here rather than passed in by every caller: the security context
     * is a thread-local that {@code @Async} would otherwise lose, so it is read
     * eagerly — Spring propagates it when the delegating security context
     * strategy is set, which {@link zm.organization.config.AsyncConfig} does.
     */
    private static String currentCaller() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return UNKNOWN_CALLER;
        }
        return authentication.getName();
    }
}
