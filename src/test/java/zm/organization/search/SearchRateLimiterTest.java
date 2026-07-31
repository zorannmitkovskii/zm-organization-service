package zm.organization.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchRateLimiterTest {

    @Test
    @DisplayName("Calls are allowed up to the limit and refused after it")
    void refusesBeyondTheLimit() {
        SearchRateLimiter limiter = new SearchRateLimiter(3, 60);

        assertThat(limiter.tryAcquire("caller")).isTrue();
        assertThat(limiter.tryAcquire("caller")).isTrue();
        assertThat(limiter.tryAcquire("caller")).isTrue();
        assertThat(limiter.tryAcquire("caller")).isFalse();
    }

    @Test
    @DisplayName("Callers are counted separately — one noisy service must not block another")
    void countersArePerCaller() {
        SearchRateLimiter limiter = new SearchRateLimiter(2, 60);

        limiter.tryAcquire("menu-service");
        limiter.tryAcquire("menu-service");

        assertThat(limiter.tryAcquire("menu-service")).isFalse();
        assertThat(limiter.tryAcquire("ivy-events-be")).isTrue();
    }

    @Test
    @DisplayName("The window expires and the caller is allowed again")
    void windowExpires() throws InterruptedException {
        SearchRateLimiter limiter = new SearchRateLimiter(1, 1);

        assertThat(limiter.tryAcquire("caller")).isTrue();
        assertThat(limiter.tryAcquire("caller")).isFalse();

        Thread.sleep(1_200);

        assertThat(limiter.tryAcquire("caller")).isTrue();
    }
}
