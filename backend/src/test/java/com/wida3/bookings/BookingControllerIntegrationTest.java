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
import java.util.UUID;
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

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(NoOpBreachCheckerConfig.class)
class BookingControllerIntegrationTest {

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
        registry.add("app.file-storage.path", () -> System.getProperty("java.io.tmpdir") + "/wida3-test-uploads");
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

    /** Creates an ACTIVE listing with the given weekly price, returning {listingId, ownerToken}. */
    private Map<String, String> createActiveListing(String ownerEmail, double weeklyPrice) {
        String ownerToken = registerAndGetToken(ownerEmail, Set.of("OWNER"));
        HttpHeaders ownerHeaders = authHeaders(ownerToken);
        Map<String, Object> listingBody = Map.of(
                "title", "Bookable warehouse",
                "city", "Tangier",
                "address", "1 Port Road",
                "warehouseType", "DRY",
                "sizeSqm", 300.0,
                "weeklyPrice", weeklyPrice,
                "photoUrls", List.of());
        ResponseEntity<Map> createResponse = restTemplate.exchange(
                url("/api/v1/listings"), HttpMethod.POST, new HttpEntity<>(listingBody, ownerHeaders), Map.class);
        String listingId = (String) createResponse.getBody().get("id");

        String adminToken = grantAdminAndGetToken("admin-" + UUID.randomUUID() + "@example.com");
        restTemplate.exchange(
                url("/api/v1/listings/" + listingId + "/approve"),
                HttpMethod.PATCH,
                new HttpEntity<>(null, authHeaders(adminToken)),
                Map.class);

        return Map.of("listingId", listingId, "ownerToken", ownerToken);
    }

