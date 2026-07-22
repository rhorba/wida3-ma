package com.wida3.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.wida3.auth.security.AuthRateLimiter;
import org.junit.jupiter.api.Test;

class AuthRateLimiterTest {

    @Test
    void allowsUpToCapacity_thenRejects() {
        AuthRateLimiter limiter = new AuthRateLimiter(3, 60);

        assertThat(limiter.tryConsume("1.2.3.4")).isTrue();
        assertThat(limiter.tryConsume("1.2.3.4")).isTrue();
        assertThat(limiter.tryConsume("1.2.3.4")).isTrue();
        assertThat(limiter.tryConsume("1.2.3.4")).isFalse();
    }

    @Test
    void tracksEachIpIndependently() {
        AuthRateLimiter limiter = new AuthRateLimiter(1, 60);

        assertThat(limiter.tryConsume("1.1.1.1")).isTrue();
        assertThat(limiter.tryConsume("1.1.1.1")).isFalse();
        assertThat(limiter.tryConsume("2.2.2.2")).isTrue();
    }
}
