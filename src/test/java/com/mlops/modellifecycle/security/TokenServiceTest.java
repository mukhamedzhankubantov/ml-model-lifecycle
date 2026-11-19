package com.mlops.modellifecycle.security;

import com.mlops.modellifecycle.config.JwtConfig;
import com.mlops.modellifecycle.config.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** No Spring context: the token is signed and read back with the same test secret. */
class TokenServiceTest {

    private static final String SECRET = "test-secret-0123456789-abcdefghijklmnop";
    private static final String ISSUER = "ml-model-lifecycle";

    private final JwtProperties props = new JwtProperties(SECRET, ISSUER, Duration.ofMinutes(15));
    private final JwtConfig config = new JwtConfig();
    private final TokenService tokens = new TokenService(config.jwtEncoder(props), props);
    private final JwtDecoder decoder = config.jwtDecoder(props);

    private String tokenFor(String user, String... roles) {
        return tokens.issue(UsernamePasswordAuthenticationToken.authenticated(
                user, null, AuthorityUtils.createAuthorityList(
                        java.util.Arrays.stream(roles).map(r -> "ROLE_" + r).toArray(String[]::new))));
    }

    @Test
    void issuedTokenCanBeDecoded() {
        Jwt jwt = decoder.decode(tokenFor("lead", Roles.LEAD));

        assertEquals("lead", jwt.getSubject());
        assertEquals(ISSUER, jwt.getClaimAsString("iss"));
        assertEquals(List.of(Roles.LEAD), jwt.getClaimAsStringList("roles"));
        assertTrue(jwt.getExpiresAt().isAfter(Instant.now()));
        assertEquals("HS256", jwt.getHeaders().get("alg"));
    }

    @Test
    void lifetimeIsFifteenMinutes() {
        Jwt jwt = decoder.decode(tokenFor("scientist", Roles.SCIENTIST));

        assertEquals(Duration.ofMinutes(15), Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()));
        assertEquals(900, tokens.expiresInSeconds());
    }

    @Test
    void payloadHasOnlyTheExpectedClaimsAndNoPassword() {
        Jwt jwt = decoder.decode(tokenFor("scientist", Roles.SCIENTIST));

        assertEquals(java.util.Set.of("iss", "sub", "iat", "exp", "roles"), jwt.getClaims().keySet());
        assertFalse(jwt.getClaims().toString().contains("password"));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtProperties other = new JwtProperties("another-secret-0123456789-abcdefghijk", ISSUER, Duration.ofMinutes(15));
        String forged = new TokenService(config.jwtEncoder(other), other)
                .issue(UsernamePasswordAuthenticationToken.authenticated(
                        "lead", null, AuthorityUtils.createAuthorityList("ROLE_" + Roles.LEAD)));

        assertThrows(BadJwtException.class, () -> decoder.decode(forged));
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() {
        JwtProperties other = new JwtProperties(SECRET, "helpdesk", Duration.ofMinutes(15));
        String foreign = new TokenService(config.jwtEncoder(other), other)
                .issue(UsernamePasswordAuthenticationToken.authenticated(
                        "lead", null, AuthorityUtils.createAuthorityList("ROLE_" + Roles.LEAD)));

        assertThrows(BadJwtException.class, () -> decoder.decode(foreign));
    }
}
