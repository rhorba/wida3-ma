package com.wida3.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.wida3.auth.dto.LoginRequest;
import com.wida3.auth.dto.RegisterRequest;
import java.util.List;
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
class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("db_user", postgres::getUsername);
        registry.add("db_password", postgres::getPassword);
        registry.add("DB_USER", postgres::getUsername);
        registry.add("DB_PASSWORD", postgres::getPassword);
        registry.add("app.jwt.secret", () -> "test-secret-key-at-least-32-bytes-long!!");
        registry.add("app.jwt.access-token-ttl-min", () -> 15);
        registry.add("app.jwt.refresh-token-ttl-days", () -> 7);
        registry.add("app.auth.rate-limit.capacity", () -> 1000);
        registry.add("app.jwt.refresh-cookie-secure", () -> false);
    }

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void registerThenLogin_succeeds() {
        RegisterRequest register = new RegisterRequest("alice@example.com", "correcthorsebattery", "Alice Renter", null);
        ResponseEntity<Object> registerResponse = restTemplate.postForEntity(url("/api/v1/auth/register"), register, Object.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        LoginRequest login = new LoginRequest("alice@example.com", "correcthorsebattery");
        ResponseEntity<Object> loginResponse = restTemplate.postForEntity(url("/api/v1/auth/login"), login, Object.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void register_duplicateEmail_returnsConflict() {
        RegisterRequest register = new RegisterRequest("bob@example.com", "correcthorsebattery", "Bob Renter", null);
        restTemplate.postForEntity(url("/api/v1/auth/register"), register, Object.class);

        ResponseEntity<Object> secondAttempt = restTemplate.postForEntity(url("/api/v1/auth/register"), register, Object.class);
        assertThat(secondAttempt.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void register_shortPassword_returnsBadRequest() {
        RegisterRequest register = new RegisterRequest("carol@example.com", "short", "Carol Renter", null);
        ResponseEntity<Object> response = restTemplate.postForEntity(url("/api/v1/auth/register"), register, Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void login_wrongPassword_returnsUnauthorized() {
        RegisterRequest register = new RegisterRequest("dave@example.com", "correcthorsebattery", "Dave Renter", null);
        restTemplate.postForEntity(url("/api/v1/auth/register"), register, Object.class);

        LoginRequest badLogin = new LoginRequest("dave@example.com", "wrongpassword");
        ResponseEntity<Object> response = restTemplate.postForEntity(url("/api/v1/auth/login"), badLogin, Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_afterFiveFailedAttempts_locksAccount() {
        RegisterRequest register = new RegisterRequest("erin@example.com", "correcthorsebattery", "Erin Renter", null);
        restTemplate.postForEntity(url("/api/v1/auth/register"), register, Object.class);

        LoginRequest badLogin = new LoginRequest("erin@example.com", "wrongpassword");
        for (int i = 0; i < 5; i++) {
            restTemplate.postForEntity(url("/api/v1/auth/login"), badLogin, Object.class);
        }

        LoginRequest correctLogin = new LoginRequest("erin@example.com", "correcthorsebattery");
        ResponseEntity<Object> response = restTemplate.postForEntity(url("/api/v1/auth/login"), correctLogin, Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
    }

    @Test
    void refresh_withValidCookie_rotatesTokenAndReturnsNewAccessToken() {
        RegisterRequest register = new RegisterRequest("frank@example.com", "correcthorsebattery", "Frank Renter", null);
        ResponseEntity<Object> registerResponse = restTemplate.postForEntity(url("/api/v1/auth/register"), register, Object.class);
        String refreshCookie = extractCookie(registerResponse);

        ResponseEntity<Object> refreshResponse = postWithCookie("/api/v1/auth/refresh", refreshCookie);
        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(extractCookie(refreshResponse)).isNotEqualTo(refreshCookie);
    }

    @Test
    void refresh_withoutCookie_returnsUnauthorized() {
        ResponseEntity<Object> response = restTemplate.postForEntity(url("/api/v1/auth/refresh"), null, Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refresh_reusingRotatedToken_returnsUnauthorized() {
        RegisterRequest register = new RegisterRequest("grace@example.com", "correcthorsebattery", "Grace Renter", null);
        ResponseEntity<Object> registerResponse = restTemplate.postForEntity(url("/api/v1/auth/register"), register, Object.class);
        String originalCookie = extractCookie(registerResponse);

        postWithCookie("/api/v1/auth/refresh", originalCookie);

        ResponseEntity<Object> reuseResponse = postWithCookie("/api/v1/auth/refresh", originalCookie);
        assertThat(reuseResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logout_thenRefresh_returnsUnauthorized() {
        RegisterRequest register = new RegisterRequest("heidi@example.com", "correcthorsebattery", "Heidi Renter", null);
        ResponseEntity<Object> registerResponse = restTemplate.postForEntity(url("/api/v1/auth/register"), register, Object.class);
        String refreshCookie = extractCookie(registerResponse);

        ResponseEntity<Object> logoutResponse = postWithCookie("/api/v1/auth/logout", refreshCookie);
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Object> refreshResponse = postWithCookie("/api/v1/auth/refresh", refreshCookie);
        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void wrongHttpMethod_returnsMethodNotAllowedNotServerError() {
        ResponseEntity<Object> response = restTemplate.getForEntity(url("/api/v1/auth/login"), Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void unknownPathUnderPermitAllPrefix_returnsNotFoundNotServerError() {
        ResponseEntity<Object> response = restTemplate.getForEntity(url("/api/v1/auth/does-not-exist"), Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void protectedPath_withoutToken_returnsUnauthorizedNotForbidden() {
        ResponseEntity<Object> response = restTemplate.getForEntity(url("/api/v1/some-protected-thing"), Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedPath_withValidToken_passesSecurityLayer() {
        RegisterRequest register = new RegisterRequest("ivan@example.com", "correcthorsebattery", "Ivan Renter", null);
        ResponseEntity<java.util.Map> registerResponse =
                restTemplate.postForEntity(url("/api/v1/auth/register"), register, java.util.Map.class);
        String token = (String) registerResponse.getBody().get("accessToken");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<Object> response = restTemplate.exchange(
                url("/api/v1/some-protected-thing"), HttpMethod.GET, new HttpEntity<>(headers), Object.class);
        // Passes the security layer (authenticated) — 404 because no such controller exists, not 401/403.
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<Object> postWithCookie(String path, String cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);
        return restTemplate.exchange(url(path), HttpMethod.POST, new HttpEntity<>(headers), Object.class);
    }

    private String extractCookie(ResponseEntity<?> response) {
        List<String> setCookieHeaders = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeaders).isNotNull().isNotEmpty();
        String setCookie = setCookieHeaders.get(0);
        return setCookie.substring(0, setCookie.indexOf(';'));
    }
}
