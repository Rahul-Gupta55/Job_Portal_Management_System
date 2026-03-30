package com.jobportal.commonsecurity.security;

import com.jobportal.commonsecurity.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

public class JwtUtil {

    private static final String ROLE_CLAIM = "role";
    private static final String USER_ID_CLAIM = "userId";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final JwtProperties jwtProperties;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecret()));
    }

    public String generateToken(Long userId, String email, String role) {
        return generateAccessToken(userId, email, role);
    }

    public String generateAccessToken(Long userId, String email, String role) {
        return buildToken(userId, email, role, ACCESS_TOKEN_TYPE, jwtProperties.getExpiration());
    }

    public String generateRefreshToken(Long userId, String email, String role) {
        return buildToken(userId, email, role, REFRESH_TOKEN_TYPE, jwtProperties.getRefreshExpiration());
    }

    private String buildToken(Long userId, String email, String role, String tokenType, long expiryMillis) {
        return Jwts.builder()
                .subject(email)
                .claim(ROLE_CLAIM, role)
                .claim(USER_ID_CLAIM, userId)
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryMillis))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Claims extractClaimsAllowExpired(String token) {
        try {
            return extractClaims(token);
        } catch (ExpiredJwtException ex) {
            return ex.getClaims();
        }
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))
                    && !claims.getExpiration().before(new Date());
        } catch (Exception ex) {
            return false;
        }
    }

    public Authentication buildAuthentication(String token) {
        Claims claims = extractClaims(token);
        if (!ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
            throw new IllegalArgumentException("Refresh tokens cannot be used for authentication");
        }

        Long userId = ((Number) claims.get(USER_ID_CLAIM)).longValue();
        String email = claims.getSubject();
        String role = claims.get(ROLE_CLAIM, String.class);
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, email, role);
        return new UsernamePasswordAuthenticationToken(
                principal,
                token,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

    public String refreshToken(String refreshToken) {
        Claims claims = extractClaims(refreshToken);
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
        if (!REFRESH_TOKEN_TYPE.equals(tokenType)) {
            throw new IllegalArgumentException("A valid refresh token is required");
        }
        return generateAccessToken(((Number) claims.get(USER_ID_CLAIM)).longValue(), claims.getSubject(), claims.get(ROLE_CLAIM, String.class));
    }

    public long getExpirationInSeconds() {
        return jwtProperties.getExpiration() / 1000;
    }

    public long getRefreshExpirationInSeconds() {
        return jwtProperties.getRefreshExpiration() / 1000;
    }
}
