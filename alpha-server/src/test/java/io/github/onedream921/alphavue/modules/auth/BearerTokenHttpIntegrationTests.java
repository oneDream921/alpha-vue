package io.github.onedream921.alphavue.modules.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BearerTokenHttpIntegrationTests {

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acceptsBearerTokenOverRealHttp() throws Exception {
        HttpRequest loginRequest = HttpRequest.newBuilder(uri("/api/auth/login"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {"username":"admin","password":"admin123"}
                        """))
                .build();

        HttpResponse<String> loginResponse = httpClient.send(
                loginRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(loginResponse.statusCode()).isEqualTo(200);
        assertThat(loginResponse.headers().firstValue("Set-Cookie")).isEmpty();

        JsonNode loginBody = objectMapper.readTree(loginResponse.body());
        String token = loginBody.path("data").path("token").asText();
        assertThat(token).isNotBlank();

        HttpRequest profileRequest = HttpRequest.newBuilder(uri("/api/auth/profile"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> profileResponse = httpClient.send(
                profileRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(profileResponse.statusCode()).isEqualTo(200);
        JsonNode profileBody = objectMapper.readTree(profileResponse.body());
        assertThat(profileBody.path("code").asInt()).isEqualTo(200);
        assertThat(profileBody.path("data").path("username").asText()).isEqualTo("admin");
        assertThat(profileBody.path("data").path("roles").toString()).contains("SUPER_ADMIN");
        assertThat(profileBody.path("data").path("permissions").toString()).contains("*");
    }

    @Test
    void protectsNonHealthActuatorEndpointsOverRealHttp() throws Exception {
        HttpResponse<String> anonymousMetrics = httpClient.send(
                HttpRequest.newBuilder(uri("/actuator/prometheus")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(anonymousMetrics.statusCode()).isEqualTo(401);

        HttpResponse<String> health = httpClient.send(
                HttpRequest.newBuilder(uri("/actuator/health")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(health.statusCode()).isEqualTo(200);

        HttpRequest loginRequest = HttpRequest.newBuilder(uri("/api/auth/login"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .build();
        HttpResponse<String> loginResponse = httpClient.send(
                loginRequest, HttpResponse.BodyHandlers.ofString());
        String token = objectMapper.readTree(loginResponse.body()).path("data").path("token").asText();
        HttpResponse<String> authenticatedMetrics = httpClient.send(
                HttpRequest.newBuilder(uri("/actuator/prometheus"))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(authenticatedMetrics.statusCode()).isEqualTo(200);
        assertThat(authenticatedMetrics.body()).contains("hikaricp_connections_active");
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }
}