    private Map<String, Object> bookingBody(String listingId, LocalDate start, LocalDate end) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("listingId", listingId);
        body.put("startDate", start.toString());
        body.put("endDate", end.toString());
        return body;
    }

    @Test
    void renter_booksAvailableWeeks_confirmedWithAccessCode() {
        Map<String, String> listing = createActiveListing("owner-book1@example.com", 1000.0);
        String renterToken = registerAndGetToken("renter-book1@example.com", Set.of());
        LocalDate start = LocalDate.now().plusDays(7);
        LocalDate end = start.plusDays(14);

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/v1/bookings"),
                HttpMethod.POST,
                new HttpEntity<>(bookingBody(listing.get("listingId"), start, end), authHeaders(renterToken)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("status")).isEqualTo("CONFIRMED");
        assertThat(response.getBody().get("accessCode")).isNotNull();
        assertThat(((Number) response.getBody().get("totalPrice")).doubleValue()).isEqualTo(2000.0);
    }

    @Test
    void renter_cannotBookOverlappingConfirmedWeeks() {
        Map<String, String> listing = createActiveListing("owner-book2@example.com", 500.0);
        String renterToken = registerAndGetToken("renter-book2@example.com", Set.of());
        LocalDate start = LocalDate.now().plusDays(30);
        LocalDate end = start.plusDays(7);

        ResponseEntity<Map> first = restTemplate.exchange(
                url("/api/v1/bookings"),
                HttpMethod.POST,
                new HttpEntity<>(bookingBody(listing.get("listingId"), start, end), authHeaders(renterToken)),
                Map.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String secondRenterToken = registerAndGetToken("renter-book3@example.com", Set.of());
        ResponseEntity<Map> second = restTemplate.exchange(
                url("/api/v1/bookings"),
                HttpMethod.POST,
                new HttpEntity<>(bookingBody(listing.get("listingId"), start.plusDays(3), end.plusDays(3)), authHeaders(secondRenterToken)),
                Map.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void endDateBeforeStartDate_returnsBadRequest() {
        Map<String, String> listing = createActiveListing("owner-book4@example.com", 500.0);
        String renterToken = registerAndGetToken("renter-book4@example.com", Set.of());
        LocalDate start = LocalDate.now().plusDays(7);

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/v1/bookings"),
                HttpMethod.POST,
                new HttpEntity<>(bookingBody(listing.get("listingId"), start, start.minusDays(1)), authHeaders(renterToken)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void nonWeekMultipleDateRange_returnsBadRequest() {
        Map<String, String> listing = createActiveListing("owner-book5@example.com", 500.0);
        String renterToken = registerAndGetToken("renter-book5@example.com", Set.of());
        LocalDate start = LocalDate.now().plusDays(7);

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/v1/bookings"),
                HttpMethod.POST,
                new HttpEntity<>(bookingBody(listing.get("listingId"), start, start.plusDays(10)), authHeaders(renterToken)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void bookingDeactivatedListing_returnsConflict() {
        String ownerToken = registerAndGetToken("owner-book6@example.com", Set.of("OWNER"));
        Map<String, Object> listingBody = Map.of(
                "title", "Never approved warehouse",
                "city", "Fes",
                "address", "2 Old Town",
                "warehouseType", "DRY",
                "sizeSqm", 100.0,
                "weeklyPrice", 200.0,
                "photoUrls", List.of());
        ResponseEntity<Map> createResponse = restTemplate.exchange(
                url("/api/v1/listings"), HttpMethod.POST, new HttpEntity<>(listingBody, authHeaders(ownerToken)), Map.class);
        String listingId = (String) createResponse.getBody().get("id");

        String renterToken = registerAndGetToken("renter-book6@example.com", Set.of());
        LocalDate start = LocalDate.now().plusDays(7);

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/v1/bookings"),
                HttpMethod.POST,
                new HttpEntity<>(bookingBody(listingId, start, start.plusDays(7)), authHeaders(renterToken)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void repeatingIdempotencyKey_returnsOriginalBookingWithoutDoubleCharging() {
        Map<String, String> listing = createActiveListing("owner-book7@example.com", 700.0);
        String renterToken = registerAndGetToken("renter-book7@example.com", Set.of());
        LocalDate start = LocalDate.now().plusDays(60);
        LocalDate end = start.plusDays(7);

        HttpHeaders headers = authHeaders(renterToken);
        headers.set("Idempotency-Key", "test-key-book7");

        ResponseEntity<Map> first = restTemplate.exchange(
                url("/api/v1/bookings"), HttpMethod.POST, new HttpEntity<>(bookingBody(listing.get("listingId"), start, end), headers), Map.class);
        ResponseEntity<Map> second = restTemplate.exchange(
                url("/api/v1/bookings"), HttpMethod.POST, new HttpEntity<>(bookingBody(listing.get("listingId"), start, end), headers), Map.class);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getBody().get("id")).isEqualTo(first.getBody().get("id"));
    }

    @Test
    void renterAndOwner_canViewBooking_strangerCannot() {
        Map<String, String> listing = createActiveListing("owner-book8@example.com", 400.0);
        String renterToken = registerAndGetToken("renter-book8@example.com", Set.of());
        LocalDate start = LocalDate.now().plusDays(90);

        ResponseEntity<Map> created = restTemplate.exchange(
                url("/api/v1/bookings"),
                HttpMethod.POST,
                new HttpEntity<>(bookingBody(listing.get("listingId"), start, start.plusDays(7)), authHeaders(renterToken)),
                Map.class);
        String bookingId = (String) created.getBody().get("id");

        ResponseEntity<Map> byRenter = restTemplate.exchange(
                url("/api/v1/bookings/" + bookingId), HttpMethod.GET, new HttpEntity<>(authHeaders(renterToken)), Map.class);
        assertThat(byRenter.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> byOwner = restTemplate.exchange(
                url("/api/v1/bookings/" + bookingId), HttpMethod.GET, new HttpEntity<>(authHeaders(listing.get("ownerToken"))), Map.class);
        assertThat(byOwner.getStatusCode()).isEqualTo(HttpStatus.OK);

        String strangerToken = registerAndGetToken("stranger-book8@example.com", Set.of());
        ResponseEntity<Map> byStranger = restTemplate.exchange(
                url("/api/v1/bookings/" + bookingId), HttpMethod.GET, new HttpEntity<>(authHeaders(strangerToken)), Map.class);
        assertThat(byStranger.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void unauthenticated_cannotCreateBooking() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                url("/api/v1/bookings"),
                bookingBody(UUID.randomUUID().toString(), LocalDate.now().plusDays(7), LocalDate.now().plusDays(14)),
                Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
