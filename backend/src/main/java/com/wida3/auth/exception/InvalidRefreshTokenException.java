package com.wida3.auth.exception;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Refresh token is invalid, expired, or has been revoked");
    }
}
