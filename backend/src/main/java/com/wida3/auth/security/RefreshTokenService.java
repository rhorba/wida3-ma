package com.wida3.auth.security;

import com.wida3.auth.entity.RefreshToken;
import com.wida3.auth.entity.User;
import com.wida3.auth.exception.InvalidRefreshTokenException;
import com.wida3.auth.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

    public static final String COOKIE_NAME = "refresh_token";

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long refreshTokenTtlDays;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.jwt.refresh-token-ttl-days}") long refreshTokenTtlDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }

    /**
     * Issues a new opaque refresh token for the user, returning the raw value to be set as a cookie.
     * Only the SHA-256 hash of the raw value is ever persisted.
     */
    public String issue(User user) {
        String rawToken = generateRawToken();
        RefreshToken entity = new RefreshToken(
                user, hash(rawToken), Instant.now().plus(refreshTokenTtlDays, ChronoUnit.DAYS));
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    /**
     * Validates the presented raw refresh token, rotates it (revokes old, issues new), and
     * returns the user + new raw token. If a revoked token is reused, treats it as a signal of
     * compromise and revokes all of that user's active refresh tokens.
     */
    @org.springframework.transaction.annotation.Transactional
    public RotationResult validateAndRotate(String rawToken) {
        String tokenHash = hash(rawToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (token.getRevokedAt() != null) {
            refreshTokenRepository.findAllByUserAndRevokedAtIsNull(token.getUser())
                    .forEach(RefreshToken::revoke);
            throw new InvalidRefreshTokenException();
        }

        if (!token.isValid()) {
            throw new InvalidRefreshTokenException();
        }

        token.revoke();
        String newRawToken = issue(token.getUser());
        return new RotationResult(token.getUser(), newRawToken);
    }

    @org.springframework.transaction.annotation.Transactional
    public void revokeAllForUser(User user) {
        refreshTokenRepository.findAllByUserAndRevokedAtIsNull(user).forEach(RefreshToken::revoke);
    }

    /** Revokes the presented token, if it exists. Idempotent — an unknown/already-revoked token is a no-op. */
    @org.springframework.transaction.annotation.Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(RefreshToken::revoke);
    }

    public long ttlSeconds() {
        return refreshTokenTtlDays * 24 * 60 * 60;
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record RotationResult(User user, String rawToken) {
    }
}
