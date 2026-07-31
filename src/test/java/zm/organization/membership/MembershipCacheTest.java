package zm.organization.membership;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class MembershipCacheTest {

    private static final UUID ORG = UUID.randomUUID();

    private final MembershipCache cache = new MembershipCache(45);

    @Test
    @DisplayName("The loader runs once; the second read is served from cache")
    void secondReadIsCached() {
        AtomicInteger loads = new AtomicInteger();

        for (int i = 0; i < 3; i++) {
            cache.get(ORG, "menu-app", "user-1", () -> {
                loads.incrementAndGet();
                return Optional.of(MemberRole.OWNER);
            });
        }

        assertThat(loads.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("A negative answer is cached too — non-members are just as hot a path")
    void negativeAnswersAreCached() {
        AtomicInteger loads = new AtomicInteger();

        for (int i = 0; i < 3; i++) {
            Optional<MemberRole> role = cache.get(ORG, "menu-app", "stranger", () -> {
                loads.incrementAndGet();
                return Optional.empty();
            });
            assertThat(role).isEmpty();
        }

        assertThat(loads.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Invalidation reaches exactly one key and forces a reload")
    void invalidationIsPrecise() {
        cache.get(ORG, "menu-app", "user-1", () -> Optional.of(MemberRole.OWNER));
        cache.get(ORG, "menu-app", "user-2", () -> Optional.of(MemberRole.MEMBER));

        cache.invalidate(ORG, "menu-app", "user-1");

        AtomicInteger reloads = new AtomicInteger();
        cache.get(ORG, "menu-app", "user-1", () -> {
            reloads.incrementAndGet();
            return Optional.empty();
        });
        cache.get(ORG, "menu-app", "user-2", () -> {
            reloads.incrementAndGet();
            return Optional.empty();
        });

        assertThat(reloads.get()).as("only the invalidated key reloads").isEqualTo(1);
    }

    @Test
    @DisplayName("The same user in two realms is two independent entries")
    void realmIsPartOfTheKey() {
        cache.get(ORG, "menu-app", "user-1", () -> Optional.of(MemberRole.OWNER));

        Optional<MemberRole> otherRealm =
                cache.get(ORG, "event-app", "user-1", () -> Optional.of(MemberRole.MEMBER));

        assertThat(otherRealm).contains(MemberRole.MEMBER);
    }
}
