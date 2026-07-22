package com.wida3.auth.controller;

import com.wida3.auth.dto.AuthResponse;
import com.wida3.auth.dto.LoginRequest;
import com.wida3.auth.dto.RegisterRequest;
import com.wida3.auth.security.AuthRateLimiter;
import com.wida3.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    public AuthController(AuthService authService, AuthRateLimiter rateLimiter) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        checkRateLimit(httpRequest);
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        checkRateLimit(httpRequest);
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    private void checkRateLimit(HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        if (!rateLimiter.tryConsume(clientIp)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS, "Too many requests, try again later");
        }
    }
}
