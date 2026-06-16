package com.slmora.patreonpostautodownloader.process;

import com.slmora.patreonpostautodownloader.config.PipelineConfig;
import com.slmora.patreonpostautodownloader.model.ExcelJob;
import com.slmora.patreonpostautodownloader.model.JobStatus;
import com.slmora.patreonpostautodownloader.pipeline.PipelineQueues;
import com.slmora.patreonpostautodownloader.pipeline.PipelineState;
import com.slmora.patreonpostautodownloader.service.CleanupService;
import com.slmora.patreonpostautodownloader.service.DocxService;
import com.slmora.patreonpostautodownloader.service.JobPersistenceService;
import com.slmora.patreonpostautodownloader.support.ExcelJobTestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessDocxProducerTest {

    @Mock
    private PipelineQueues pipelineQueues;

    @Mock
    private BlockingQueue<ExcelJob> docxReadyQueue;

    @Mock
    private BlockingQueue<ExcelJob> failedQueue;

    @Mock
    private DocxService docxService;

    @Mock
    private CleanupService cleanupService;

    @Mock
    private JobPersistenceService jobPersistenceService;

    @Test
    void GivenDocxQueueEmptyAndUpstreamFinished_WhenStart_ThenProducerStopsAndMarksFinished() throws Exception {
        // Arrange
        PipelineState pipelineState = new PipelineState();
        pipelineState.setProcessImageDownloadWorkerFinished(true);
        pipelineState.setProcessRetryFinished(true);

        when(pipelineQueues.docxReadyQueue()).thenReturn(docxReadyQueue);
        when(docxReadyQueue.poll(anyLong(), eq(TimeUnit.SECONDS))).thenReturn(null);
        when(docxReadyQueue.isEmpty()).thenReturn(true);

        try (MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class)) {
            pipelineConfigMock.when(PipelineConfig::getProcessDocxThreads).thenReturn(1);
            ProcessDocxProducer processDocxProducer = new ProcessDocxProducer(
                    pipelineQueues,
                    pipelineState,
                    docxService,
                    cleanupService,
                    jobPersistenceService
            );

            // Act
            processDocxProducer.start();

            // Assert
            assertThat(pipelineState.isProcessDocxProducerFinished()).isTrue();
            verify(docxReadyQueue, times(1)).poll(anyLong(), eq(TimeUnit.SECONDS));
            verify(docxService, never()).createDocx(
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anyString()
            );
        }
    }

    @Test
    void GivenDocxCreationSucceeds_WhenStart_ThenSuccessIsPersistedAndCleanupRuns() throws Exception {
        // Arrange
        ExcelJob excelJob = ExcelJobTestBuilder.anExcelJob().build();
        PipelineState pipelineState = new PipelineState();
        pipelineState.setProcessImageDownloadWorkerFinished(true);
        pipelineState.setProcessRetryFinished(true);

        when(pipelineQueues.docxReadyQueue()).thenReturn(docxReadyQueue);
        when(docxReadyQueue.poll(anyLong(), eq(TimeUnit.SECONDS))).thenReturn(excelJob).thenReturn(null);
        when(docxReadyQueue.isEmpty()).thenReturn(true);

        try (MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class)) {
            pipelineConfigMock.when(PipelineConfig::getProcessDocxThreads).thenReturn(1);
            pipelineConfigMock.when(PipelineConfig::getDocxOutputDirPath).thenReturn(Path.of("C:/tmp/docx"));
            pipelineConfigMock.when(PipelineConfig::getDocxPostFileNamePattern).thenReturn("patreon_posts_output(.*)\\.xlsx");
            pipelineConfigMock.when(PipelineConfig::getDocxPostFileName).thenReturn("patreon_posts_report_temp.docx");
            pipelineConfigMock.when(PipelineConfig::getExcelPostSheetName).thenReturn("Posts");

            ProcessDocxProducer processDocxProducer = new ProcessDocxProducer(
                    pipelineQueues,
                    pipelineState,
                    docxService,
                    cleanupService,
                    jobPersistenceService
            );

            // Act
            processDocxProducer.start();

            // Assert
            assertThat(excelJob.getStatus()).isEqualTo(JobStatus.DOCX_CREATED);
            verify(docxService, times(1)).createDocx(
                    eq(excelJob),
                    org.mockito.ArgumentMatchers.any(Path.class),
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anyString()
            );
            verify(jobPersistenceService, times(1)).saveSuccessJob(excelJob);
            verify(cleanupService, times(1)).cleanupAfterSuccess(excelJob);
        }
    }

    @Test
    void GivenDocxCreationFails_WhenStart_ThenJobMovesToFailedQueueAndFailureIsPersisted() throws Exception {
        // Arrange
        ExcelJob excelJob = ExcelJobTestBuilder.anExcelJob().build();
        PipelineState pipelineState = new PipelineState();
        pipelineState.setProcessImageDownloadWorkerFinished(true);
        pipelineState.setProcessRetryFinished(true);

        when(pipelineQueues.docxReadyQueue()).thenReturn(docxReadyQueue);
        when(pipelineQueues.failedQueue()).thenReturn(failedQueue);
        when(docxReadyQueue.poll(anyLong(), eq(TimeUnit.SECONDS))).thenReturn(excelJob).thenReturn(null);
        when(docxReadyQueue.isEmpty()).thenReturn(true);

        doThrow(new RuntimeException("docx conversion failed"))
                .when(docxService)
                .createDocx(
                        eq(excelJob),
                        org.mockito.ArgumentMatchers.any(Path.class),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString()
                );

        try (MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class)) {
            pipelineConfigMock.when(PipelineConfig::getProcessDocxThreads).thenReturn(1);
            pipelineConfigMock.when(PipelineConfig::getDocxOutputDirPath).thenReturn(Path.of("C:/tmp/docx"));
            pipelineConfigMock.when(PipelineConfig::getDocxPostFileNamePattern).thenReturn("patreon_posts_output(.*)\\.xlsx");
            pipelineConfigMock.when(PipelineConfig::getDocxPostFileName).thenReturn("patreon_posts_report_temp.docx");
            pipelineConfigMock.when(PipelineConfig::getExcelPostSheetName).thenReturn("Posts");

            ProcessDocxProducer processDocxProducer = new ProcessDocxProducer(
                    pipelineQueues,
                    pipelineState,
                    docxService,
                    cleanupService,
                    jobPersistenceService
            );

            // Act
            processDocxProducer.start();

            // Assert
            assertThat(excelJob.getStatus()).isEqualTo(JobStatus.FAILED);
            assertThat(excelJob.getErrorMessage()).contains("DOCX failed");
            verify(failedQueue, times(1)).put(excelJob);
            verify(jobPersistenceService, times(1)).saveFailedJob(excelJob);
            verify(jobPersistenceService, never()).saveSuccessJob(excelJob);
            verify(cleanupService, never()).cleanupAfterSuccess(excelJob);
        }
    }

    @Test
    void GivenDocxQueueEmptyInitiallyAndUpstreamLaterFinishes_WhenStart_ThenProducerKeepsPollingAndStops() throws Exception {
        // Arrange
        PipelineState pipelineState = new PipelineState();

        when(pipelineQueues.docxReadyQueue()).thenReturn(docxReadyQueue);
        when(docxReadyQueue.poll(anyLong(), eq(TimeUnit.SECONDS)))
                .thenReturn(null)
                .thenAnswer(invocation -> {
                    pipelineState.setProcessImageDownloadWorkerFinished(true);
                    pipelineState.setProcessRetryFinished(true);
                    return null;
                });
        when(docxReadyQueue.isEmpty()).thenReturn(true);

        try (MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class)) {
            pipelineConfigMock.when(PipelineConfig::getProcessDocxThreads).thenReturn(1);

            ProcessDocxProducer processDocxProducer = new ProcessDocxProducer(
                    pipelineQueues,
                    pipelineState,
                    docxService,
                    cleanupService,
                    jobPersistenceService
            );

            // Act
            processDocxProducer.start();

            // Assert
            assertThat(pipelineState.isProcessDocxProducerFinished()).isTrue();
            verify(docxReadyQueue, atLeast(2)).poll(anyLong(), eq(TimeUnit.SECONDS));
            verify(docxService, never()).createDocx(
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anyString()
            );
        }
    }

    @Test
    void GivenDocxCreationFailsAndFailedQueuePutInterrupted_WhenStart_ThenFailurePersistenceIsSkipped() throws Exception {
        // Arrange
        ExcelJob excelJob = ExcelJobTestBuilder.anExcelJob().build();
        PipelineState pipelineState = new PipelineState();
        pipelineState.setProcessImageDownloadWorkerFinished(true);
        pipelineState.setProcessRetryFinished(true);

        when(pipelineQueues.docxReadyQueue()).thenReturn(docxReadyQueue);
        when(pipelineQueues.failedQueue()).thenReturn(failedQueue);
        when(docxReadyQueue.poll(anyLong(), eq(TimeUnit.SECONDS))).thenReturn(excelJob).thenReturn(null);
        when(docxReadyQueue.isEmpty()).thenReturn(true);

        doThrow(new RuntimeException("docx conversion failed"))
                .when(docxService)
                .createDocx(
                        eq(excelJob),
                        org.mockito.ArgumentMatchers.any(Path.class),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString()
                );
        doThrow(new InterruptedException("failed queue interrupted"))
                .when(failedQueue)
                .put(excelJob);

        try (MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class)) {
            pipelineConfigMock.when(PipelineConfig::getProcessDocxThreads).thenReturn(1);
            pipelineConfigMock.when(PipelineConfig::getDocxOutputDirPath).thenReturn(Path.of("C:/tmp/docx"));
            pipelineConfigMock.when(PipelineConfig::getDocxPostFileNamePattern).thenReturn("patreon_posts_output(.*)\\.xlsx");
            pipelineConfigMock.when(PipelineConfig::getDocxPostFileName).thenReturn("patreon_posts_report_temp.docx");
            pipelineConfigMock.when(PipelineConfig::getExcelPostSheetName).thenReturn("Posts");

            ProcessDocxProducer processDocxProducer = new ProcessDocxProducer(
                    pipelineQueues,
                    pipelineState,
                    docxService,
                    cleanupService,
                    jobPersistenceService
            );

            // Act
            processDocxProducer.start();

            // Assert
            assertThat(excelJob.getStatus()).isEqualTo(JobStatus.FAILED);
            verify(failedQueue, times(1)).put(excelJob);
            verify(jobPersistenceService, never()).saveFailedJob(excelJob);
        }
    }
}



