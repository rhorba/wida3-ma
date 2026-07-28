package com.wida3.bookings;

import static org.assertj.core.api.Assertions.assertThat;

import com.wida3.auth.NoOpBreachCheckerConfig;
import com.wida3.auth.entity.Role;
import com.wida3.auth.entity.User;
import com.wida3.auth.repository.RoleRepository;
import com.wida3.auth.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Separate Spring context from BookingControllerIntegrationTest since the mock payment
 * outcome is fixed per-context via a property, not per-request. */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(NoOpBreachCheckerConfig.class)
class BookingPaymentFailureIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("DB_USER", postgres::getUsername);
        registry.add("DB_PASSWORD", postgres::getPassword);
        registry.add("app.jwt.secret", () -> "test-secret-key-at-least-32-bytes-long!!");
        registry.add("app.jwt.access-token-ttl-min", () -> 15);
        registry.add("app.jwt.refresh-token-ttl-days", () -> 7);
        registry.add("app.auth.rate-limit.capacity", () -> 1000);
        registry.add("app.jwt.refresh-cookie-secure", () -> false);
        registry.add("app.file-storage.path", () -> System.getProperty("java.io.tmpdir") + "/wida3-test-uploads-payfail");
        registry.add("app.payment.mock.always-succeed", () -> false);
    }

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private String registerAndGetToken(String email, Set<String> roles) {
        Map<String, Object> body = Map.of(
                "email", email, "password", "correcthorsebattery", "fullName", "Test User", "roles", roles);
        ResponseEntity<Map> response = restTemplate.postForEntity(url("/api/v1/auth/register"), body, Map.class);
        return (String) response.getBody().get("accessToken");
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private String grantAdminAndGetToken(String email) {
        registerAndGetToken(email, Set.of());
        User user = userRepository.findByEmail(email).orElseThrow();
        Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
        user.addRole(adminRole);
        userRepository.save(user);
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                url("/api/v1/auth/login"), Map.of("email", email, "password", "correcthorsebattery"), Map.class);
        return (String) loginResponse.getBody().get("accessToken");
    }

    @Test
    void payment_declines_bookingStaysCancelledWithNoAccessCode() {
        String ownerToken = registerAndGetToken("owner-payfail@example.com", Set.of("OWNER"));
        Map<String, Object> listingBody = Map.of(
                "title", "Warehouse for declined payment test",
                "city", "Marrakesh",
                "address", "1 Medina Street",
                "warehouseType", "DRY",
                "sizeSqm", 200.0,
                "weeklyPrice", 300.0,
                "photoUrls", List.of());
        ResponseEntity<Map> createResponse = restTemplate.exchange(
                url("/api/v1/listings"), HttpMethod.POST, new HttpEntity<>(listingBody, authHeaders(ownerToken)), Map.class);
        String listingId = (String) createResponse.getBody().get("id");

        String adminToken = grantAdminAndGetToken("admin-payfail@example.com");
        restTemplate.exchange(
                url("/api/v1/listings/" + listingId + "/approve"),
                HttpMethod.PATCH,
                new HttpEntity<>(null, authHeaders(adminToken)),
                Map.class);

        String renterToken = registerAndGetToken("renter-payfail@example.com", Set.of());
        LocalDate start = LocalDate.now().plusDays(7);
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("listingId", listingId);
        body.put("startDate", start.toString());
        body.put("endDate", start.plusDays(7).toString());

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/v1/bookings"), HttpMethod.POST, new HttpEntity<>(body, authHeaders(renterToken)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("status")).isEqualTo("CANCELLED");
        assertThat(response.getBody().get("accessCode")).isNull();
        assertThat(response.getBody().get("paymentFailureReason")).isNotNull();
    }
}
