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

/**
 * The {@code UrlExecutionServiceTest} test class is created for verifying
 * Patreon API URL execution behavior implemented by {@link UrlExecutionService}.
 * <p>
 * It focuses on input validation, authenticated request setup through
 * {@link PipelineConfig}, successful JSON mapping into URL execution results,
 * HTTP failure handling, and invalid Patreon response shape handling.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Verifies null, empty, and blank URLs return an empty result.</li>
 *     <li>Verifies invalid URL syntax is handled without escaping to callers.</li>
 *     <li>Verifies valid Patreon-shaped JSON parses posts, media URLs, and next-page links.</li>
 *     <li>Verifies non-200 responses and invalid {@code data} shape return an empty result.</li>
 *     <li>Uses a local {@link HttpServer} fixture so tests do not call Patreon.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link UrlExecutionService}<br>
 * 2 - {@link PipelineConfig}<br>
 * 3 - {@link HttpServer}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link UrlExecutionServiceTest#tearDown()}</li>
 *     <li>{@link UrlExecutionServiceTest#GivenNullOrBlankUrl_WhenExecuteUrl_ThenReturnsEmptyOptional(String)}</li>
 *     <li>{@link UrlExecutionServiceTest#GivenInvalidUrlFormat_WhenExecuteUrl_ThenReturnsEmptyOptional()}</li>
 *     <li>{@link UrlExecutionServiceTest#GivenValidPatreonPayload_WhenExecuteUrl_ThenParsesPostAndNextUrl()}</li>
 *     <li>{@link UrlExecutionServiceTest#GivenHttpNon200_WhenExecuteUrl_ThenReturnsEmptyOptional()}</li>
 *     <li>{@link UrlExecutionServiceTest#GivenPayloadWithoutDataArray_WhenExecuteUrl_ThenReturnsEmptyOptional()}</li>
 *     <li>{@link UrlExecutionServiceTest#startServer()}</li>
 *     <li>{@link UrlExecutionServiceTest#addHandler(String, int, String, byte[])}</li>
 *     <li>{@link UrlExecutionServiceTest#urlOf(String)}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>Tests mock {@link PipelineConfig#getPatreonAccessCookie()} so no real Patreon session cookie is required.</li>
 *     <li>The local HTTP server is stopped after each test to release the dynamically assigned port.</li>
 * </ul>
 *
 * @author: SLMORA
 * @since 1.0
 *
 * <h4>Revision History</h4>
 * <blockquote><pre>
 * <br>Version      Date            Editor              Note
 * <br>-------------------------------------------------------
 * <br>1.0          6/6/2026       SLMORA              Initial Code
 * </pre></blockquote>
 */
class UrlExecutionServiceTest {

    /**
     * Service under test used for all URL execution scenarios.
     */
    private final UrlExecutionService urlExecutionService = new UrlExecutionService();

    /**
     * Local HTTP server used to return deterministic Patreon-style responses.
     */
    private HttpServer server;

    /**
     * <h3>Stop local HTTP server</h3>
     * Stops the per-test local server when one was started.
     * <p>
     * This cleanup keeps tests isolated and prevents a completed test from
     * holding on to an assigned loopback port.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Checks whether a local server was created by the current test.</li>
     *     <li>Stops the server immediately with a zero-second delay.</li>
     * </ul>
     *
     * @since 1.0
     */
    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    /**
     * <h3>Return empty for null or blank URL</h3>
     * Verifies that {@link UrlExecutionService#executeUrl(String, int)} returns
     * {@link java.util.Optional#empty()} for null, empty, and whitespace-only
     * URL input.
     * <p>
     * This test targets input validation before any HTTP request is created.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Runs the scenario for null, empty, and whitespace-only URL values.</li>
     *     <li>Invokes URL execution with a diagnostic recursive index.</li>
     *     <li>Asserts the result is empty.</li>
     * </ul>
     *
     * @param inputUrl null, empty, or blank URL input supplied by parameterized test sources
     *
     * @since 1.0
     *
     * @see UrlExecutionService#executeUrl(String, int)
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void GivenNullOrBlankUrl_WhenExecuteUrl_ThenReturnsEmptyOptional(String inputUrl) {
        // Act
        var result = urlExecutionService.executeUrl(inputUrl, 1);

        // Assert
        assertThat(result).isEmpty();
    }

    /**
     * <h3>Return empty for invalid URL syntax</h3>
     * Verifies that {@link UrlExecutionService#executeUrl(String, int)} catches
     * invalid URL format failures and returns an empty result.
     * <p>
     * This test targets the public failure contract for malformed API URL
     * values.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Invokes URL execution with a syntactically invalid URL string.</li>
     *     <li>Asserts the service returns an empty result instead of throwing to the caller.</li>
     * </ul>
     *
     * @since 1.0
     *
     * @see UrlExecutionService#executeUrl(String, int)
     */
    @Test
    void GivenInvalidUrlFormat_WhenExecuteUrl_ThenReturnsEmptyOptional() {
        // Act
        var result = urlExecutionService.executeUrl("ht@tp:// bad-url", 7);

        // Assert
        assertThat(result).isEmpty();
    }

