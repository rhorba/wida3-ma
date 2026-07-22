package com.wida3.auth.exception;

public class BreachedPasswordException extends RuntimeException {

    public BreachedPasswordException() {
        super("This password has appeared in a known data breach — choose a different one");
    }
}
