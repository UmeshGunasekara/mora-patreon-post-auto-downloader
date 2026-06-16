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
 * The {@code ExcelJob} class is created for carrying one generated Excel batch
 * through the Patreon post download pipeline.
 * <p>
 * A job starts after the Excel producer finalizes a workbook. Later stages use
 * the same job object to store image records, image download state, retry
 * counts, generated DOCX path, terminal status, and error details while the job
 * moves across the Excel-ready, retry, DOCX-ready, and failed queues.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Stores the unique job identifier and finalized Excel workbook path.</li>
 *     <li>Maintains a synchronized image-record list populated from the Excel workbook.</li>
 *     <li>Tracks volatile status, retry count, DOCX path, and error details shared between process threads.</li>
 *     <li>Provides a concise {@link ExcelJob#toString()} for queue and job-status logging.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link ImageRecord}<br>
 * 2 - {@link JobStatus}<br>
 * 3 - {@link DownloadStatus}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link ExcelJob#ExcelJob(long, Path)}</li>
 *     <li>{@link ExcelJob#incrementRetryCount()}</li>
 *     <li>{@link ExcelJob#toString()}</li>
 *     <li>Lombok-generated accessors and mutators from {@link Data}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>The image list is synchronized because image processing and retry paths can inspect and mutate job image records across pipeline stages.</li>
 *     <li>Volatile fields provide visibility for job state exchanged between producer-worker threads.</li>
 *     <li>This model does not enforce legal status transitions; process and retry services own that workflow logic.</li>
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
     * Unique identifier assigned by the Excel producer when the batch workbook
     * is finalized.
     */
    private final long jobId;

    /**
     * Path to the finalized Excel workbook for this job.
     */
    private final Path excelFile;

    /**
     * Path to the generated DOCX report after document creation succeeds.
     */
    private volatile Path docxFile;

    /**
     * Thread-safe collection of image records parsed from the Excel workbook.
     */
    private final List<ImageRecord> imageRecords =
            Collections.synchronizedList(new ArrayList<>());

    /**
     * Current lifecycle status for this job, updated by process stages.
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
     * <h3>Create Excel pipeline job</h3>
     * Creates a job for a finalized Excel workbook produced by the Excel
     * producer stage.
     * <p>
     * The constructor captures immutable job identity values. Mutable processing
     * fields such as status, image records, retry count, DOCX path, and error
     * message are populated as the job moves through later pipeline stages.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Stores the unique job identifier used in logs and persistence output.</li>
     *     <li>Stores the finalized Excel file path used by image extraction and DOCX generation.</li>
     *     <li>Leaves status and retry metadata unset until process stages update them.</li>
     * </ul>
     *
     * @param jobId unique job identifier assigned by the producer
     * @param excelFile path to the finalized Excel workbook
     *
     * @since 1.0
     */
    public ExcelJob(long jobId, Path excelFile) {
        this.jobId = jobId;
        this.excelFile = excelFile;
    }

    /**
     * <h3>Increment retry count</h3>
     * Increments the retry counter after a failed image processing attempt.
     * <p>
     * Retry services use this value when deciding whether the job should return
     * to retry processing or move to failed-job handling.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Adds one to the current retry count.</li>
     *     <li>Does not validate the configured maximum retry limit.</li>
     * </ul>
     *
     * @implNote The counter field is volatile for visibility, but this increment
     * operation is not atomic across multiple concurrent callers.
     * @since 1.0
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }

    /**
     * <h3>Create job log summary</h3>
     * Builds a concise representation suitable for logs, queue diagnostics, and
     * job-status output.
     * <p>
     * The summary intentionally excludes the image-record list to avoid noisy log
     * output when a job contains many downloaded image entries.
     * </p>
     *
     * @return formatted job summary including identifier, file, status, retry count, and error message
     *
     * @implNote Keep this output compact because jobs are logged during queue
     * handoff and failure handling.
     * @since 1.0
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
