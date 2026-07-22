package com.wida3.auth.security;

public interface PasswordBreachChecker {

    /**
     * @return true if the password appears in a known breach corpus.
     */
    boolean isBreached(String plaintextPassword);
}
