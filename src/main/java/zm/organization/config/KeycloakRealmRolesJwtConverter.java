package zm.organization.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Keycloak puts realm roles in {@code realm_access.roles}; Spring's default
 * converter only reads {@code scope}. Same converter as zm-iam-service uses —
 * copied rather than shared because there is no commons module yet.
 *
 * <p>Client roles ({@code resource_access.<client>.roles}) are deliberately not
 * translated: the platform's authorisation model uses realm roles only, and
 * folding client roles in would let two clients collide on a role name.
 *
 * <p>The principal name is {@code azp}, the calling service account's client
 * id. That is exactly what the audit trail records as the caller.
 */
public class KeycloakRealmRolesJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>(scopeConverter.convert(jwt));
        for (String role : realmRoles(jwt)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }

        String name = jwt.getClaimAsString("azp");
        if (name == null) {
            name = jwt.getSubject();
        }
        return new JwtAuthenticationToken(jwt, authorities, name);
    }

    private static List<String> realmRoles(Jwt jwt) {
        Object claim = jwt.getClaims().get("realm_access");
        if (!(claim instanceof Map<?, ?> map)) {
            return List.of();
        }
        if (!(map.get("roles") instanceof List<?> roles)) {
            return List.of();
        }
        List<String> out = new ArrayList<>(roles.size());
        for (Object role : roles) {
            if (role instanceof String s) {
                out.add(s);
            }
        }
        return out;
    }
}
