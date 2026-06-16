package com.slmora.patreonpostautodownloader.service;

import com.slmora.patreonpostautodownloader.config.PipelineConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class UrlExecutionServiceTest {

    private final UrlExecutionService urlExecutionService = new UrlExecutionService();
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void GivenNullOrBlankUrl_WhenExecuteUrl_ThenReturnsEmptyOptional(String inputUrl) {
        // Act
        var result = urlExecutionService.executeUrl(inputUrl, 1);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void GivenInvalidUrlFormat_WhenExecuteUrl_ThenReturnsEmptyOptional() {
        // Act
        var result = urlExecutionService.executeUrl("ht@tp:// bad-url", 7);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void GivenValidPatreonPayload_WhenExecuteUrl_ThenParsesPostAndNextUrl() throws Exception {
        // Arrange
        server = startServer();
        addHandler("/ok", 200, "application/json", """
                {
                  "data": [
                    {
                      "id": "123",
                      "attributes": {
                        "published_at": "2026-06-16T10:15:30+00:00",
                        "title": "Post title",
                        "cleaned_teaser_text": "Teaser",
                        "content_json_string": "{\\"type\\":\\"paragraph\\",\\"text\\":\\"hello\\"}",
                        "comment_count": 5,
                        "url": "https://www.patreon.com/posts/123"
                      }
                    }
                  ],
                  "included": [
                    {
                      "type": "media",
                      "attributes": {
                        "image_urls": {
                          "default_large": "https://cdn.example.com/p/post/123/large.jpg",
                          "default": "https://cdn.example.com/p/post/123/thumb.jpg"
                        }
                      }
                    }
                  ],
                  "links": {
                    "next": "https://api.patreon.com/next-page"
                  }
                }
                """.getBytes(StandardCharsets.UTF_8));

        try (MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class)) {
            pipelineConfigMock.when(PipelineConfig::getPatreonAccessCookie).thenReturn("session=abc");

            // Act
            var result = urlExecutionService.executeUrl(urlOf("/ok"), 1);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getNextUrl()).isEqualTo("https://api.patreon.com/next-page");
            assertThat(result.get().getPostRecordList()).hasSize(1);
            assertThat(result.get().getPostRecordList().getFirst().getId()).isEqualTo("123");
            assertThat(result.get().getPostRecordList().getFirst().getLargeUrl()).contains("/p/post/123/large.jpg");
            assertThat(result.get().getPostRecordList().getFirst().getThumbUrl()).contains("/p/post/123/thumb.jpg");
        }
    }

    @Test
    void GivenHttpNon200_WhenExecuteUrl_ThenReturnsEmptyOptional() throws Exception {
        // Arrange
        server = startServer();
        addHandler("/status-500", 500, "application/json", "{}".getBytes(StandardCharsets.UTF_8));

        try (MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class)) {
            pipelineConfigMock.when(PipelineConfig::getPatreonAccessCookie).thenReturn("session=abc");

            // Act
            var result = urlExecutionService.executeUrl(urlOf("/status-500"), 2);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Test
    void GivenPayloadWithoutDataArray_WhenExecuteUrl_ThenReturnsEmptyOptional() throws Exception {
        // Arrange
        server = startServer();
        addHandler("/bad-json", 200, "application/json", "{\"data\":{},\"included\":[],\"links\":{\"next\":null}}".getBytes(StandardCharsets.UTF_8));

        try (MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class)) {
            pipelineConfigMock.when(PipelineConfig::getPatreonAccessCookie).thenReturn("session=abc");

            // Act
            var result = urlExecutionService.executeUrl(urlOf("/bad-json"), 3);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    private HttpServer startServer() throws IOException {
        HttpServer localServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        localServer.start();
        return localServer;
    }

    private void addHandler(String path, int status, String contentType, byte[] body) {
        server.createContext(path, new FixedResponseHandler(status, contentType, body));
    }

    private String urlOf(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    private record FixedResponseHandler(int status, String contentType, byte[] body) implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        }
    }
}

