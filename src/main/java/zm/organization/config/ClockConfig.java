package zm.organization.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Time is injected rather than read from {@code LocalDateTime.now()} so tests
 * can pin it. Invite expiry is the first thing that needs it — asserting that
 * a code has expired should not mean sleeping for seven days.
 */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
