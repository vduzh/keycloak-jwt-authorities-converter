package io.github.vduzh.keycloak.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtGrantedAuthoritiesConverterTest {

    private KeycloakJwtGrantedAuthoritiesConverter converter;

    @BeforeEach
    void setUp() {
        converter = new KeycloakJwtGrantedAuthoritiesConverter();
    }

    @Test
    void shouldReturnEmptyWhenNoClaimNamesConfigured() {
        Jwt jwt = buildJwt(Map.of("roles", List.of("admin")));

        Collection<GrantedAuthority> result = converter.convert(jwt);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldExtractAuthoritiesFromSimpleClaim() {
        converter.setClaimNames(List.of("roles"));
        Jwt jwt = buildJwt(Map.of("roles", List.of("admin", "user")));

        Collection<GrantedAuthority> result = converter.convert(jwt);

        assertThat(result).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_admin", "ROLE_user");
    }

    @Test
    void shouldExtractAuthoritiesFromNestedClaim() {
        converter.setClaimNames(List.of("realm_access.roles"));
        Jwt jwt = buildJwt(Map.of("realm_access", Map.of("roles", List.of("admin", "user"))));

        Collection<GrantedAuthority> result = converter.convert(jwt);

        assertThat(result).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_admin", "ROLE_user");
    }

    @Test
    void shouldAddRolePrefix() {
        converter.setClaimNames(List.of("roles"));
        Jwt jwt = buildJwt(Map.of("roles", List.of("manager")));

        Collection<GrantedAuthority> result = converter.convert(jwt);

        assertThat(result).extracting(GrantedAuthority::getAuthority)
                .allMatch(a -> a.startsWith("ROLE_"));
    }

    @Test
    void shouldReturnEmptyWhenClaimIsNull() {
        converter.setClaimNames(List.of("missing_claim"));
        Jwt jwt = buildJwt(Map.of("other", "value"));

        Collection<GrantedAuthority> result = converter.convert(jwt);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenClaimIsNotCollection() {
        converter.setClaimNames(List.of("roles"));
        Jwt jwt = buildJwt(Map.of("roles", "not_a_collection"));

        Collection<GrantedAuthority> result = converter.convert(jwt);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldIgnoreNonStringClaimsInCollection() {
        converter.setClaimNames(List.of("roles"));
        Jwt jwt = buildJwt(Map.of("roles", List.of("admin", 123, "user")));

        Collection<GrantedAuthority> result = converter.convert(jwt);

        assertThat(result).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_admin", "ROLE_user");
    }

    @Test
    void shouldMergeAuthoritiesFromMultipleClaimNames() {
        converter.setClaimNames(List.of("roles", "realm_access.roles"));
        Jwt jwt = buildJwt(Map.of(
                "roles", List.of("admin"),
                "realm_access", Map.of("roles", List.of("manager"))
        ));

        Collection<GrantedAuthority> result = converter.convert(jwt);

        assertThat(result).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_admin", "ROLE_manager");
    }

    @Test
    void shouldDeduplicateAuthorities() {
        converter.setClaimNames(List.of("roles", "realm_access.roles"));
        Jwt jwt = buildJwt(Map.of(
                "roles", List.of("admin", "user"),
                "realm_access", Map.of("roles", List.of("admin", "manager"))
        ));

        Collection<GrantedAuthority> result = converter.convert(jwt);

        assertThat(result).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_admin", "ROLE_user", "ROLE_manager");
    }

    @Test
    void shouldContinueToNextClaimWhenPreviousClaimIsNull() {
        converter.setClaimNames(List.of("missing_claim", "roles"));
        Jwt jwt = buildJwt(Map.of("roles", List.of("admin")));

        Collection<GrantedAuthority> result = converter.convert(jwt);

        assertThat(result).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_admin");
    }

    @Test
    void shouldContinueToNextClaimWhenPreviousClaimIsNotCollection() {
        converter.setClaimNames(List.of("invalid", "roles"));
        Jwt jwt = buildJwt(Map.of("invalid", "not_a_collection", "roles", List.of("admin")));

        Collection<GrantedAuthority> result = converter.convert(jwt);

        assertThat(result).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_admin");
    }

    private Jwt buildJwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("test-user")
                .issuedAt(Instant.now())
                .claims(c -> c.putAll(claims))
                .build();
    }
}
