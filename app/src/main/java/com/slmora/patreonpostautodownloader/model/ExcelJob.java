/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 2:20 PM
 */
package com.slmora.patreonpostautodownloader.model;

import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an Excel batch moving through the Patreon post download pipeline.
 * <p>
 * An {@code ExcelJob} identifies the generated Excel workbook, tracks image
 * records extracted from that workbook, and carries status, retry, and error
 * details between the producer, image download, retry, DOCX, and failed-job
 * monitor stages. Lombok {@link Data} generates the standard getters, setters
 * for mutable fields, {@code equals}, {@code hashCode}, and required accessor
 * methods for this pipeline DTO.
 * </p>
 *
 * <p>Methods:</p>
 * <ul>
 *     <li>{@link #ExcelJob(long, Path)} - creates a job for a generated Excel file.</li>
 *     <li>{@link #incrementRetryCount()} - increments the retry attempt counter.</li>
 *     <li>{@link #toString()} - returns a log-friendly job summary.</li>
 *     <li>Lombok-generated accessors and mutators from {@link Data}.</li>
 * </ul>
 *
 * <p>Key responsibilities:</p>
 * <ul>
 *     <li>Keep the unique job identifier and Excel file path together.</li>
 *     <li>Store image records associated with the Excel batch.</li>
 *     <li>Expose status, retry count, and error message for pipeline coordination.</li>
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
public class ExcelJob
{
    /**
     * Unique identifier assigned by the Excel producer.
     */
    private final long jobId;

    /**
     * Path to the finalized Excel workbook for this job.
     */
    private final Path excelFile;

    private volatile Path docxFile;

    /**
     * Thread-safe collection of image records parsed from the Excel workbook.
     */
    private final List<ImageRecord> imageRecords =
            Collections.synchronizedList(new ArrayList<>());

    /**
     * Current lifecycle status for this job.
     */
    private volatile JobStatus status;

    /**
     * Number of retry attempts made for this job.
     */
    private volatile int retryCount;

    /**
     * Last error message recorded while processing this job.
     */
    private volatile String errorMessage;

    /**
     * Creates a job for a generated Excel workbook.
     *
     * @param jobId unique job identifier assigned by the producer
     * @param excelFile path to the finalized Excel workbook
     */
    public ExcelJob(long jobId, Path excelFile) {
        this.jobId = jobId;
        this.excelFile = excelFile;
    }

    /**
     * Increments the retry counter after a failed processing attempt.
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }

    /**
     * Builds a concise representation suitable for logs and queue diagnostics.
     *
     * @return formatted job summary including identifier, file, status, retry count, and error message
     */
    public String toString() {
        return "ExcelJob{" +
                "jobId=" + jobId +
                ", excelFile=" + excelFile +
                ", status=" + status +
                ", retryCount=" + retryCount +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
