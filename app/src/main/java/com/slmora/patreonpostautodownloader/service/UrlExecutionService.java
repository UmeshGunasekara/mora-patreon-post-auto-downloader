/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 3:40 PM
 */
package com.slmora.patreonpostautodownloader.service;

import com.slmora.common.logging.MoraLogger;
import com.slmora.common.logging.MoraLoggerThreadInfo;
import com.slmora.patreonpostautodownloader.config.PipelineConfig;
import com.slmora.patreonpostautodownloader.model.PostRecord;
import com.slmora.patreonpostautodownloader.model.URLExecute;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The {@code UrlExecutionService} class is created for executing Patreon API
 * URLs and converting response pages into pipeline post records.
 * <p>
 * The Excel producer uses this service to fetch each paginated Patreon response,
 * parse post attributes, collect media image URLs, and discover the next page
 * URL for continued processing.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Executes authenticated HTTP GET requests against Patreon API URLs.</li>
 *     <li>Maps response {@code data[]} entries into {@link PostRecord} instances.</li>
 *     <li>Extracts pagination from {@code links.next} into {@link URLExecute}.</li>
 *     <li>Collects matching media image URLs from the response {@code included[]} array.</li>
 *     <li>Returns {@link Optional#empty()} for blank URLs, HTTP failures, invalid URLs, or invalid JSON shape.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link PipelineConfig}<br>
 * 2 - {@link URLExecute}<br>
 * 3 - {@link PostRecord}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link UrlExecutionService#executeUrl(String, int)}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>The Patreon access cookie is read from {@link PipelineConfig#getPatreonAccessCookie()} and must be treated as sensitive configuration.</li>
 *     <li>Media URLs are pipe-delimited in the resulting post record because downstream Excel columns store them as text.</li>
 *     <li>The method catches request and parsing failures so the producer can retry the same URL according to its own loop.</li>
 * </ul>
 *
 * @author: SLMORA
 * @since 1.0
 *
 * <h4>Revision History</h4>
 * <blockquote><pre>
 * <br>Version      Date            Editor              Note
 * <br>-------------------------------------------------------
 * <br>1.0          6/6/2026      SLMORA                Initial Code
 * </pre></blockquote>
 */
public class UrlExecutionService
{
    /**
     * Class-scoped logger used for URL execution, parsing, and failure
     * diagnostics.
     */
    private final static MoraLogger LOGGER = MoraLogger.getLogger(UrlExecutionService.class);

    /**
     * Connection timeout used when opening a Patreon API HTTP connection.
     */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Per-request timeout used while waiting for a Patreon API response.
     */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    /**
     * <h3>Execute Patreon API URL</h3>
     * Fetches the JSON response for one Patreon API URL and extracts post data
     * plus the next pagination URL.
     * <p>
     * This is the public boundary used by the Excel producer. It logs and
     * returns an empty optional for invalid input, failed HTTP responses,
     * invalid URLs, interrupted requests, or JSON parsing errors.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Rejects {@code null}, empty, and blank URLs before creating an HTTP request.</li>
     *     <li>Fetches a JSON response through {@link UrlExecutionService#fetchJsonResponseForUrl(String)}.</li>
     *     <li>Maps the response with {@link UrlExecutionService#extractPostsFromJson(String)}.</li>
     *     <li>Returns {@link Optional#empty()} when execution or parsing cannot complete.</li>
     * </ul>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Optional<URLExecute> page = urlExecutionService.executeUrl(apiUrl, 1);
     * }</pre>
     *
     * @param url Patreon API URL to execute
     * @param recursiveIndex producer pagination index used only for diagnostics
     *
     * @return parsed URL execution result, or {@link Optional#empty()} when the URL cannot be processed
     *
     * @apiNote Callers are expected to decide whether and when to retry an empty result.
     * @since 1.0
     */
    public Optional<URLExecute> executeUrl(String url, int recursiveIndex)
    {
        URLExecute urlExecute;

        if (url == null || url.isBlank()) {
            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()),
                    "Input URL file is empty with recursive index: {}", recursiveIndex);
            return Optional.empty();
        }

        LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                        Thread.currentThread().threadId(),
                        Thread.currentThread().getStackTrace()),
                "Processing URL: {}", url);

        try {
            String jsonResponse = fetchJsonResponseForUrl(url);
            LOGGER.debug(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()),
                    "Processing URL jsonResponse: {}", jsonResponse);

            urlExecute = extractPostsFromJson(jsonResponse);
            LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()),
                    "Fetched posts: {}", urlExecute.getPostRecordList().size());

        } catch (Exception e) {
            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()),
                    "Error processing URL: {}  with recursive index: {}", url,  recursiveIndex);
            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()), e);
            return Optional.empty();
        }
        return Optional.of(urlExecute);
    }

    /**
     * <h3>Fetch Patreon JSON response</h3>
     * Sends an authenticated HTTP GET request and returns the raw response body.
     * <p>
     * The request uses browser-like headers because Patreon API responses are
     * expected to be accessed with the configured session cookie.
     * </p>
     *
     * @param apiUrl Patreon API URL to request
     *
     * @return raw JSON response body
     * @throws IOException when the response status is not {@code 200} or the request fails
     * @throws InterruptedException when the HTTP client send operation is interrupted
     * @since 1.0
     */
    private String fetchJsonResponseForUrl(String apiUrl) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .header("Accept", "application/json, text/plain, */*")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Referer", "https://www.patreon.com/")
                .header("Origin", "https://www.patreon.com")
                // Patreon access is session-bound; keep the cookie source centralized in PipelineConfig.
                .header("Cookie", PipelineConfig.getPatreonAccessCookie());

        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("HTTP request failed. Status code: " + response.statusCode()
                    + ", body: " + response.body());
        }

        return response.body();
    }

    /**
     * <h3>Extract posts from JSON</h3>
     * Parses one Patreon response page into {@link URLExecute}.
     * <p>
     * The method reads the response root, validates that {@code data} is an
     * array, maps post attributes into {@link PostRecord}, and collects media
     * URLs from the {@code included} relationship section.
     * </p>
     *
     * @param jsonResponse raw Patreon JSON response body
     *
     * @return parsed posts and next pagination URL
     * @throws IOException when the JSON cannot be parsed or {@code data} is not an array
     * @since 1.0
     */
    private URLExecute extractPostsFromJson(String jsonResponse) throws IOException {
        List<PostRecord> posts = new ArrayList<>();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonResponse);

        JsonNode dataArray = rootNode.path("data");
        JsonNode includedArray = rootNode.path("included");
        JsonNode links = rootNode.path("links");

        String nextUrl = getText(links, "next");

        if (!dataArray.isArray()) {
            // The producer depends on data[] page semantics; malformed pages should trigger retry handling.
            throw new IOException("'data' node is missing or not an array.");
        }

        for (JsonNode postNode : dataArray) {
            PostRecord record = new PostRecord();

            String postId = getText(postNode, "id");
            record.setId(postId);

            JsonNode attributes = postNode.path("attributes");
            record.setPublishedAt(getText(attributes, "published_at"));
            record.setTitle(getText(attributes, "title"));
            record.setCleanedTeaserText(getText(attributes, "cleaned_teaser_text"));
            record.setContentJsonString(getText(attributes, "content_json_string"));
            record.setCommentCount(getInt(attributes, "comment_count"));
            record.setPatreonUrl(getText(attributes, "url"));

            String largeUrl = "";
            String thumbUrl = "";
//            JsonNode imageNode = attributes.path("image");
//            if (!imageNode.isMissingNode() && !imageNode.isNull()) {
//                largeUrl = getText(imageNode, "large_url");
//                thumbUrl = getText(imageNode, "thumb_url");
//            }

            // Patreon currently exposes usable post media through included[].image_urls for this workflow.
            largeUrl = collectLargeUrls(postId, attributes, includedArray, "default_large");
            record.setLargeUrl(largeUrl);
            thumbUrl = collectLargeUrls(postId, attributes, includedArray, "default");
            record.setThumbUrl(thumbUrl);

            posts.add(record);
        }

        URLExecute urlExecute = new URLExecute();
        urlExecute.setPosts(posts);
        urlExecute.setNextUrl(nextUrl);

        return urlExecute;
    }

    /**
     * <h3>Collect media image URLs</h3>
     * Collects matching media URLs for a single Patreon post from the response
     * {@code included[]} array.
     * <p>
     * URLs are kept in response order, de-duplicated, filtered to the current
     * post id, and joined with {@code |} for Excel storage.
     * </p>
     *
     * @param postId Patreon post id used to match media URLs
     * @param attributes post attributes node; currently retained for direct-image compatibility
     * @param includedArray response {@code included} node containing related media entries
     * @param fetchName preferred {@code image_urls} field to read, such as {@code default_large} or {@code default}
     *
     * @return pipe-delimited image URLs, or an empty string when no matching media URL exists
     * @since 1.0
     */
    private String collectLargeUrls(String postId, JsonNode attributes, JsonNode includedArray, String fetchName) {
        Set<String> urls = new LinkedHashSet<>();

        // 1) Main image URL from data -> attributes -> image -> large_url
//        JsonNode imageNode = attributes.path("image");
//        if (!imageNode.isMissingNode() && !imageNode.isNull()) {
//            String directLargeUrl = getText(imageNode, "large_url");
//            if (!directLargeUrl.isBlank()) {
//                urls.add(directLargeUrl);
//            }
//        }

        // Only keep related media entries whose CDN path belongs to the current post id.
        if (includedArray != null && includedArray.isArray()) {
            String postPattern = "/p/post/" + postId + "/";

            for (JsonNode includedNode : includedArray) {
                String type = getText(includedNode, "type");
                if (!"media".equals(type)) {
                    continue;
                }

                JsonNode includedAttributes = includedNode.path("attributes");
                JsonNode imageUrlsNode = includedAttributes.path("image_urls");

                if (imageUrlsNode.isMissingNode() || imageUrlsNode.isNull()) {
                    continue;
                }

                String defaultLarge = getText(imageUrlsNode, fetchName);
                if (defaultLarge.isBlank()) {
                    // Some Patreon media entries omit the requested size but still expose original.
                    defaultLarge = getText(imageUrlsNode, "original");
                }

                if (!defaultLarge.isBlank() && defaultLarge.contains(postPattern)) {
                    urls.add(defaultLarge);
                }
            }
        }

        return String.join("|", urls);
    }

    /**
     * <h3>Read text field</h3>
     * Reads a text field from a JSON node using the service's default missing
     * value convention.
     *
     * @param node JSON node to read from
     * @param fieldName field name to resolve
     *
     * @return field text, or {@code "null"} when the field is missing or JSON null
     * @since 1.0
     */
    private String getText(JsonNode node, String fieldName) {
        JsonNode valueNode = node.path(fieldName);
        return valueNode.isMissingNode() || valueNode.isNull() ? "null" : valueNode.asText();
    }

    /**
     * <h3>Read integer field</h3>
     * Reads an integer field from a JSON node using zero as the missing value.
     *
     * @param node JSON node to read from
     * @param fieldName field name to resolve
     *
     * @return field integer value, or {@code 0} when the field is missing or JSON null
     * @since 1.0
     */
    private int getInt(JsonNode node, String fieldName) {
        JsonNode valueNode = node.path(fieldName);
        return valueNode.isMissingNode() || valueNode.isNull() ? 0 : valueNode.asInt();
    }

}
