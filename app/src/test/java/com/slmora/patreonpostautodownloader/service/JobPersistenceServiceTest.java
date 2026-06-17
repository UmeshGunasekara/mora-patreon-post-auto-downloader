package com.slmora.patreonpostautodownloader.service;

import com.slmora.patreonpostautodownloader.config.PipelineConfig;
import com.slmora.patreonpostautodownloader.model.DownloadStatus;
import com.slmora.patreonpostautodownloader.model.ExcelJob;
import com.slmora.patreonpostautodownloader.model.JobStatus;
import com.slmora.patreonpostautodownloader.support.ExcelJobTestBuilder;
import com.slmora.patreonpostautodownloader.support.ImageRecordTestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

/**
 * The {@code JobPersistenceServiceTest} test class is created for verifying
 * file-based job persistence behavior implemented by {@link JobPersistenceService}.
 * <p>
 * It focuses on log file creation, compact status entry writing, success entry
 * writing, failed-job diagnostic blocks, and single-line sanitization of
 * persisted error values used by the Patreon post download pipeline.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Verifies required log files are created during service construction.</li>
 *     <li>Verifies job status entries include status, retry count, job id, and sanitized errors.</li>
 *     <li>Verifies successful DOCX jobs are appended to the status log.</li>
 *     <li>Verifies failed-job logs include job metadata and image-level failure details.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link JobPersistenceService}<br>
 * 2 - {@link PipelineConfig}<br>
 * 3 - {@link ExcelJob}<br>
 * 4 - {@link JobStatus}<br>
 * 5 - {@link DownloadStatus}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link JobPersistenceServiceTest#GivenMissingLogFiles_WhenServiceConstructed_ThenRequiredLogFilesAreCreated()}</li>
 *     <li>{@link JobPersistenceServiceTest#GivenJobWithMultilineError_WhenSaveJobStatus_ThenLogContainsSingleLineSanitizedError()}</li>
 *     <li>{@link JobPersistenceServiceTest#GivenSuccessfulJob_WhenSaveSuccessJob_ThenStatusLogContainsSuccessEntry()}</li>
 *     <li>{@link JobPersistenceServiceTest#GivenFailedJobWithImageRecords_WhenSaveFailedJob_ThenFailedLogContainsImageFailureLines()}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>Tests mock {@link PipelineConfig#getFailedOutputDirPath()} so log files are written only under JUnit's {@link TempDir} directory.</li>
 *     <li>The assertions inspect persisted text directly because this service writes human-readable log files.</li>
 * </ul>
 *
 * @author: SLMORA
 * @since 1.0
 *
 * <h4>Revision History</h4>
 * <blockquote><pre>
 * <br>Version      Date            Editor              Note
 * <br>-------------------------------------------------------
 * <br>1.0          6/6/2026       SLMORA              Initial Code
 * </pre></blockquote>
 */
class JobPersistenceServiceTest {

    /**
     * Temporary directory used as the configured failed-output directory for log
     * file fixtures.
     */
    @TempDir
    Path tempDir;

