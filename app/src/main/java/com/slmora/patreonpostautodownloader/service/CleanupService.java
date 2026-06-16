/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 3:45 PM
 */
package com.slmora.patreonpostautodownloader.service;

import com.slmora.common.logging.MoraLogger;
import com.slmora.common.logging.MoraLoggerThreadInfo;
import com.slmora.patreonpostautodownloader.config.PipelineConfig;
import com.slmora.patreonpostautodownloader.model.ExcelJob;

import java.io.IOException;
import java.nio.file.Files;

/**
 * The {@code CleanupService} class is created for handling post-processing
 * cleanup after a Patreon post pipeline job completes.
 * <p>
 * The DOCX producer calls this service after a successful report is created.
 * The current success-cleanup operation records completion and leaves generated
 * Excel files in place, while {@link CleanupService#deleteExcelFile(ExcelJob)}
 * provides an explicit deletion operation for callers or tests that need it.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Provides a success-cleanup hook for completed {@link ExcelJob} instances.</li>
 *     <li>Logs cleanup success or failure without interrupting downstream pipeline shutdown.</li>
 *     <li>Provides explicit Excel file deletion through {@link CleanupService#deleteExcelFile(ExcelJob)}.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link ExcelJob}<br>
 * 2 - {@link com.slmora.patreonpostautodownloader.process.ProcessDocxProducer}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link CleanupService#cleanupAfterSuccess(ExcelJob)}</li>
 *     <li>{@link CleanupService#deleteExcelFile(ExcelJob)}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>Successful cleanup is intentionally non-fatal; exceptions are logged and swallowed.</li>
 *     <li>Excel files are not deleted by {@link CleanupService#cleanupAfterSuccess(ExcelJob)} in the current implementation.</li>
 *     <li>{@link CleanupService#deleteExcelFile(ExcelJob)} is idempotent for missing files because it uses {@link Files#deleteIfExists(java.nio.file.Path)}.</li>
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
public class CleanupService
{
    /**
     * Class-scoped logger used for cleanup completion and cleanup failure
     * diagnostics.
     */
    private final static MoraLogger LOGGER = MoraLogger.getLogger(CleanupService.class);

    /**
     * <h3>Run success cleanup</h3>
     * Runs the cleanup hook after a job has successfully produced its DOCX
     * report.
     * <p>
     * The current implementation logs cleanup completion and deliberately keeps
     * the generated Excel file available for audit or later inspection.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Receives a completed {@link ExcelJob} from the DOCX producer.</li>
     *     <li>Logs cleanup completion with the job id.</li>
     *     <li>Catches and logs cleanup errors without rethrowing them.</li>
     * </ul>
     *
     * @param job completed Excel job whose success cleanup should run
     *
     * @implNote File deletion is not part of this success hook at the moment.
     * Use {@link CleanupService#deleteExcelFile(ExcelJob)} when deletion is
     * explicitly required.
     * @apiNote This method is safe for the DOCX producer to call after success
     * persistence because cleanup failures do not change job status.
     * @since 1.0
     */
    public void cleanupAfterSuccess(ExcelJob job) {
        try {
            // Keep the generated Excel file by default so completed jobs remain auditable after DOCX creation.
            // Files.deleteIfExists(job.getExcelFile());
            LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()),
                    "Cleanup completed for job: {}", job.getJobId());

        } catch (Exception e) {
            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()),
                    "Cleanup failed for job: {} with {}", job.getJobId(), e);
        }
    }

    /**
     * <h3>Delete Excel file</h3>
     * Deletes the Excel workbook associated with the supplied job when the job
     * has an Excel file path.
     * <p>
     * The operation uses {@link Files#deleteIfExists(java.nio.file.Path)}, so it
     * succeeds when the file is already absent. A job with a {@code null} Excel
     * path is ignored.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Checks whether {@link ExcelJob#getExcelFile()} is present.</li>
     *     <li>Deletes the file only when the path is not {@code null}.</li>
     *     <li>Lets file-system exceptions propagate to the caller.</li>
     * </ul>
     *
     * @param job Excel job whose workbook should be deleted
     *
     * @throws Exception when the file-system delete operation fails
     *
     * @apiNote This method performs real file-system deletion and should be
     * called only when the Excel workbook is no longer needed.
     * @since 1.0
     */
    public void deleteExcelFile(ExcelJob job) throws Exception {
        if (job.getExcelFile() != null) {
            Files.deleteIfExists(job.getExcelFile());
        }
    }
}
