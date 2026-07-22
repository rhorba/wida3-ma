package com.wida3.listings;

import static org.assertj.core.api.Assertions.assertThat;

import com.wida3.auth.NoOpBreachCheckerConfig;
import com.wida3.auth.dto.RegisterRequest;
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
