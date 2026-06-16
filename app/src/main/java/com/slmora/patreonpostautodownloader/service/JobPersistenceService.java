/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 3:46 PM
 */
package com.slmora.patreonpostautodownloader.service;

import com.slmora.common.logging.MoraLogger;
import com.slmora.common.logging.MoraLoggerThreadInfo;
import com.slmora.patreonpostautodownloader.config.PipelineConfig;
import com.slmora.patreonpostautodownloader.model.ExcelJob;
import com.slmora.patreonpostautodownloader.model.ImageRecord;
import com.slmora.patreonpostautodownloader.process.FailedJobMonitor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The {@code JobPersistenceService} class is created for persisting pipeline
 * job status and failure details to local log files.
 * <p>
 * Pipeline workers call this service when an {@link ExcelJob} changes status,
 * completes DOCX generation, or reaches the failed-job path monitored by
 * {@link FailedJobMonitor}.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Creates job status and failed-job log files in the configured failure output directory.</li>
 *     <li>Appends compact status lines for normal job progress and successful DOCX creation.</li>
 *     <li>Writes detailed failed-job sections including retry count, output files, and image-level failures.</li>
 *     <li>Uses separate locks so status and failure log appends remain serialized per file.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link PipelineConfig}<br>
 * 2 - {@link ExcelJob}<br>
 * 3 - {@link ImageRecord}<br>
 * 4 - {@link FailedJobMonitor}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link JobPersistenceService#saveJobStatus(ExcelJob)}</li>
 *     <li>{@link JobPersistenceService#saveSuccessJob(ExcelJob)}</li>
 *     <li>{@link JobPersistenceService#saveFailedJob(ExcelJob)}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>The service writes append-only text logs and does not remove or rotate existing entries.</li>
 *     <li>Write failures are logged and not rethrown, allowing pipeline workers to continue their own failure handling.</li>
 *     <li>Log values are normalized to one physical line to keep each persisted record parseable by humans.</li>
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
public class JobPersistenceService
{
    /**
     * Class-scoped logger used when persistence writes cannot be completed.
     */
    private final static MoraLogger LOGGER = MoraLogger.getLogger(JobPersistenceService.class);

    /**
     * File path used for compact job status and success entries.
     */
    private final Path jobLogFile;

    /**
     * File path used for detailed failed-job entries.
     */
    private final Path failedLogFile;

    /**
     * Monitor object that serializes writes to {@link JobPersistenceService#jobLogFile}.
     */
    private final Object statusLock = new Object();

    /**
     * Monitor object that serializes writes to {@link JobPersistenceService#failedLogFile}.
     */
    private final Object failedLock = new Object();

    /**
     * Timestamp format used in persisted job log entries.
     */
    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * <h3>Create job persistence service</h3>
     * Resolves the job status and failed-job log files from pipeline
     * configuration and creates missing files.
     * <p>
     * Both log files are placed in {@link PipelineConfig#getFailedOutputDirPath()}
     * because they represent operational status and failure diagnostics for
     * the current pipeline run.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Resolves {@code job-status.log} and {@code failed-jobs.log} under the configured failed output directory.</li>
     *     <li>Creates each log file when it does not already exist.</li>
     *     <li>Leaves existing log files untouched so new entries append to previous history.</li>
     * </ul>
     *
     * @throws IOException when the configured log files cannot be created
     *
     * @apiNote The configured failed output directory must already exist before this constructor is called.
     * @since 1.0
     */
    public JobPersistenceService() throws IOException
    {
        this.jobLogFile = PipelineConfig.getFailedOutputDirPath().resolve("job-status.log");
        this.failedLogFile = PipelineConfig.getFailedOutputDirPath().resolve("failed-jobs.log");

        createIfNotExists(jobLogFile);
        createIfNotExists(failedLogFile);
    }

    /**
     * <h3>Create missing log file</h3>
     * Creates the supplied file only when it does not already exist.
     *
     * @param file log file path to create when absent
     *
     * @throws IOException when the file cannot be created
     * @since 1.0
     */
    private void createIfNotExists(Path file) throws IOException {
        if (!Files.exists(file)) {
            Files.createFile(file);
        }
    }

    /**
     * <h3>Save job status</h3>
     * Appends one compact status line for the supplied job.
     * <p>
     * The entry includes timestamp, job id, current status, retry count, source
     * Excel file, and normalized error message.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Builds a single-line status record from the current {@link ExcelJob} state.</li>
     *     <li>Serializes access to the status log with {@code statusLock}.</li>
     *     <li>Logs persistence errors without rethrowing them to the pipeline caller.</li>
     * </ul>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * persistenceService.saveJobStatus(job);
     * }</pre>
     *
     * @param job pipeline job whose current state should be written
     *
     * @apiNote This method appends to the status log and does not replace previous entries for the same job.
     * @since 1.0
     */
    public void saveJobStatus(ExcelJob job) {
        String logLine =
                timestamp()
                        + " | JOB_ID=" + job.getJobId()
                        + " | STATUS=" + job.getStatus()
                        + " | RETRY=" + job.getRetryCount()
                        + " | EXCEL=" + excelFile(job)
                        + " | ERROR=" + safe(job.getErrorMessage());

        // Multiple pipeline stages may report status concurrently; keep each status line atomic.
        synchronized (statusLock) {
            try (BufferedWriter writer = Files.newBufferedWriter(
                    jobLogFile,
                    StandardOpenOption.APPEND
            )) {
                writer.write(logLine);
                writer.newLine();

            } catch (Exception e) {
                LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()),
                        "Failed to save status: {}", e);
            }
        }
    }

    /**
     * <h3>Save successful job</h3>
     * Appends one success line after a job has produced its DOCX output.
     * <p>
     * The entry captures the Excel input path and generated DOCX path so the
     * successful processing artifact can be traced later.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Builds a success record containing job id, Excel file, and DOCX file.</li>
     *     <li>Serializes access to the same status log used by general job status entries.</li>
     *     <li>Logs persistence errors without interrupting caller-side pipeline cleanup.</li>
     * </ul>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * persistenceService.saveSuccessJob(job);
     * }</pre>
     *
     * @param job completed job whose success should be persisted
     *
     * @apiNote This method records DOCX completion but does not change the job status itself.
     * @since 1.0
     */
    public void saveSuccessJob(ExcelJob job) {
        String logLine =
                timestamp()
                        + " | SUCCESS"
                        + " | JOB_ID=" + job.getJobId()
                        + " | EXCEL=" + excelFile(job)
                        + " | DOCX=" + docxFile(job)
                        + " | DOCX_CREATED";

        // Success entries share the job status file, so they use the same status lock.
        synchronized (statusLock) {
            try (BufferedWriter writer = Files.newBufferedWriter(
                    jobLogFile,
                    StandardOpenOption.APPEND
            )) {
                writer.write(logLine);
                writer.newLine();

            } catch (Exception e) {
                LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()),
                        "Failed to save success job: {}", e);
            }
        }
    }

    /**
     * <h3>Save failed job</h3>
     * Appends a detailed failed-job section for the supplied job.
     * <p>
     * The failure section is intended for post-run troubleshooting and includes
     * job metadata plus each image record's row number, URL, download status,
     * and error message.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Writes a delimiter block so each failed job can be visually separated in the log file.</li>
     *     <li>Persists job timestamp, id, status, Excel path, DOCX path, retry count, and error text.</li>
     *     <li>Persists image-level failure details for records attached to the job.</li>
     *     <li>Serializes failed-log writes independently from status-log writes.</li>
     * </ul>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * persistenceService.saveFailedJob(job);
     * }</pre>
     *
     * @param job failed job whose diagnostic details should be persisted
     *
     * @apiNote The method writes all attached image records, not only records whose download status is failed.
     * @since 1.0
     */
    public void saveFailedJob(ExcelJob job) {
        // Failed-job entries are multi-line blocks; the dedicated lock prevents interleaved diagnostics.
        synchronized (failedLock) {
            try (BufferedWriter writer = Files.newBufferedWriter(
                    failedLogFile,
                    StandardOpenOption.APPEND
            )) {
                writer.write("================================================");
                writer.newLine();

                writer.write("TIMESTAMP=" + timestamp());
                writer.newLine();

                writer.write("JOB_ID=" + job.getJobId());
                writer.newLine();

                writer.write("STATUS=" + job.getStatus());
                writer.newLine();

//                writer.write("SOURCE_URL=" + safe(job.getSourceUrl()));
//                writer.newLine();

                writer.write("EXCEL_FILE=" + excelFile(job));
                writer.newLine();

                writer.write("DOCX_FILE=" + docxFile(job));
                writer.newLine();

                writer.write("RETRY_COUNT=" + job.getRetryCount());
                writer.newLine();

                writer.write("ERROR=" + safe(job.getErrorMessage()));
                writer.newLine();

                writer.write("IMAGE_FAILURES");
                writer.newLine();

                for (ImageRecord record : job.getImageRecords()) {
                    writer.write(
                            "ROW=" + record.getRowNumber()
                                    + ", URL=" + safe(record.getImageUrl())
                                    + ", STATUS=" + record.getDownloadStatus()
                                    + ", ERROR=" + safe(record.getErrorMessage())
                    );
                    writer.newLine();
                }

                writer.write("================================================");
                writer.newLine();
                writer.newLine();

            } catch (Exception e) {
                LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()),
                        "Failed to save failed job: {}", e);
            }
        }
    }

    /**
     * <h3>Resolve Excel file text</h3>
     * Converts the job Excel path to log-safe text.
     *
     * @param job job that may contain an Excel file path
     *
     * @return Excel file path text, or an empty value when the job has no Excel file
     * @since 1.0
     */
    private String excelFile(ExcelJob job) {
        return job.getExcelFile() == null
                ? ""
                : job.getExcelFile().toString();
    }

    /**
     * <h3>Resolve DOCX file text</h3>
     * Converts the job DOCX path to log-safe text.
     *
     * @param job job that may contain a DOCX file path
     *
     * @return DOCX file path text, or an empty value when the job has no DOCX file
     * @since 1.0
     */
    private String docxFile(ExcelJob job) {
        return job.getDocxFile() == null
                ? ""
                : job.getDocxFile().toString();
    }

    /**
     * <h3>Normalize log value</h3>
     * Converts nullable text into a single-line value suitable for log records.
     *
     * @param value raw value to persist
     *
     * @return empty string for {@code null}, otherwise the value with line breaks replaced by spaces
     * @since 1.0
     */
    private String safe(String value) {
        if (value == null) {
            return "";
        }

        // Keep one logical field on one physical line so manual log review remains reliable.
        return value
                .replace("\n", " ")
                .replace("\r", " ");
    }

    /**
     * <h3>Create timestamp text</h3>
     * Formats the current local date and time for job persistence logs.
     *
     * @return formatted timestamp using {@code yyyy-MM-dd HH:mm:ss}
     * @since 1.0
     */
    private String timestamp() {
        return LocalDateTime.now().format(TS_FORMAT);
    }
}
