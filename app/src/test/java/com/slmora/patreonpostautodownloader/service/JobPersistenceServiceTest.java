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

class JobPersistenceServiceTest {

    @TempDir
    Path tempDir;

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

