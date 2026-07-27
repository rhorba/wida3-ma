package com.wida3.listings;

import static org.assertj.core.api.Assertions.assertThat;

import com.wida3.auth.NoOpBreachCheckerConfig;
import com.wida3.auth.dto.RegisterRequest;
import com.wida3.auth.entity.Role;
import com.wida3.auth.entity.User;
import com.wida3.auth.repository.RoleRepository;
import com.wida3.auth.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(NoOpBreachCheckerConfig.class)
class ListingControllerIntegrationTest {

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
        RegisterRequest register = new RegisterRequest(email, "correcthorsebattery", "Test User", null, roles);
        ResponseEntity<Map> response = restTemplate.postForEntity(url("/api/v1/auth/register"), register, Map.class);
        return (String) response.getBody().get("accessToken");
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private Map<String, Object> validListingBody(List<String> photoUrls) {
        return Map.of(
                "title", "Spacious dry warehouse",
                "city", "Casablanca",
                "address", "123 Zone Industrielle",
                "warehouseType", "DRY",
                "sizeSqm", 250.5,
                "weeklyPrice", 1200.0,
                "photoUrls", photoUrls);
    }

    @Test
    void owner_createsListing_savedAsPendingApproval() {
        String token = registerAndGetToken("owner1@example.com", Set.of("OWNER"));
        HttpHeaders headers = authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/v1/listings"),
                HttpMethod.POST,
                new HttpEntity<>(validListingBody(List.of()), headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("status")).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void renterOnly_cannotCreateListing() {
        String token = registerAndGetToken("renter1@example.com", Set.of());
        HttpHeaders headers = authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/v1/listings"),
                HttpMethod.POST,
                new HttpEntity<>(validListingBody(List.of()), headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void unauthenticated_cannotCreateListing() {
        ResponseEntity<Map> response =
                restTemplate.postForEntity(url("/api/v1/listings"), validListingBody(List.of()), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void invalidWarehouseType_returnsBadRequest() {
        String token = registerAndGetToken("owner2@example.com", Set.of("OWNER"));
        HttpHeaders headers = authHeaders(token);

        Map<String, Object> body = new java.util.HashMap<>(validListingBody(List.of()));
        body.put("warehouseType", "NOT_A_REAL_TYPE");

        ResponseEntity<Map> response =
                restTemplate.exchange(url("/api/v1/listings"), HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void tooManyPhotoUrls_returnsBadRequest() {
        String token = registerAndGetToken("owner3@example.com", Set.of("OWNER"));
        HttpHeaders headers = authHeaders(token);

        List<String> elevenUrls = java.util.stream.IntStream.range(0, 11)
                .mapToObj(i -> "/uploads/photo" + i + ".jpg")
                .toList();

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/v1/listings"), HttpMethod.POST, new HttpEntity<>(validListingBody(elevenUrls), headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void photoUrlNotFromUploadEndpoint_returnsBadRequest() {
        String token = registerAndGetToken("owner4@example.com", Set.of("OWNER"));
        HttpHeaders headers = authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/v1/listings"),
                HttpMethod.POST,
                new HttpEntity<>(validListingBody(List.of("https://evil.example.com/x.jpg")), headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void owner_uploadsPhoto_thenUsesUrlInListing() {
        String token = registerAndGetToken("owner5@example.com", Set.of("OWNER"));
        HttpHeaders headers = authHeaders(token);

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}) {
            @Override
            public String getFilename() {
                return "photo.jpg";
            }
        });
        HttpHeaders uploadHeaders = authHeaders(token);
        uploadHeaders.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<Map> uploadResponse = restTemplate.exchange(
                url("/api/v1/files/upload"), HttpMethod.POST, new HttpEntity<>(form, uploadHeaders), Map.class);

        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String photoUrl = (String) uploadResponse.getBody().get("url");
        assertThat(photoUrl).startsWith("/uploads/");

        ResponseEntity<byte[]> servedFile = restTemplate.getForEntity(url(photoUrl), byte[].class);
        assertThat(servedFile.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(servedFile.getBody()).isEqualTo(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

        ResponseEntity<Map> listingResponse = restTemplate.exchange(
                url("/api/v1/listings"),
                HttpMethod.POST,
                new HttpEntity<>(validListingBody(List.of(photoUrl)), headers),
                Map.class);
        assertThat(listingResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        @SuppressWarnings("unchecked")
        List<String> photoUrls = (List<String>) listingResponse.getBody().get("photoUrls");
        assertThat(photoUrls).containsExactly(photoUrl);
    }

    @Test
    void upload_wrongContentType_returnsBadRequest() {
        String token = registerAndGetToken("owner6@example.com", Set.of("OWNER"));

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource("not an image".getBytes()) {
            @Override
            public String getFilename() {
                return "notes.txt";
            }
        });
        HttpHeaders headers = authHeaders(token);
        headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/v1/files/upload"), HttpMethod.POST, new HttpEntity<>(form, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void upload_fileTooLarge_returnsPayloadTooLarge() {
        String token = registerAndGetToken("owner7@example.com", Set.of("OWNER"));

        byte[] oversized = new byte[6 * 1024 * 1024]; // exceeds app-level 5MB limit
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(oversized) {
            @Override
            public String getFilename() {
                return "big.jpg";
            }
        });
        HttpHeaders headers = authHeaders(token);
        headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/v1/files/upload"), HttpMethod.POST, new HttpEntity<>(form, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @Test
    void search_unauthenticated_findsOnlyActiveListingsMatchingFilters() {
        // Unique city per test: the Testcontainers Postgres instance is shared (not rolled back)
        // across every test method in this class, so search assertions must not depend on the
        // absence of listings created by unrelated tests using the shared default city.
        String city = "SearchCityA-" + java.util.UUID.randomUUID();
        String ownerToken = registerAndGetToken("owner8@example.com", Set.of("OWNER"));
        HttpHeaders ownerHeaders = authHeaders(ownerToken);

        Map<String, Object> pendingBody = new java.util.HashMap<>(validListingBody(List.of()));
        pendingBody.put("city", city);
        ResponseEntity<Map> pendingResponse = restTemplate.exchange(
                url("/api/v1/listings"), HttpMethod.POST, new HttpEntity<>(pendingBody, ownerHeaders), Map.class);
        Object pendingId = pendingResponse.getBody().get("id");

        activateListing((String) pendingId);

        ResponseEntity<List> matching = restTemplate.getForEntity(
                url("/api/v1/listings/search?city=" + city + "&warehouseType=DRY"), List.class);
        assertThat(matching.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(matching.getBody()).hasSize(1);

        ResponseEntity<List> nonMatchingCity = restTemplate.getForEntity(
                url("/api/v1/listings/search?city=" + city + "-nonexistent"), List.class);
        assertThat(nonMatchingCity.getBody()).isEmpty();
    }

    @Test
    void search_excludesPendingApprovalListings() {
        String city = "SearchCityB-" + java.util.UUID.randomUUID();
        String token = registerAndGetToken("owner9@example.com", Set.of("OWNER"));
        HttpHeaders headers = authHeaders(token);
        Map<String, Object> body = new java.util.HashMap<>(validListingBody(List.of()));
        body.put("city", city);
        restTemplate.exchange(url("/api/v1/listings"), HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

        ResponseEntity<List> response =
                restTemplate.getForEntity(url("/api/v1/listings/search?city=" + city), List.class);
        assertThat(response.getBody()).isEmpty();
    }

    private void activateListing(String listingId) {
        String adminToken = grantAdminAndGetToken("admin-" + listingId + "@example.com");
        HttpHeaders adminHeaders = authHeaders(adminToken);
        restTemplate.exchange(
                url("/api/v1/listings/" + listingId + "/approve"),
                HttpMethod.PATCH,
                new HttpEntity<>(null, adminHeaders),
                Map.class);
    }

    /**
     * ADMIN is not self-assignable at registration (AuthService.SELF_ASSIGNABLE_ROLES), mirroring
     * production where admin accounts are provisioned out-of-band. Tests grant it directly via the
     * repositories, then re-login to get a token carrying the new role claim.
     */
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

    private String createPendingListing(String ownerEmail) {
        String ownerToken = registerAndGetToken(ownerEmail, Set.of("OWNER"));
        HttpHeaders ownerHeaders = authHeaders(ownerToken);
        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/v1/listings"), HttpMethod.POST, new HttpEntity<>(validListingBody(List.of()), ownerHeaders), Map.class);
        return (String) response.getBody().get("id");
    }

    @Test
    void admin_approvesPendingListing_becomesActive() {
        String listingId = createPendingListing("owner10@example.com");
        String adminToken = grantAdminAndGetToken("admin10@example.com");

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/v1/listings/" + listingId + "/approve"),
                HttpMethod.PATCH,
                new HttpEntity<>(null, authHeaders(adminToken)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("ACTIVE");
    }

    @Test
    void admin_rejectsPendingListing_becomesRejectedWithReason() {
        String listingId = createPendingListing("owner11@example.com");
        String adminToken = grantAdminAndGetToken("admin11@example.com");

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/v1/listings/" + listingId + "/reject"),
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("reason", "Photos too blurry to verify the space"), authHeaders(adminToken)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("REJECTED");
        assertThat(response.getBody().get("rejectionReason")).isEqualTo("Photos too blurry to verify the space");
    }

    @Test
    void reject_blankReason_returnsBadRequest() {
        String listingId = createPendingListing("owner12@example.com");
        String adminToken = grantAdminAndGetToken("admin12@example.com");

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/v1/listings/" + listingId + "/reject"),
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("reason", ""), authHeaders(adminToken)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void nonAdmin_cannotApproveListing_returnsForbidden() {
        String listingId = createPendingListing("owner13@example.com");
        String ownerToken = registerAndGetToken("owner14@example.com", Set.of("OWNER"));

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/v1/listings/" + listingId + "/approve"),
                HttpMethod.PATCH,
                new HttpEntity<>(null, authHeaders(ownerToken)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void approvingAlreadyActiveListing_returnsConflict() {
        String listingId = createPendingListing("owner15@example.com");
        String adminToken = grantAdminAndGetToken("admin15@example.com");
        HttpHeaders adminHeaders = authHeaders(adminToken);

        restTemplate.exchange(
                url("/api/v1/listings/" + listingId + "/approve"), HttpMethod.PATCH, new HttpEntity<>(null, adminHeaders), Map.class);
        ResponseEntity<Map> secondApprove = restTemplate.exchange(
                url("/api/v1/listings/" + listingId + "/approve"), HttpMethod.PATCH, new HttpEntity<>(null, adminHeaders), Map.class);

        assertThat(secondApprove.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void approvingUnknownListing_returnsNotFound() {
        String adminToken = grantAdminAndGetToken("admin16@example.com");

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/v1/listings/" + java.util.UUID.randomUUID() + "/approve"),
                HttpMethod.PATCH,
                new HttpEntity<>(null, authHeaders(adminToken)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void admin_listsPendingListings_ownerCannot() {
        createPendingListing("owner17@example.com");
        String adminToken = grantAdminAndGetToken("admin17@example.com");

        ResponseEntity<List> adminResponse = restTemplate.exchange(
                url("/api/v1/listings/pending"), HttpMethod.GET, new HttpEntity<>(authHeaders(adminToken)), List.class);
        assertThat(adminResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) adminResponse.getBody()).isNotEmpty();

        String ownerToken = registerAndGetToken("owner18@example.com", Set.of("OWNER"));
        ResponseEntity<Map> ownerResponse = restTemplate.exchange(
                url("/api/v1/listings/pending"), HttpMethod.GET, new HttpEntity<>(authHeaders(ownerToken)), Map.class);
        assertThat(ownerResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void upload_asRenterOnly_returnsForbidden() {
        String token = registerAndGetToken("renter2@example.com", Set.of());

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(new byte[]{1, 2, 3}) {
            @Override
            public String getFilename() {
                return "photo.jpg";
            }
        });
        HttpHeaders headers = authHeaders(token);
        headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/v1/files/upload"), HttpMethod.POST, new HttpEntity<>(form, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
