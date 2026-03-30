package com.jobportal.commonsecurity.security;

import com.jobportal.commonsecurity.config.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(Base64.getEncoder().encodeToString("12345678901234567890123456789012".getBytes()));
        properties.setExpiration(60_000L);
        properties.setRefreshExpiration(120_000L);
        jwtUtil = new JwtUtil(properties);
    }

    @Test
    void generateTokenAndExtractClaims() {
        String token = jwtUtil.generateToken(10L, "user@example.com", "JOB_SEEKER");

        Claims claims = jwtUtil.extractClaims(token);

        assertThat(claims.getSubject()).isEqualTo("user@example.com");
        assertThat(((Number) claims.get("userId")).longValue()).isEqualTo(10L);
        assertThat(claims.get("role", String.class)).isEqualTo("JOB_SEEKER");
        assertThat(claims.get("tokenType", String.class)).isEqualTo("ACCESS");
        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void buildAuthenticationCreatesJwtPrincipalAndRoleAuthority() {
        String token = jwtUtil.generateAccessToken(22L, "recruiter@example.com", "RECRUITER");

        Authentication authentication = jwtUtil.buildAuthentication(token);

        assertThat(authentication.getPrincipal()).isInstanceOf(JwtUserPrincipal.class);
        JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
        assertThat(principal.userId()).isEqualTo(22L);
        assertThat(principal.email()).isEqualTo("recruiter@example.com");
        assertThat(principal.role()).isEqualTo("RECRUITER");
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_RECRUITER");
    }

    @Test
    void refreshTokenReturnsNewValidAccessTokenWhenUsingRefreshToken() {
        String refreshToken = jwtUtil.generateRefreshToken(5L, "admin@example.com", "ADMIN");

        String refreshedToken = jwtUtil.refreshToken(refreshToken);

        assertThat(refreshedToken).isNotBlank();
        assertThat(jwtUtil.isTokenValid(refreshedToken)).isTrue();
        assertThat(jwtUtil.extractClaims(refreshedToken).getSubject()).isEqualTo("admin@example.com");
        assertThat(jwtUtil.extractClaims(refreshedToken).get("tokenType", String.class)).isEqualTo("ACCESS");
    }

    @Test
    void accessTokenCannotBeUsedAsRefreshToken() {
        String accessToken = jwtUtil.generateAccessToken(5L, "admin@example.com", "ADMIN");

        assertThatThrownBy(() -> jwtUtil.refreshToken(accessToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("refresh token");
    }

    @Test
    void refreshTokenCannotBeUsedForAuthentication() {
        String refreshToken = jwtUtil.generateRefreshToken(5L, "admin@example.com", "ADMIN");

        assertThatThrownBy(() -> jwtUtil.buildAuthentication(refreshToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Refresh tokens cannot be used for authentication");
    }

    @Test
    void invalidTokenIsRejected() {
        assertThat(jwtUtil.isTokenValid("not-a-jwt")).isFalse();
        assertThatThrownBy(() -> jwtUtil.extractClaims("not-a-jwt"))
                .isInstanceOf(Exception.class);
    }
}