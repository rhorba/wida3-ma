package com.wida3.auth;

import com.wida3.auth.security.PasswordBreachChecker;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Integration tests must not depend on live network access to the HIBP API.
 * The breach-check logic itself belongs to a focused unit test, not this suite.
 */
@TestConfiguration
public class NoOpBreachCheckerConfig {

    @Bean
    @Primary
    public PasswordBreachChecker passwordBreachChecker() {
        return plaintextPassword -> false;
    }
}
