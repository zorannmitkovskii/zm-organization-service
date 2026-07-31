package zm.organization.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Audit writes run off the request thread.
 *
 * <p>The executor is wrapped so the security context travels with the task —
 * without it, {@code SecurityContextHolder} is empty on the audit thread and
 * every entry would be attributed to "anonymous", which defeats the purpose of
 * the trail.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("taskExecutor")
    Executor auditTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("audit-");
        // Audit is fail-open, and that has to hold under saturation too: the
        // default policy throws back into the calling request thread, so a
        // flooded queue would start failing writes. Dropping the entry and
        // logging is the behaviour the ticket asks for.
        executor.setRejectedExecutionHandler((task, pool) ->
                log.error("[Audit] Executor saturated — audit entry dropped"));
        executor.initialize();
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }
}
