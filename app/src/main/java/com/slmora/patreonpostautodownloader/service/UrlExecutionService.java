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

    public Optional<URLExecute> executeUrl(String url)
    {
//        List<PostRecord> allPosts = new ArrayList<>();
        URLExecute urlExecute;


        if (url == null || url.isBlank()) {
//            System.out.println("Input URL file is empty.");
            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                    Thread.currentThread().getId(),
                    Thread.currentThread().getStackTrace()), "Input URL file is empty.");
            return Optional.empty();
        }

//        System.out.println("Processing URL: " + url);
        LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                Thread.currentThread().getId(),
                Thread.currentThread().getStackTrace()),"Processing URL: {}", url);

        try {

            String jsonResponse = fetchJsonResponse(url);
            urlExecute = extractPosts(jsonResponse);

//            List<PostRecord> postRecords = urlExecute.getPostRecordList();

//            allPosts.addAll(postRecords);
//            System.out.println("Fetched posts: " + urlExecute.getPostRecordList().size());
            LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                    Thread.currentThread().getId(),
                    Thread.currentThread().getStackTrace()),"Fetched posts: {}", urlExecute.getPostRecordList().size());

            // Replace this with your real URL execution logic.
//            List<PostRecord> records = new ArrayList<>();

        } catch (IOException | InterruptedException e) {
//            System.err.println("Error processing URL: " + url);
//            e.printStackTrace();
            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                    Thread.currentThread().getId(),
                    Thread.currentThread().getStackTrace()), "Error processing URL: {}", url);
            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                    Thread.currentThread().getId(),
                    Thread.currentThread().getStackTrace()), e);
            return Optional.empty();
        }
        return Optional.of(urlExecute);
    }

    private String fetchJsonResponse(String apiUrl) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(60))
                .GET()
                .header("Accept", "application/json, text/plain, */*")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Referer", "https://www.patreon.com/")
                .header("Origin", "https://www.patreon.com")
//                .header("Cookie", "patreon_device_id=08f32255-011d-4544-9d8c-fb8ce831a362; patreon_location_country_code=SG; patreon_locale_code=en-US; g_state={\"i_l\":0,\"i_ll\":1771459152508,\"i_b\":\"/2gBIfOQdE6N8fC00iDuwk+zwdqwiOY792zIJYIE1WE\",\"i_e\":{\"enable_itp_optimization\":0}}; __ssid=7d5aeef0-15c2-4085-a2cf-876a04a6d219; session_id=JS1Wb9P7WXGroo-3eFs95P1OKUJ3TnDrOkw8v2XYokA; patreon_currency_pref=USD; _ga=GA1.1.948299339.1771685127; _ga_JF55G82FNT=GS2.1.s1771685126$o1$g1$t1771685273$j60$l0$h0; stream_user_token={%22id%22:%2299404896%22%2C%22type%22:%22stream-user-token%22%2C%22token%22:%22eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHBpcmVzX2F0IjoxNzczODc3MDI1LCJ1c2VyX2lkIjoiOTk0MDQ4OTYiLCJpYXQiOjE3NzI2Njc0MjV9.nNtQfKKGegZdbmsLn24nCO3j9cEQW8SAsv22ZJemTx8%22%2C%22expiresAt%22:%222026-03-18T23:37:05.128+00:00%22}; muxData=mux_viewer_id=2aef67d4-7d06-4b34-bce4-6736413da772&msn=0.29754360385419387&sid=d11b0227-0296-4535-a439-5c9832fcb5a4&sst=1772755603863.1&sex=1772757103865.2; _cfuvid=8S3e08Tah_THahJ0C_qwq3JPv..f.1r0X93Xj18jego-1773326038322-0.0.1.1-604800000; analytics_session_id=ba8664b0-c642-49dd-a43b-4a2588ba4575; __cf_bm=rpPRgP2trXDiP1EzlhXiQpFrzTfvOtZgM8DnpHOLTZ4-1773548345-1.0.1.1-g8poSCidEw2LgWfkNmC4e_ouqIjkhKOolcTduzd4x8pvGDdtUxIfKW9n.6uu_1153sSZFaPe_FU2dmISfMnP3FszNEFqRtEqJTNcF.tOINFueheT_CrH3HC_fooVUG8G");
//                .header("Cookie", "patreon_device_id=08f32255-011d-4544-9d8c-fb8ce831a362; patreon_location_country_code=SG; patreon_locale_code=en-US; g_state={\"i_l\":0,\"i_ll\":1771459152508,\"i_b\":\"/2gBIfOQdE6N8fC00iDuwk+zwdqwiOY792zIJYIE1WE\",\"i_e\":{\"enable_itp_optimization\":0}}; __ssid=7d5aeef0-15c2-4085-a2cf-876a04a6d219; session_id=JS1Wb9P7WXGroo-3eFs95P1OKUJ3TnDrOkw8v2XYokA; patreon_currency_pref=USD; _ga=GA1.1.948299339.1771685127; _ga_JF55G82FNT=GS2.1.s1771685126$o1$g1$t1771685273$j60$l0$h0; stream_user_token={%22id%22:%2299404896%22%2C%22type%22:%22stream-user-token%22%2C%22token%22:%22eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHBpcmVzX2F0IjoxNzczODc3MDI1LCJ1c2VyX2lkIjoiOTk0MDQ4OTYiLCJpYXQiOjE3NzI2Njc0MjV9.nNtQfKKGegZdbmsLn24nCO3j9cEQW8SAsv22ZJemTx8%22%2C%22expiresAt%22:%222026-03-18T23:37:05.128+00:00%22}; muxData=mux_viewer_id=2aef67d4-7d06-4b34-bce4-6736413da772&msn=0.29754360385419387&sid=d11b0227-0296-4535-a439-5c9832fcb5a4&sst=1772755603863.1&sex=1772757103865.2; _cfuvid=8S3e08Tah_THahJ0C_qwq3JPv..f.1r0X93Xj18jego-1773326038322-0.0.1.1-604800000; analytics_session_id=e74f4ee1-afcf-4ba4-967d-b1a5fc17961c; __cf_bm=vLVokYEcwBseWR0ddqTyvji8LXQWN6uz4dWlCUbym_Y-1773595883-1.0.1.1-SdnkAJ1KcBBmWJgi5c57pNT.l4IRbdyjbyoRpMFoPyZ0dIf1gyYOVcTOPZl7rKoI.HKaUT357NjaNoNgtrlbAStDM6qG2POkEkK0SQuUkcXwFKmBeKnasWEOvbfCxJ5Z");
                .header("Cookie", "patreon_device_id=08f32255-011d-4544-9d8c-fb8ce831a362; patreon_location_country_code=SG; patreon_locale_code=en-US; g_state={\"i_l\":0,\"i_ll\":1771459152508,\"i_b\":\"/2gBIfOQdE6N8fC00iDuwk+zwdqwiOY792zIJYIE1WE\",\"i_e\":{\"enable_itp_optimization\":0}}; __ssid=7d5aeef0-15c2-4085-a2cf-876a04a6d219; session_id=JS1Wb9P7WXGroo-3eFs95P1OKUJ3TnDrOkw8v2XYokA; patreon_currency_pref=USD; _ga=GA1.1.948299339.1771685127; _ga_JF55G82FNT=GS***REMOVED***");

        HttpRequest request = requestBuilder.build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("HTTP request failed. Status code: " + response.statusCode()
                    + ", body: " + response.body());
        }

        return response.body();
    }

    private URLExecute extractPosts(String jsonResponse) throws IOException {
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

//            record.setSourceUrl(sourceUrl);
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
