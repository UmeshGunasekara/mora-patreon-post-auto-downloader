/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/9/2026 10:43 PM
 */
package com.slmora.patreonpostautodownloader.model;

import lombok.Data;

import java.util.List;

/**
 * Holds the result of executing a Patreon post API URL.
 * <p>
 * This DTO carries the post records extracted from one API response and the
 * next pagination URL returned by Patreon. It is used by the Excel producer to
 * write the current page into a workbook and continue processing additional
 * pages when a next link is available. Lombok {@link Data} generates the
 * standard getters, setters, {@code equals}, {@code hashCode}, and
 * {@code toString} methods.
 * </p>
 *
 * <p>Methods:</p>
 * <ul>
 *     <li>{@link #setPosts(List)} - assigns post records and returns this result for fluent use.</li>
 *     <li>Lombok-generated getters and setters for {@link #postRecordList} and {@link #nextUrl}.</li>
 *     <li>Lombok-generated {@code equals}, {@code hashCode}, and {@code toString} methods.</li>
 * </ul>
 *
 * <p>Key responsibilities:</p>
 * <ul>
 *     <li>Store the posts parsed from a Patreon API response.</li>
 *     <li>Store the next pagination URL for continued post retrieval.</li>
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
public class URLExecute
{
    /**
     * Post records parsed from the executed Patreon API URL.
     */
    private List<PostRecord> postRecordList;

    /**
     * Pagination URL for the next Patreon API page.
     */
    private String nextUrl;

    /**
     * Assigns post records and returns this object for fluent construction.
     *
     * @param posts post records parsed from the API response
     * @return this URL execution result
     */
    public URLExecute setPosts(List<PostRecord> posts)
    {
        this.postRecordList = posts;
        return this;
    }
}
