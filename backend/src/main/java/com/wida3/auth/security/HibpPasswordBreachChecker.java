package com.wida3.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Checks passwords against the Have I Been Pwned range API using k-anonymity:
 * only the first 5 chars of the SHA-1 hash are ever sent over the network.
 */
@Component
public class HibpPasswordBreachChecker implements PasswordBreachChecker {

    private static final Logger log = LoggerFactory.getLogger(HibpPasswordBreachChecker.class);
    private static final String RANGE_API = "https://api.pwnedpasswords.com/range/";

    private final RestClient restClient;

    public HibpPasswordBreachChecker() {
        this.restClient = RestClient.builder()
                .requestFactory(clientRequestFactory())
                .build();
    }

    @Override
    public boolean isBreached(String plaintextPassword) {
        try {
            String sha1Hex = sha1Hex(plaintextPassword);
            String prefix = sha1Hex.substring(0, 5);
            String suffix = sha1Hex.substring(5);

            String body = restClient.get()
                    .uri(RANGE_API + prefix)
                    .retrieve()
                    .body(String.class);

            if (body == null) {
                return false;
            }
            return body.lines().anyMatch(line -> line.startsWith(suffix));
        } catch (Exception ex) {
            log.warn("Breach-list check unavailable, allowing registration to proceed: {}", ex.getMessage());
            return false;
        }
    }

    private static String sha1Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02X", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 not available", e);
        }
    }

    private static org.springframework.http.client.ClientHttpRequestFactory clientRequestFactory() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(2).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(2).toMillis());
        return factory;
    }
}
