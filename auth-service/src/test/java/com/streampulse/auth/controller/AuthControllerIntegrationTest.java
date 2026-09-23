package com.streampulse.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.streampulse.auth.dto.LoginRequest;
import com.streampulse.auth.dto.RefreshRequest;
import com.streampulse.auth.dto.RegisterTenantRequest;
import com.streampulse.auth.dto.TokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Full-stack integration test against a real Postgres via Testcontainers, proving
 * register -> login -> refresh (with rotation) end to end. Requires a Docker daemon; not
 * executed in environments without one (see repo README for the honest status of this run).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("authdb")
            .withUsername("streampulse")
            .withPassword("streampulse");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void registerLoginAndRotateRefreshToken() {
        RegisterTenantRequest register = new RegisterTenantRequest("Acme Corp", "acme-" + System.nanoTime(), "admin@acme.io", "password123");
        ResponseEntity<TokenResponse> registerResponse = restTemplate.postForEntity(url("/api/v1/auth/register-tenant"), register, TokenResponse.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResponse.getBody()).isNotNull();

        LoginRequest login = new LoginRequest("admin@acme.io", "password123", register.tenantSlug());
        ResponseEntity<TokenResponse> loginResponse = restTemplate.postForEntity(url("/api/v1/auth/login"), login, TokenResponse.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String originalRefreshToken = loginResponse.getBody().refreshToken();

        RefreshRequest refresh = new RefreshRequest(originalRefreshToken);
        ResponseEntity<TokenResponse> refreshResponse = restTemplate.postForEntity(url("/api/v1/auth/refresh"), refresh, TokenResponse.class);
        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResponse.getBody().refreshToken()).isNotEqualTo(originalRefreshToken);

        ResponseEntity<TokenResponse> reuseResponse = restTemplate.postForEntity(url("/api/v1/auth/refresh"), refresh, TokenResponse.class);
        assertThat(reuseResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