    /**
     * <h3>Create required log files</h3>
     * Verifies that constructing {@link JobPersistenceService} creates
     * {@code job-status.log} and {@code failed-jobs.log} when they do not
     * already exist.
     * <p>
     * This test targets constructor setup before any job status or failure entry
     * is written.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Mocks {@link PipelineConfig#getFailedOutputDirPath()} to return the temporary directory.</li>
     *     <li>Constructs the persistence service.</li>
     *     <li>Asserts both required log files exist.</li>
     * </ul>
     *
     * @throws Exception when service construction or file inspection fails
     * @since 1.0
     *
     * @see JobPersistenceService#JobPersistenceService()
     */
    @Test
    void GivenMissingLogFiles_WhenServiceConstructed_ThenRequiredLogFilesAreCreated() throws Exception {
        // Arrange + Act
        try (MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class)) {
            pipelineConfigMock.when(PipelineConfig::getFailedOutputDirPath).thenReturn(tempDir);
            new JobPersistenceService();

            // Assert
            assertThat(Files.exists(tempDir.resolve("job-status.log"))).isTrue();
            assertThat(Files.exists(tempDir.resolve("failed-jobs.log"))).isTrue();
        }
    }

    /**
     * <h3>Persist sanitized job status line</h3>
     * Verifies that {@link JobPersistenceService#saveJobStatus(ExcelJob)}
     * appends a compact status log entry and normalizes multiline error text
     * into a single physical line.
     * <p>
     * This test targets the status log format used for normal pipeline progress
     * and retry diagnostics.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Builds a job with status, retry count, and newline-delimited error text.</li>
     *     <li>Writes the job status through the persistence service.</li>
     *     <li>Reads {@code job-status.log} from the temporary output directory.</li>
     *     <li>Asserts the entry includes job metadata and a single-line sanitized error value.</li>
     * </ul>
     *
     * @throws Exception when service construction, log writing, or log reading fails
     * @since 1.0
     *
     * @see JobPersistenceService#saveJobStatus(ExcelJob)
     */
    @Test
    void GivenJobWithMultilineError_WhenSaveJobStatus_ThenLogContainsSingleLineSanitizedError() throws Exception {
        // Arrange
        ExcelJob job = ExcelJobTestBuilder.anExcelJob()
                .withStatus(JobStatus.EXCEL_CREATED)
                .withRetryCount(2)
                .withErrorMessage("line1\nline2\rline3")
                .build();

        try (MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class)) {
            pipelineConfigMock.when(PipelineConfig::getFailedOutputDirPath).thenReturn(tempDir);
            JobPersistenceService service = new JobPersistenceService();

            // Act
            service.saveJobStatus(job);

            // Assert
            String content = Files.readString(tempDir.resolve("job-status.log"));
            assertThat(content).contains("JOB_ID=" + job.getJobId());
            assertThat(content).contains("STATUS=EXCEL_CREATED");
            assertThat(content).contains("RETRY=2");
            assertThat(content).contains("ERROR=line1 line2 line3");
        }
    }

    /**
     * <h3>Persist successful job entry</h3>
     * Verifies that {@link JobPersistenceService#saveSuccessJob(ExcelJob)}
     * appends a success entry containing DOCX completion information.
     * <p>
     * This test targets the status log entry written after DOCX generation
     * succeeds.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Builds a job marked {@link JobStatus#DOCX_CREATED} with a DOCX output path.</li>
     *     <li>Writes the success entry through the persistence service.</li>
     *     <li>Reads {@code job-status.log} and verifies success, DOCX path, and completion marker text.</li>
     * </ul>
     *
     * @throws Exception when service construction, log writing, or log reading fails
     * @since 1.0
     *
     * @see JobPersistenceService#saveSuccessJob(ExcelJob)
     */
    @Test
    void GivenSuccessfulJob_WhenSaveSuccessJob_ThenStatusLogContainsSuccessEntry() throws Exception {
        // Arrange
        ExcelJob job = ExcelJobTestBuilder.anExcelJob()
                .withStatus(JobStatus.DOCX_CREATED)
                .withDocxFile(tempDir.resolve("output.docx"))
                .build();

        try (MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class)) {
            pipelineConfigMock.when(PipelineConfig::getFailedOutputDirPath).thenReturn(tempDir);
            JobPersistenceService service = new JobPersistenceService();

            // Act
            service.saveSuccessJob(job);

            // Assert
            String content = Files.readString(tempDir.resolve("job-status.log"));
            assertThat(content).contains("SUCCESS");
            assertThat(content).contains("DOCX_CREATED");
            assertThat(content).contains("DOCX=" + tempDir.resolve("output.docx"));
        }
    }

    /**
     * <h3>Persist failed job details with image failures</h3>
     * Verifies that {@link JobPersistenceService#saveFailedJob(ExcelJob)}
     * appends a detailed failed-job block containing job metadata, sanitized
     * error text, and image-level failure lines.
     * <p>
     * This test targets the failure log format consumed during post-run manual
     * troubleshooting.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Builds a failed job with retry count and multiline error text.</li>
     *     <li>Adds one failed image record with row number, URL, status, and error text.</li>
     *     <li>Writes the failed-job block through the persistence service.</li>
     *     <li>Reads {@code failed-jobs.log} and verifies job-level and image-level diagnostic lines.</li>
     * </ul>
     *
     * @throws Exception when service construction, log writing, or log reading fails
     * @since 1.0
     *
     * @see JobPersistenceService#saveFailedJob(ExcelJob)
     */
    @Test
    void GivenFailedJobWithImageRecords_WhenSaveFailedJob_ThenFailedLogContainsImageFailureLines() throws Exception {
        // Arrange
        ExcelJob job = ExcelJobTestBuilder.anExcelJob()
                .withStatus(JobStatus.FAILED)
                .withRetryCount(3)
                .withErrorMessage("download\nerror")
                .build();
        job.getImageRecords().add(
                ImageRecordTestBuilder.anImageRecord()
                        .withRowNumber(9)
                        .withImageUrl("https://cdn.example.com/pic.jpg")
                        .withDownloadStatus(DownloadStatus.FAILED)
                        .withErrorMessage("socket timeout")
                        .build()
        );

        try (MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class)) {
            pipelineConfigMock.when(PipelineConfig::getFailedOutputDirPath).thenReturn(tempDir);
            JobPersistenceService service = new JobPersistenceService();

            // Act
            service.saveFailedJob(job);

            // Assert
            String content = Files.readString(tempDir.resolve("failed-jobs.log"));
            assertThat(content).contains("JOB_ID=" + job.getJobId());
            assertThat(content).contains("STATUS=FAILED");
            assertThat(content).contains("RETRY_COUNT=3");
            assertThat(content).contains("ERROR=download error");
            assertThat(content).contains("ROW=9, URL=https://cdn.example.com/pic.jpg, STATUS=FAILED, ERROR=socket timeout");
        }
    }
}

