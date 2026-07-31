package zm.organization.membership;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Caches the answer to "what role does this user hold in this organization".
 *
 * <p>That question is asked before every protected operation in every product,
 * so it is the hottest path in the service. Negative answers are cached too —
 * a product checking a user who is not a member is just as common as one who
 * is, and leaving those uncached would let an unauthenticated caller push
 * every request through to the database.
 *
 * <p>Entries are evicted by exact key when a membership changes, so a revoked
 * member stops passing checks immediately rather than at the end of the TTL.
 * The TTL is the backstop for changes this instance did not make — another
 * replica, or a direct database edit.
 */
@Component
public class MembershipCache {

    private final Cache<Key, Optional<MemberRole>> cache;

    public MembershipCache(@Value("${organization.membership.cache-ttl-seconds:45}") long ttlSeconds) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .maximumSize(50_000)
                .build();
    }

    public Optional<MemberRole> get(UUID orgId, String realm, String userId,
                                    Supplier<Optional<MemberRole>> loader) {
        return cache.get(new Key(orgId, realm, userId), key -> loader.get());
    }

    public void invalidate(UUID orgId, String realm, String userId) {
        cache.invalidate(new Key(orgId, realm, userId));
    }

    private record Key(UUID orgId, String realm, String userId) {
    }
}
