package com.wida3.auth.controller;

import com.wida3.auth.dto.AuthResponse;
import com.wida3.auth.dto.LoginRequest;
import com.wida3.auth.dto.RegisterRequest;
import com.wida3.auth.dto.TokenPair;
import com.wida3.auth.exception.InvalidRefreshTokenException;
import com.wida3.auth.security.AuthRateLimiter;
import com.wida3.auth.security.RefreshTokenService;
import com.wida3.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthRateLimiter rateLimiter;
    private final RefreshTokenService refreshTokenService;
    private final boolean refreshCookieSecure;

    public AuthController(
            AuthService authService,
            AuthRateLimiter rateLimiter,
            RefreshTokenService refreshTokenService,
            @Value("${app.jwt.refresh-cookie-secure}") boolean refreshCookieSecure) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
        this.refreshTokenService = refreshTokenService;
        this.refreshCookieSecure = refreshCookieSecure;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        checkRateLimit(httpRequest);
        TokenPair tokenPair = authService.register(request);
        return withRefreshCookie(ResponseEntity.status(HttpStatus.CREATED), tokenPair);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        checkRateLimit(httpRequest);
        TokenPair tokenPair = authService.login(request);
        return withRefreshCookie(ResponseEntity.ok(), tokenPair);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest httpRequest) {
        checkRateLimit(httpRequest);
        String rawRefreshToken = extractRefreshCookie(httpRequest)
                .orElseThrow(InvalidRefreshTokenException::new);
        TokenPair tokenPair = authService.refresh(rawRefreshToken);
        return withRefreshCookie(ResponseEntity.ok(), tokenPair);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        extractRefreshCookie(httpRequest).ifPresent(authService::logout);
        ResponseCookie expired = ResponseCookie.from(RefreshTokenService.COOKIE_NAME, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(0)
                .build();
        return ResponseEntity.noContent().header("Set-Cookie", expired.toString()).build();
    }

    private ResponseEntity<AuthResponse> withRefreshCookie(
            ResponseEntity.BodyBuilder builder, TokenPair tokenPair) {
        ResponseCookie cookie = ResponseCookie.from(RefreshTokenService.COOKIE_NAME, tokenPair.rawRefreshToken())
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(refreshTokenService.ttlSeconds())
                .build();
        return builder.header("Set-Cookie", cookie.toString()).body(tokenPair.response());
    }

    private java.util.Optional<String> extractRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return java.util.Optional.empty();
        }
        for (Cookie cookie : request.getCookies()) {
            if (RefreshTokenService.COOKIE_NAME.equals(cookie.getName())) {
                return java.util.Optional.of(cookie.getValue());
            }
        }
        return java.util.Optional.empty();
    }

    private void checkRateLimit(HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        if (!rateLimiter.tryConsume(clientIp)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS, "Too many requests, try again later");
        }
    }
}
