package com.wida3.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.wida3.auth.security.JwtService;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private final JwtService jwtService =
            new JwtService("test-secret-key-at-least-32-bytes-long!!", 15);

    @Test
    void issueThenExtract_roundTripsSubject() {
        String token = jwtService.issueAccessToken("alice@example.com", Set.of("RENTER"));

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractSubject(token)).isEqualTo("alice@example.com");
    }

    @Test
    void differentSubjects_produceDifferentTokens() {
        String token1 = jwtService.issueAccessToken("alice@example.com", Set.of("RENTER"));
        String token2 = jwtService.issueAccessToken("bob@example.com", Set.of("OWNER"));

        assertThat(token1).isNotEqualTo(token2);
        assertThat(jwtService.extractSubject(token1)).isEqualTo("alice@example.com");
        assertThat(jwtService.extractSubject(token2)).isEqualTo("bob@example.com");
    }
}
