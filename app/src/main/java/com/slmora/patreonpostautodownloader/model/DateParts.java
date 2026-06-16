/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 2:05 AM
 */
package com.slmora.patreonpostautodownloader.model;

import lombok.Data;

/**
 * The {@code DateParts} class is created for carrying separated date and time
 * text derived from a Patreon post {@code published_at} timestamp.
 * <p>
 * The Excel and DOCX services use this DTO after parsing or splitting Patreon
 * timestamp values. It keeps the date and time portions available separately for
 * image record metadata, document output, and downstream display needs.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Stores the date portion extracted from a Patreon {@code published_at} value.</li>
 *     <li>Stores the time portion extracted from a Patreon {@code published_at} value.</li>
 *     <li>Uses Lombok {@link Data} to provide standard getters, setters, {@code equals}, {@code hashCode}, and {@code toString} methods.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * N/A<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>Lombok-generated getters and setters for {@link DateParts#date} and {@link DateParts#time}</li>
 *     <li>Lombok-generated {@code equals}, {@code hashCode}, and {@code toString}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>This model does not validate date or time format; parsing and fallback behavior belong to the calling service.</li>
 *     <li>Values may be empty strings when the source timestamp is missing or cannot provide a time portion.</li>
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
@Data
public class DateParts
{
    /**
     * Date component extracted from {@code published_at}, usually formatted as
     * an ISO local date such as {@code 2026-06-16}.
     */
    private String date;

    /**
     * Time component extracted from {@code published_at}, usually formatted as
     * an ISO local time when parsing succeeds, or the raw text after {@code T}
     * when fallback splitting is used.
     */
    private String time;
}
