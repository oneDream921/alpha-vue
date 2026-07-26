package io.github.onedream921.alphavue.modules.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LocalFileHttpIntegrationTests {

    private static final byte[] PNG = new byte[]{
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
            0x00, 0x00, 0x00, 0x00
    };

    @TempDir
    static Path uploadRoot;

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void fileProperties(DynamicPropertyRegistry registry) {
        registry.add("alpha.file.provider", () -> "local");
        registry.add("alpha.file.local-root", uploadRoot::toString);
        registry.add("alpha.file.local-public-url", () -> "/uploads");
    }

    @Test
    void servesLocalPngOverRealHttpWithoutAuthentication() throws Exception {
        Path image = uploadRoot.resolve("preview.png");
        Files.write(image, PNG);

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + port + "/uploads/preview.png"))
                .GET()
                .build();
        HttpResponse<byte[]> response = HttpClient.newHttpClient().send(
                request, HttpResponse.BodyHandlers.ofByteArray());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type"))
                .contains(MediaType.IMAGE_PNG_VALUE);
        assertThat(response.body()).isEqualTo(PNG);
    }
}
