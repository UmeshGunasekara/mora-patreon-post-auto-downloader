/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 2:18 PM
 */
package com.slmora.patreonpostautodownloader.model;

/**
 * The {@code DownloadStatus} enum is created for representing the download
 * state of an individual {@link ImageRecord}.
 * <p>
 * Image download and retry services update this status while attempting to
 * fetch images referenced by an {@link ExcelJob}. Process workers inspect these
 * values to decide whether a job can proceed to DOCX generation, retry, or
 * failed-job persistence.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Tracks per-image download progress independently from job-level status.</li>
 *     <li>Supports retry decisions by marking failed image records.</li>
 *     <li>Provides simple enum constants that can be written to failed-job details.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link ImageRecord}<br>
 * 2 - {@link ExcelJob}<br>
 * 3 - {@link JobStatus}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link DownloadStatus#valueOf(String)}</li>
 *     <li>{@link DownloadStatus#values()}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>This enum describes image-level state; batch lifecycle state is represented by {@link JobStatus}.</li>
 *     <li>State transitions are performed by image download and retry services.</li>
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
public enum DownloadStatus
{
    /**
     * Image has been discovered but no download result has been recorded yet.
     */
    PENDING,

    /**
     * Image was downloaded successfully and its output path should be available.
     */
    SUCCESS,

    /**
     * Image download failed and error details should be stored on the image record.
     */
    FAILED
}
