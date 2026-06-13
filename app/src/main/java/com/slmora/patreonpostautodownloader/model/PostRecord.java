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
 * Represents a Patreon post extracted from an API response.
 * <p>
 * This DTO carries the post metadata, textual content, post URL, and image
 * URLs that are written to Excel and later used for image downloads and DOCX
 * report generation. Lombok {@link Data} generates the accessors, mutators,
 * {@code equals}, {@code hashCode}, and {@code toString} methods.
 * </p>
 *
 * <p>Methods:</p>
 * <ul>
 *     <li>Lombok-generated getters and setters for all post fields.</li>
 *     <li>Lombok-generated {@code equals}, {@code hashCode}, and {@code toString} methods.</li>
 * </ul>
 *
 * <p>Key responsibilities:</p>
 * <ul>
 *     <li>Store Patreon post identity, title, publication date, and comment count.</li>
 *     <li>Store teaser and structured content used for report output.</li>
 *     <li>Store Patreon and image URLs used by downstream pipeline stages.</li>
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
     * Patreon post identifier.
     */
    private String id;

    /**
     * Publication timestamp returned by the Patreon API.
     */
    private String publishedAt;

    /**
     * Patreon post title.
     */
    private String title;

    /**
     * Cleaned teaser text extracted from the post payload.
     */
    private String cleanedTeaserText;

    /**
     * Raw or serialized content JSON used for DOCX text extraction.
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
     * Large image URL associated with the post.
     */
    private String largeUrl;

    /**
     * Thumbnail image URL associated with the post.
     */
    private String thumbUrl;
}
