package com.wida3.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 10) String password,
        @NotBlank String fullName,
        String phone,
        /** Optional additional roles requested at signup (e.g. "OWNER"). Always granted RENTER
         * regardless. "ADMIN" is never accepted here — admin is assigned out-of-band only. */
        Set<String> roles) {

    public RegisterRequest {
        roles = roles == null ? Set.of() : roles;
    }
}
