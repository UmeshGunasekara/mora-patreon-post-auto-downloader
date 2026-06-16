/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/9/2026 10:38 PM
 */
package com.slmora.patreonpostautodownloader.model;

import lombok.Data;

/**
 * The {@code PostRecord} class is created for carrying Patreon post data
 * extracted from an API response.
 * <p>
 * The URL execution service maps Patreon JSON into this DTO. The Excel service
 * writes these values into the configured workbook columns, and later pipeline
 * stages use the stored content and image URLs for downloads and DOCX report
 * generation.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Stores Patreon identity, publication timestamp, title, teaser text, content JSON, and comment count.</li>
 *     <li>Stores the public Patreon post URL used for report references.</li>
 *     <li>Stores large and thumbnail image URLs used by downstream image extraction and download stages.</li>
 *     <li>Uses Lombok {@link Data} to provide standard accessors, mutators, {@code equals}, {@code hashCode}, and {@code toString} methods.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link URLExecute}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>Lombok-generated getters and setters for all post fields</li>
 *     <li>Lombok-generated {@code equals}, {@code hashCode}, and {@code toString}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>This model preserves string values as received or normalized by the URL parsing service; it does not validate Patreon payload fields.</li>
 *     <li>The Excel column contract depends on several fields in this DTO, including {@code id}, {@code publishedAt}, {@code title}, {@code contentJsonString}, {@code largeUrl}, and {@code thumbUrl}.</li>
 * </ul>
 *
 * @author: SLMORA
 * @since 1.0
 *
 * <h4>Revision History</h4>
 * <blockquote><pre>
 * <br>Version      Date            Editor              Note
 * <br>-------------------------------------------------------
 * <br>1.0          6/9/2026      SLMORA                Initial Code
 * </pre></blockquote>
 */
@Data
public class PostRecord
{
    /**
     * Patreon post identifier mapped from the response {@code id} field.
     */
    private String id;

    /**
     * Publication timestamp returned by the Patreon API, usually in an ISO
     * offset date-time format.
     */
    private String publishedAt;

    /**
     * Patreon post title.
     */
    private String title;

    /**
     * Cleaned teaser text extracted from the post payload attributes.
     */
    private String cleanedTeaserText;

    /**
     * Raw or serialized content JSON string used by DOCX generation to extract
     * report text.
     */
    private String contentJsonString;

    /**
     * Number of comments reported for the post.
     */
    private int commentCount;

    /**
     * Public Patreon URL for the post.
     */
    private String patreonUrl;

    /**
     * Large image URL associated with the post, when Patreon exposes one in the
     * response payload.
     */
    private String largeUrl;

    /**
     * Thumbnail image URL associated with the post, when Patreon exposes one in
     * the response payload.
     */
    private String thumbUrl;
}
