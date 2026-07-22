package com.wida3.auth.dto;

/** Internal carrier — accessToken response body + raw refresh token for the cookie. Never serialized as-is. */
public record TokenPair(AuthResponse response, String rawRefreshToken) {
}
