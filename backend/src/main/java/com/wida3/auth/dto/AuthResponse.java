package com.wida3.auth.dto;

import java.util.Set;

public record AuthResponse(String accessToken, String email, Set<String> roles) {
}
