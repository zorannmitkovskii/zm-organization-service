package zm.organization;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.BadJwtException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Replaces JWKS signature verification with a lookup over three canned tokens.
 *
 * <p>Everything else about the security configuration stays real: the filter
 * chain, the request matchers, the realm-role to authority conversion and the
 * 401/403 decisions are all the production ones. Only the signature check is
 * stubbed, because verifying it would mean running a Keycloak for tests whose
 * subject is authorisation, not cryptography. {@code SecurityIT} covers the
 * unauthenticated and wrong-role paths against this same chain.
 */
@TestConfiguration
public class TestJwtDecoderConfig {

    public static final String ORG_CLIENT_TOKEN = "test-token-with-org-client";
    public static final String NO_ROLE_TOKEN = "test-token-without-org-client";
    public static final String CALLER = "zm-menu-service-svc";

    @Bean
    @Primary
    JwtDecoder testJwtDecoder() {
        return token -> switch (token) {
            case ORG_CLIENT_TOKEN -> jwt(CALLER, List.of("org-client"));
            case NO_ROLE_TOKEN -> jwt("ivy-events-be-svc", List.of("iam-client"));
            // BadJwtException, not JwtException: the token is bad, the decoder
            // is fine. Spring maps the former to 401 and the latter to 500.
            default -> throw new BadJwtException("Unrecognised test token");
        };
    }

    private static Jwt jwt(String azp, List<String> realmRoles) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("stub")
                .header("alg", "none")
                .subject("service-account-" + azp)
                .claim("azp", azp)
                .claim("realm_access", Map.of("roles", realmRoles))
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .build();
    }
}
