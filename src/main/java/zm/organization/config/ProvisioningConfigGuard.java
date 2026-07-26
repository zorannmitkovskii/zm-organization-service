package zm.organization.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import zm.iam.provisioning.client.IamProvisioningProperties;

/**
 * Fails startup with an actionable message when the IAM provisioning
 * configuration contains an unresolved placeholder.
 *
 * <p>Spring Boot's {@code @ConfigurationProperties} binder resolves
 * placeholders with {@code ignoreUnresolvablePlaceholders = true}. A missing
 * environment variable therefore does not fail the bind — it silently stores
 * the literal string {@code ${IAM_BASE_URL}}, which only surfaces much later
 * as {@code URISyntaxException: Illegal character in path at index 1}. That
 * exact failure took ivy-events-be down in production on 2026-07-25.
 *
 * <p>This guard runs at bean initialisation, well before
 * {@code ProvisioningStartupListener} fires on ApplicationReadyEvent, so the
 * operator sees which variable is missing instead of a URI parse error.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "iam.provisioning", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class ProvisioningConfigGuard {

    private static final String PLACEHOLDER_MARKER = "${";

    private final IamProvisioningProperties properties;

    @PostConstruct
    void verify() {
        requireResolved("iam.provisioning.base-url", "IAM_BASE_URL", properties.getBaseUrl());
        requireResolved("iam.provisioning.token", "IAM_PROVISIONING_TOKEN", properties.getToken());
    }

    private static void requireResolved(String property, String envVar, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    property + " is not set. Set the " + envVar + " environment variable, "
                            + "or disable provisioning with iam.provisioning.enabled=false.");
        }
        if (value.contains(PLACEHOLDER_MARKER)) {
            throw new IllegalStateException(
                    property + " is an unresolved placeholder (" + value + "). "
                            + "The " + envVar + " environment variable is missing in this environment.");
        }
    }
}
