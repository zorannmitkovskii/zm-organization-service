package zm.organization.config;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ORG-04. Every {@code /internal/**} endpoint requires a JWT issued by the
 * {@code zm-services} realm carrying the {@code org-client} realm role.
 *
 * <p>Authorisation philosophy, stated plainly because it is unusual: this
 * service <em>trusts backends</em>. Any caller holding {@code org-client} sees
 * every organization. Semantic authorisation — "may this user edit that menu"
 * — belongs to the product, which asks {@code /internal/members/check} for the
 * user's role and decides for itself. Scoping the registry per service would
 * defeat its purpose, which is precisely to be shared.
 *
 * <p>Health and metrics stay open: the Docker healthcheck and the metrics
 * scraper have no token, and neither reveals registry data.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain internalApiFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Error and forward dispatches are separate trips through
                        // the filter chain. Without this they land on denyAll, so
                        // an exception inside a handler comes back as 403 and the
                        // real failure never reaches the caller or the logs.
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus")
                        .permitAll()
                        .requestMatchers("/internal/**").hasRole("org-client")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new KeycloakRealmRolesJwtConverter())));
        return http.build();
    }

    /**
     * Built from the JWKS URI rather than from {@code issuer-uri}, because
     * Spring Boot's issuer-uri auto-configuration performs OIDC discovery
     * during context startup: the service would refuse to boot whenever
     * Keycloak happened to be down or still starting. This decoder fetches the
     * key set on first use instead, and the issuer claim is still validated —
     * just by an explicit validator rather than as a side effect of discovery.
     */
    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    JwtDecoder jwtDecoder(@Value("${iam.security.internal.jwk-set-uri}") String jwkSetUri,
                          @Value("${iam.security.internal.issuer-uri}") String issuerUri) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
        return decoder;
    }
}
