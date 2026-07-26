package zm.organization.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import zm.iam.provisioning.client.IamProvisioningProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The guard exists because Spring Boot binds unresolved placeholders as
 * literal strings instead of failing — see the class javadoc.
 */
class ProvisioningConfigGuardTest {

    private static IamProvisioningProperties props(String baseUrl, String token) {
        IamProvisioningProperties p = new IamProvisioningProperties();
        p.setBaseUrl(baseUrl);
        p.setToken(token);
        return p;
    }

    @Test
    @DisplayName("Resolved configuration passes")
    void resolvedConfigurationPasses() {
        var guard = new ProvisioningConfigGuard(props("http://ivy-iam:8383", "a-real-token"));

        assertThatCode(guard::verify).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Unresolved base-url placeholder names the missing variable")
    void unresolvedBaseUrlIsRejected() {
        var guard = new ProvisioningConfigGuard(props("${IAM_BASE_URL}", "a-real-token"));

        assertThatThrownBy(guard::verify)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("iam.provisioning.base-url")
                .hasMessageContaining("IAM_BASE_URL");
    }

    @Test
    @DisplayName("Unresolved token placeholder names the missing variable")
    void unresolvedTokenIsRejected() {
        var guard = new ProvisioningConfigGuard(props("http://ivy-iam:8383", "${IAM_PROVISIONING_TOKEN}"));

        assertThatThrownBy(guard::verify)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("IAM_PROVISIONING_TOKEN");
    }

    @Test
    @DisplayName("Blank token is rejected — an empty default is still a misconfiguration")
    void blankTokenIsRejected() {
        var guard = new ProvisioningConfigGuard(props("http://ivy-iam:8383", "  "));

        assertThatThrownBy(guard::verify)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("iam.provisioning.token");
    }

    @Test
    @DisplayName("Null base-url points the operator at the disable switch")
    void nullBaseUrlMentionsDisableSwitch() {
        var guard = new ProvisioningConfigGuard(props(null, "a-real-token"));

        assertThatThrownBy(guard::verify)
                .isInstanceOf(IllegalStateException.class)
                .satisfies(e -> assertThat(e.getMessage()).contains("iam.provisioning.enabled=false"));
    }
}
