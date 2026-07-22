package com.wida3.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    private final SecretKey key;
    private final long accessTokenTtlMin;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-ttl-min}") long accessTokenTtlMin) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtlMin = accessTokenTtlMin;
    }

    public String issueAccessToken(String subjectEmail, Set<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subjectEmail)
                .claim("roles", roles)
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(now.plus(accessTokenTtlMin, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public Set<String> extractRoles(String token) {
        List<String> roles = parseClaims(token).get("roles", List.class);
        return roles == null ? Set.of() : Set.copyOf(roles);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