    /**
     * <h3>Parse valid Patreon response page</h3>
     * Verifies that {@link UrlExecutionService#executeUrl(String, int)} maps a
     * successful Patreon-shaped JSON response into posts, image URLs, and the
     * next-page URL.
     * <p>
     * This test targets post attribute mapping, {@code included[]} media mapping,
     * and {@code links.next} pagination extraction.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Starts a local HTTP server returning a representative Patreon JSON page.</li>
     *     <li>Mocks the configured Patreon cookie used by the request.</li>
     *     <li>Invokes URL execution against the local server URL.</li>
     *     <li>Asserts post id, next URL, large image URL, and thumbnail URL mapping.</li>
     * </ul>
     *
     * @throws Exception when local server setup or URL execution fails
     * @since 1.0
     *
     * @see UrlExecutionService#executeUrl(String, int)
     */
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

    /**
     * <h3>Return empty for non-success HTTP response</h3>
     * Verifies that {@link UrlExecutionService#executeUrl(String, int)} returns
     * an empty result when the server responds with a non-200 status.
     * <p>
     * This test targets HTTP status validation in the request path.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Starts a local HTTP server returning {@code 500}.</li>
     *     <li>Mocks the configured Patreon cookie.</li>
     *     <li>Invokes URL execution and asserts the result is empty.</li>
     * </ul>
     *
     * @throws Exception when local server setup or URL execution fails
     * @since 1.0
     *
     * @see UrlExecutionService#executeUrl(String, int)
     */
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

    /**
     * <h3>Return empty for response without data array</h3>
     * Verifies that {@link UrlExecutionService#executeUrl(String, int)} returns
     * an empty result when the JSON payload does not contain the expected
     * Patreon {@code data[]} array.
     * <p>
     * This test targets response-shape validation during JSON extraction.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Starts a local HTTP server returning JSON where {@code data} is an object.</li>
     *     <li>Mocks the configured Patreon cookie.</li>
     *     <li>Invokes URL execution and asserts the invalid payload maps to an empty result.</li>
     * </ul>
     *
     * @throws Exception when local server setup or URL execution fails
     * @since 1.0
     *
     * @see UrlExecutionService#executeUrl(String, int)
     */
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

    /**
     * <h3>Start local HTTP server</h3>
     * Creates and starts a local HTTP server on an available loopback port.
     *
     * @return started HTTP server bound to {@code 127.0.0.1}
     * @throws IOException when the local server cannot be created
     * @since 1.0
     */
    private HttpServer startServer() throws IOException {
        HttpServer localServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        localServer.start();
        return localServer;
    }

    /**
     * <h3>Add fixed HTTP response handler</h3>
     * Registers a deterministic response handler for the supplied path.
     *
     * @param path request path to handle
     * @param status HTTP status code to return
     * @param contentType response content type header
     * @param body response body bytes
     *
     * @since 1.0
     */
    private void addHandler(String path, int status, String contentType, byte[] body) {
        server.createContext(path, new FixedResponseHandler(status, contentType, body));
    }

    /**
     * <h3>Build local server URL</h3>
     * Builds an HTTP URL for the current local server and supplied path.
     *
     * @param path request path hosted by the local server
     *
     * @return absolute local HTTP URL
     * @since 1.0
     */
    private String urlOf(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    /**
     * The {@code FixedResponseHandler} record is created for returning fixed
     * HTTP responses from the local test server.
     * <p>
     * It supports the URL execution tests by simulating Patreon-style success
     * payloads, HTTP error responses, and malformed JSON payloads without
     * external network access.
     * </p>
     *
     * <h4>Key Features</h4>
     * <ul>
     *     <li>Writes the configured content type header.</li>
     *     <li>Sends the configured status code and body bytes.</li>
     *     <li>Closes the response body stream after writing.</li>
     * </ul>
     *
     * <h4>Codes</h4>
     * 1 - {@link HttpHandler}<br>
     * 2 - {@link HttpExchange}<br>
     *
     * <h4>Methods</h4>
     * <ul>
     *     <li>{@link FixedResponseHandler#handle(HttpExchange)}</li>
     * </ul>
     *
     * <p>
     * <h4>Notes</h4>
     * <ul>
     *     <li>This record is a test fixture and is not used by production URL execution.</li>
     * </ul>
     *
     * @param status HTTP status code returned by the fixture
     * @param contentType content type header returned by the fixture
     * @param body response body bytes returned by the fixture
     *
     * @author: SLMORA
     * @since 1.0
     *
     * <h4>Revision History</h4>
     * <blockquote><pre>
     * <br>Version      Date            Editor              Note
     * <br>-------------------------------------------------------
     * <br>1.0          6/6/2026       SLMORA              Initial Code
     * </pre></blockquote>
     */
    private record FixedResponseHandler(int status, String contentType, byte[] body) implements HttpHandler {
        /**
         * <h3>Write fixed HTTP response</h3>
         * Writes the configured status, content type, and body to the local
         * server exchange.
         *
         * @param exchange HTTP exchange supplied by the local test server
         *
         * @throws IOException when response headers or body cannot be written
         * @since 1.0
         */
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

