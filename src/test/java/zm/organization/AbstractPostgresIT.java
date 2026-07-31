package zm.organization;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base for integration tests: a real Postgres via Testcontainers, Flyway
 * running against it, IAM provisioning switched off because no IAM instance
 * exists inside the test JVM, and a stubbed JWT decoder so requests can carry
 * a token without a Keycloak.
 *
 * <p>The HTTP helpers attach the {@code org-client} token by default. Tests
 * that care about authorisation pass a different token explicitly.
 */
@Testcontainers
@Import(TestJwtDecoderConfig.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "iam.provisioning.enabled=false")
public abstract class AbstractPostgresIT {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("org_db")
                    .withUsername("org_user")
                    .withPassword("org_pass");

    static {
        POSTGRES.start();
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    protected TestRestTemplate rest;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    /**
     * For assertions and set-up that are clearer as SQL than as API calls —
     * ageing a row past its expiry, for instance.
     */
    protected JdbcTemplate jdbc() {
        return jdbcTemplate;
    }

    protected <T> ResponseEntity<T> get(String path, Class<T> type) {
        return exchange(HttpMethod.GET, path, null, TestJwtDecoderConfig.ORG_CLIENT_TOKEN, type);
    }

    protected <T> ResponseEntity<T> post(String path, String body, Class<T> type) {
        return exchange(HttpMethod.POST, path, body, TestJwtDecoderConfig.ORG_CLIENT_TOKEN, type);
    }

    protected <T> ResponseEntity<T> patch(String path, String body, Class<T> type) {
        return exchange(HttpMethod.PATCH, path, body, TestJwtDecoderConfig.ORG_CLIENT_TOKEN, type);
    }

    protected <T> ResponseEntity<T> delete(String path, Class<T> type) {
        return exchange(HttpMethod.DELETE, path, null, TestJwtDecoderConfig.ORG_CLIENT_TOKEN, type);
    }

    protected <T> ResponseEntity<T> exchange(HttpMethod method, String path, String body,
                                             String token, Class<T> type) {
        HttpHeaders headers = new HttpHeaders();
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return rest.exchange(path, method, new HttpEntity<>(body, headers), type);
    }
}
