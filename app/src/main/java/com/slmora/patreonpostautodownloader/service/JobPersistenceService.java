/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 3:46 PM
 */
package com.slmora.patreonpostautodownloader.service;

import com.slmora.common.logging.MoraLoggerThreadInfo;
import com.slmora.patreonpostautodownloader.config.PipelineConfig;
import com.slmora.patreonpostautodownloader.model.ExcelJob;
import com.slmora.patreonpostautodownloader.model.ImageRecord;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The {@code JobPersistenceService} Class created for
 * <h4>Key Features</h4>
 * <ul>
 *      <li>...</li>
 * </ul>
 * <h4>Codes</h4>
 * 1 - {@link }<br>
 * <h4>Methods</h4>
 * <ul>
 *      <li>{@link }</li>
 * </ul>
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>....</li>
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
    private final Path jobLogFile;
    private final Path failedLogFile;

    private final Object statusLock = new Object();
    private final Object failedLock = new Object();

    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public JobPersistenceService() throws IOException
    {
        this.jobLogFile = PipelineConfig.getFailedOutputDirPath().resolve("job-status.log");
        this.failedLogFile = PipelineConfig.getFailedOutputDirPath().resolve("failed-jobs.log");

        createIfNotExists(jobLogFile);
        createIfNotExists(failedLogFile);
    }

    private void createIfNotExists(Path file) throws IOException {
        if (!Files.exists(file)) {
            Files.createFile(file);
        }
    }

    public void saveJobStatus(ExcelJob job) {
        String logLine =
                timestamp()
                        + " | JOB_ID=" + job.getJobId()
                        + " | STATUS=" + job.getStatus()
                        + " | RETRY=" + job.getRetryCount()
                        + " | EXCEL=" + excelFile(job)
                        + " | ERROR=" + safe(job.getErrorMessage());

        synchronized (statusLock) {
            try (BufferedWriter writer = Files.newBufferedWriter(
                    jobLogFile,
                    StandardOpenOption.APPEND
            )) {
                writer.write(logLine);
                writer.newLine();

            } catch (Exception e) {
                System.err.println("Failed to save status: " + e.getMessage());
            }
        }
    }

    public void saveSuccessJob(ExcelJob job) {
        String logLine =
                timestamp()
                        + " | SUCCESS"
                        + " | JOB_ID=" + job.getJobId()
                        + " | EXCEL=" + excelFile(job)
                        + " | DOCX_CREATED";

        synchronized (statusLock) {
            try (BufferedWriter writer = Files.newBufferedWriter(
                    jobLogFile,
                    StandardOpenOption.APPEND
            )) {
                writer.write(logLine);
                writer.newLine();

            } catch (Exception e) {
                System.err.println("Failed to save success job: " + e.getMessage());
            }
        }
    }

    public void saveFailedJob(ExcelJob job) {
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
                System.err.println("Failed to save failed job: " + e.getMessage());
            }
        }
    }

    private String excelFile(ExcelJob job) {
        return job.getExcelFile() == null
                ? ""
                : job.getExcelFile().toString();
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\n", " ")
                .replace("\r", " ");
    }

    private String timestamp() {
        return LocalDateTime.now().format(TS_FORMAT);
    }

    private static MoraLoggerThreadInfo threadInfo() {
        Thread t = Thread.currentThread();
        return new MoraLoggerThreadInfo(t.getName(), t.threadId(), t.getStackTrace());
    }
}
