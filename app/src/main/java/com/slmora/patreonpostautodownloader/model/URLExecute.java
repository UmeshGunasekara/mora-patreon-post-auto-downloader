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
 * The {@code URLExecute} class is created for carrying the parsed result of one
 * Patreon post API URL execution.
 * <p>
 * The URL execution service creates this DTO after reading a Patreon API
 * response. The Excel producer consumes it to append the extracted
 * {@link PostRecord} values into the workbook and continue pagination when
 * {@link URLExecute#nextUrl} is present.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Stores all Patreon posts parsed from one API response page.</li>
 *     <li>Stores the next pagination URL returned by Patreon when additional pages exist.</li>
 *     <li>Supports fluent assignment of posts through {@link URLExecute#setPosts(List)}.</li>
 *     <li>Uses Lombok {@link Data} to provide standard accessors, mutators, {@code equals}, {@code hashCode}, and {@code toString} methods.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link PostRecord}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link URLExecute#setPosts(List)}</li>
 *     <li>Lombok-generated getters and setters for {@link URLExecute#postRecordList} and {@link URLExecute#nextUrl}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>This DTO does not validate URLs or response completeness; parsing and validation belong to the URL execution service.</li>
 *     <li>{@code nextUrl} may be {@code null} or blank when Patreon does not provide another page.</li>
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
     * Post records parsed from the executed Patreon API response page.
     */
    private List<PostRecord> postRecordList;

    /**
     * Pagination URL for the next Patreon API page, when one is provided by the
     * response links.
     */
    private String nextUrl;

    /**
     * <h3>Assign parsed post records</h3>
     * Assigns post records and returns this result object for fluent
     * construction in the URL parsing flow.
     * <p>
     * The method intentionally mirrors the generated setter behavior while
     * returning {@code this}, allowing the caller to build a response object in a
     * compact way after JSON extraction.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Replaces the current post list reference with the supplied list.</li>
     *     <li>Does not copy or validate the list content.</li>
     *     <li>Returns the same {@link URLExecute} instance for fluent use.</li>
     * </ul>
     *
     * @param posts post records parsed from the API response; may be {@code null} when the caller has no parsed records
     *
     * @return this URL execution result
     *
     * @implNote The list reference is stored directly because this model is a
     * mutable pipeline transfer object.
     * @since 1.0
     */
    public URLExecute setPosts(List<PostRecord> posts)
    {
        this.postRecordList = posts;
        return this;
    }
}
