package com.wida3.auth.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Per-IP token bucket for /api/v1/auth/* endpoints.
 * In-memory is sufficient for the single-instance MVP deployment (Architecture: single VPS).
 */
@Component
public class AuthRateLimiter {

    private final int capacity;
    private final Duration refillPeriod;
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public AuthRateLimiter(
            @Value("${app.auth.rate-limit.capacity:10}") int capacity,
            @Value("${app.auth.rate-limit.refill-period-seconds:60}") long refillPeriodSeconds) {
        this.capacity = capacity;
        this.refillPeriod = Duration.ofSeconds(refillPeriodSeconds);
    }

    public boolean tryConsume(String clientIp) {
        Bucket bucket = buckets.computeIfAbsent(clientIp, ip -> newBucket());
        return bucket.tryConsume(1);
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(capacity, io.github.bucket4j.Refill.greedy(capacity, refillPeriod));
        return Bucket.builder().addLimit(limit).build();
    }
}
