package zm.organization.search;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Caps how often one caller may search.
 *
 * <p>Search is the only endpoint that answers questions about organizations
 * the caller has no membership in. Unthrottled, it is a way to walk the whole
 * registry one tax id at a time and build a list of every company on the
 * platform — the masking in {@link OrganizationSummary} limits what each answer
 * reveals, and this limits how many answers there are.
 *
 * <p>A fixed window, not a sliding one: the imprecision at the boundary is
 * irrelevant here, and the counter is a single atomic per caller instead of a
 * timestamp list.
 */
@Component
public class SearchRateLimiter {

    private final Cache<String, AtomicInteger> windows;
    private final int maxPerWindow;

    public SearchRateLimiter(
            @Value("${organization.search.rate-limit.max-per-window:30}") int maxPerWindow,
            @Value("${organization.search.rate-limit.window-seconds:60}") long windowSeconds) {
        this.maxPerWindow = maxPerWindow;
        this.windows = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(windowSeconds))
                .maximumSize(10_000)
                .build();
    }

    /** @return true when the caller may proceed. */
    public boolean tryAcquire(String caller) {
        AtomicInteger counter = windows.get(caller, key -> new AtomicInteger());
        return counter.incrementAndGet() <= maxPerWindow;
    }
}
