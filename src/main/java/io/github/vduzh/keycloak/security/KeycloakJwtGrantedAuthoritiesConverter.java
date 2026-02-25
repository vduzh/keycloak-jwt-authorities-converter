package io.github.vduzh.keycloak.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Extracts the {@link GrantedAuthority}s from a Keycloak {@link Jwt} token.
 *
 * <p>Uses SpEL expressions to navigate nested JWT claims (e.g. {@code realm_access.roles})
 * and converts them into Spring Security authorities with a {@code ROLE_} prefix.
 */
public final class KeycloakJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final Logger log = LoggerFactory.getLogger(KeycloakJwtGrantedAuthoritiesConverter.class);

    private static final String DEFAULT_AUTHORITY_PREFIX = "ROLE_";

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private List<String> claimNames = List.of();

    /**
     * Creates a new {@code KeycloakJwtGrantedAuthoritiesConverter}.
     */
    public KeycloakJwtGrantedAuthoritiesConverter() {
    }

    /**
     * Sets the JWT claim names to extract authorities from.
     * Supports dot notation for nested claims (e.g. {@code realm_access.roles}).
     *
     * @param claimNames the list of claim names
     */
    public void setClaimNames(List<String> claimNames) {
        this.claimNames = List.copyOf(claimNames);
    }

    @Override
    public Collection<GrantedAuthority> convert(@NonNull Jwt jwt) {
        log.debug("[JWT] Converting jwt for user: {} to granted authorities", jwt.getSubject());

        Set<String> authorities = getAuthorities(jwt);
        log.debug("[JWT] Extracted {} authorities: {} for user: {}", authorities.size(),
                authorities, jwt.getSubject());

        Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        for (String authority : authorities) {
            grantedAuthorities.add(new SimpleGrantedAuthority(DEFAULT_AUTHORITY_PREFIX + authority));
            log.trace("[JWT] Added authority: {} to granted authorities list", authority);
        }
        return grantedAuthorities;
    }

    private Set<String> getAuthorities(Jwt jwt) {
        log.debug("[JWT] Extracting authorities from jwt for user: {}", jwt.getSubject());

        Set<String> authorities = new HashSet<>();
        for (String claimName : claimNames) {
            log.trace("[JWT] Getting claims for: {}", claimName);

            // Convert realm_access.roles -> ['realm_access']['roles']
            String expressionString = claimName.replaceAll("([^.]+)", "['$1']");
            log.trace("[JWT] Generated SpEL expression: {}", expressionString);

            Expression exp = PARSER.parseExpression(expressionString);
            Object claims = exp.getValue(jwt.getClaims());
            if (claims == null) {
                log.debug("[JWT] No claims found for: {}", claimName);
                continue;
            }
            log.debug("[JWT] Found claims: {} for: {}", claims, claimName);

            // Process claim
            if (!(claims instanceof Collection)) {
                log.warn("[JWT] Collection of claims expected but received: {}. Ignoring it...", claims.getClass());
                continue;
            }
            for (Object claim : (Collection<?>) claims) {
                if (claim instanceof String) {
                    authorities.add((String) claim);
                    log.trace("[JWT] Added claim: {} to authority list", claim);
                } else {
                    log.warn("[JWT] Claim as string expected but received: {}. Ignoring it...", claim.getClass());
                }
            }
        }
        return authorities;
    }
}
