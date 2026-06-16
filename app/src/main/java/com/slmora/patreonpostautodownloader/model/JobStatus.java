/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 2:17 PM
 */
package com.slmora.patreonpostautodownloader.model;

/**
 * The {@code JobStatus} enum is created for representing the lifecycle state of
 * an {@link ExcelJob} as it moves through the Patreon post download pipeline.
 * <p>
 * Process stages update this status while creating Excel files, downloading
 * images, retrying failures, generating DOCX reports, and persisting failed
 * jobs. The status is used for logging, retry decisions, queue handoff, and job
 * persistence output.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Tracks the current processing stage of an {@link ExcelJob}.</li>
 *     <li>Represents retry, success, and terminal failure states used by process workers.</li>
 *     <li>Provides simple enum constants that can be written directly to job-status logs.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link ExcelJob}<br>
 * 2 - {@link DownloadStatus}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link JobStatus#valueOf(String)}</li>
 *     <li>{@link JobStatus#values()}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>This enum describes job-level state; individual image download state is represented by {@link DownloadStatus}.</li>
 *     <li>State transitions are controlled by process and retry services, not by this enum.</li>
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
public enum JobStatus
{
    /**
     * Excel batch file has been created and queued for image extraction.
     */
    EXCEL_CREATED,

    /**
     * Image records are being read from Excel and downloaded.
     */
    IMAGE_DOWNLOAD_IN_PROGRESS,

    /**
     * All image records for the job have completed successfully.
     */
    IMAGES_DOWNLOADED,

    /**
     * Job has failed image downloads and is waiting for retry processing.
     */
    RETRY_PENDING,

    /**
     * DOCX report generation is in progress for the job.
     */
    DOCX_CREATION_IN_PROGRESS,

    /**
     * DOCX report has been created successfully for the job.
     */
    DOCX_CREATED,

    /**
     * Job reached a terminal failure state and should be persisted by failed-job handling.
     */
    FAILED
}
