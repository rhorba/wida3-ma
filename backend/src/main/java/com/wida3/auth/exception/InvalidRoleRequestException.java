package com.wida3.auth.exception;

public class InvalidRoleRequestException extends RuntimeException {

    public InvalidRoleRequestException(String role) {
        super("Role '" + role + "' cannot be self-assigned at registration");
    }
}
