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
 * The {@code UrlExecutionService} Class created for
 * <h4>Key Features</h4>
 * <ul>
 *      <li>...</li>
 * </ul>
 * <h4>Codes</h4>
 * 1 - {@link }<br>
 * <h4>Methods</h4>
 * <ul>
 *      <li>{@link }</li>
 * </ul>
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>....</li>
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
    private final static MoraLogger LOGGER = MoraLogger.getLogger(UrlExecutionService.class);

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

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
                .header("Cookie", PipelineConfig.getPatreonAccessCookie());

        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("HTTP request failed. Status code: " + response.statusCode()
                    + ", body: " + response.body());
        }

        return response.body();
    }

    private URLExecute extractPostsFromJson(String jsonResponse) throws IOException {
        List<PostRecord> posts = new ArrayList<>();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonResponse);

        JsonNode dataArray = rootNode.path("data");
        JsonNode includedArray = rootNode.path("included");
        JsonNode links = rootNode.path("links");

        String nextUrl = getText(links, "next");

        if (!dataArray.isArray()) {
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

        // 2) Additional image URLs from included[] where type == media
        //    use attributes.image_urls.default_large
        //    and only keep URLs containing /p/post/<postId>/
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
                    defaultLarge = getText(imageUrlsNode, "original");
                }

                if (!defaultLarge.isBlank() && defaultLarge.contains(postPattern)) {
                    urls.add(defaultLarge);
                }
            }
        }

        return String.join("|", urls);
    }

    private String getText(JsonNode node, String fieldName) {
        JsonNode valueNode = node.path(fieldName);
        return valueNode.isMissingNode() || valueNode.isNull() ? "null" : valueNode.asText();
    }

    private int getInt(JsonNode node, String fieldName) {
        JsonNode valueNode = node.path(fieldName);
        return valueNode.isMissingNode() || valueNode.isNull() ? 0 : valueNode.asInt();
    }

}
