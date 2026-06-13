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
 * Holds separated date and time values used by the Patreon post pipeline.
 * <p>
 * This DTO represents a timestamp after it has been split into display or file
 * naming components. Lombok {@link Data} generates the accessors, mutators,
 * {@code equals}, {@code hashCode}, and {@code toString} methods.
 * </p>
 *
 * <p>Methods:</p>
 * <ul>
 *     <li>Lombok-generated getters and setters for {@link #date} and {@link #time}.</li>
 *     <li>Lombok-generated {@code equals}, {@code hashCode}, and {@code toString} methods.</li>
 * </ul>
 *
 * <p>Key responsibilities:</p>
 * <ul>
 *     <li>Store the date portion of a parsed timestamp.</li>
 *     <li>Store the time portion of a parsed timestamp.</li>
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
     * Date component extracted from a timestamp.
     */
    private String date;

    /**
     * Time component extracted from a timestamp.
     */
    private String time;
}
