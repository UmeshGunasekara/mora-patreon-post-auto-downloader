package com.slmora.patreonpostautodownloader.process;

import com.slmora.patreonpostautodownloader.config.PipelineConfig;
import com.slmora.patreonpostautodownloader.model.DownloadStatus;
import com.slmora.patreonpostautodownloader.model.ExcelJob;
import com.slmora.patreonpostautodownloader.model.ImageRecord;
import com.slmora.patreonpostautodownloader.model.JobStatus;
import com.slmora.patreonpostautodownloader.pipeline.PipelineQueues;
import com.slmora.patreonpostautodownloader.pipeline.PipelineState;
import com.slmora.patreonpostautodownloader.service.ImageDownloadService;
import com.slmora.patreonpostautodownloader.service.RetryService;
import com.slmora.patreonpostautodownloader.support.ExcelJobTestBuilder;
import com.slmora.patreonpostautodownloader.support.ImageRecordTestBuilder;
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
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessRetryTest {

    @Mock
    private PipelineQueues pipelineQueues;

    @Mock
    private BlockingQueue<ExcelJob> retryQueue;

    @Mock
    private BlockingQueue<ExcelJob> docxReadyQueue;

    @Mock
    private ImageDownloadService imageDownloadService;

    @Mock
    private RetryService retryService;

    @Test
    void GivenRetryQueueEmptyAndUpstreamFinished_WhenStart_ThenRetryProcessStopsAndSetsFinishedFlag() throws InterruptedException {
        // Arrange
        PipelineState pipelineState = new PipelineState();
        pipelineState.setProcessImageDownloadWorkerFinished(true);
        when(pipelineQueues.retryQueue()).thenReturn(retryQueue);
        when(retryQueue.poll(anyLong(), eq(TimeUnit.SECONDS))).thenReturn(null);
        when(retryQueue.isEmpty()).thenReturn(true);

        ProcessRetry processRetry = new ProcessRetry(pipelineQueues, pipelineState, imageDownloadService, retryService);

        // Act
        processRetry.start();

        // Assert
        assertThat(pipelineState.isProcessRetryFinished()).isTrue();
        verify(retryQueue, atLeast(1)).poll(anyLong(), eq(TimeUnit.SECONDS));
        verifyNoInteractions(imageDownloadService);
    }

    @Test
    void GivenRetrySucceedsWithoutFailedImages_WhenStart_ThenJobMovesToDocxQueue() throws Exception {
        // Arrange
        ExcelJob excelJob = jobWithStatuses(DownloadStatus.SUCCESS, DownloadStatus.SUCCESS);
        PipelineState pipelineState = new PipelineState();
        pipelineState.setProcessImageDownloadWorkerFinished(true);

        when(pipelineQueues.retryQueue()).thenReturn(retryQueue);
        when(pipelineQueues.docxReadyQueue()).thenReturn(docxReadyQueue);
        when(retryQueue.poll(anyLong(), eq(TimeUnit.SECONDS))).thenReturn(excelJob).thenReturn(null);
        when(retryQueue.isEmpty()).thenReturn(true);

        try (MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class)) {
            pipelineConfigMock.when(PipelineConfig::getImageOutputDirPath).thenReturn(Path.of("C:/tmp/images"));
            ProcessRetry processRetry = new ProcessRetry(pipelineQueues, pipelineState, imageDownloadService, retryService);

            // Act
            processRetry.start();

            // Assert
            assertThat(excelJob.getStatus()).isEqualTo(JobStatus.IMAGES_DOWNLOADED);
            verify(docxReadyQueue, times(1)).put(excelJob);
            verify(retryService, never()).sendToRetryOrFailed(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
        }
    }

    @Test
    void GivenRetryStillHasFailedImages_WhenStart_ThenJobIsSentToRetryService() throws Exception {
        // Arrange
        ExcelJob excelJob = jobWithStatuses(DownloadStatus.SUCCESS, DownloadStatus.FAILED);
        PipelineState pipelineState = new PipelineState();
        pipelineState.setProcessImageDownloadWorkerFinished(true);

        when(pipelineQueues.retryQueue()).thenReturn(retryQueue);
        when(retryQueue.poll(anyLong(), eq(TimeUnit.SECONDS))).thenReturn(excelJob).thenReturn(null);
        when(retryQueue.isEmpty()).thenReturn(true);

        try (MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class)) {
            pipelineConfigMock.when(PipelineConfig::getImageOutputDirPath).thenReturn(Path.of("C:/tmp/images"));
            ProcessRetry processRetry = new ProcessRetry(pipelineQueues, pipelineState, imageDownloadService, retryService);

            // Act
            processRetry.start();

            // Assert
            verify(retryService, times(1))
                    .sendToRetryOrFailed(excelJob, "Retry failed. There are some images has not been downloaded successfully");
            verify(docxReadyQueue, never()).put(excelJob);
        }
    }

    @Test
    void GivenRetryServiceThrowsException_WhenStart_ThenJobIsRoutedToRetryServiceWithErrorMessage() throws Exception {
        // Arrange
        ExcelJob excelJob = jobWithStatuses(DownloadStatus.PENDING);
        PipelineState pipelineState = new PipelineState();
        pipelineState.setProcessImageDownloadWorkerFinished(true);

        when(pipelineQueues.retryQueue()).thenReturn(retryQueue);
        when(retryQueue.poll(anyLong(), eq(TimeUnit.SECONDS))).thenReturn(excelJob).thenReturn(null);
        when(retryQueue.isEmpty()).thenReturn(true);

        doThrow(new RuntimeException("download exploded"))
                .when(imageDownloadService)
                .retryFailedImages(eq(excelJob), org.mockito.ArgumentMatchers.any(Path.class));

        try (MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class)) {
            pipelineConfigMock.when(PipelineConfig::getImageOutputDirPath).thenReturn(Path.of("C:/tmp/images"));
            ProcessRetry processRetry = new ProcessRetry(pipelineQueues, pipelineState, imageDownloadService, retryService);

            // Act
            processRetry.start();

            // Assert
            verify(retryService, times(1)).sendToRetryOrFailed(excelJob, "download exploded");
        }
    }

    @Test
    void GivenRetryPollInterruptedOnce_WhenStart_ThenLoopContinuesAndFinishes() throws Exception {
        // Arrange
        PipelineState pipelineState = new PipelineState();
        pipelineState.setProcessImageDownloadWorkerFinished(true);

        when(pipelineQueues.retryQueue()).thenReturn(retryQueue);
        when(retryQueue.poll(anyLong(), eq(TimeUnit.SECONDS)))
                .thenThrow(new InterruptedException("interrupted"))
                .thenReturn(null);
        when(retryQueue.isEmpty()).thenReturn(true);

        ProcessRetry processRetry = new ProcessRetry(pipelineQueues, pipelineState, imageDownloadService, retryService);

        // Act
        processRetry.start();

        // Assert
        assertThat(pipelineState.isProcessRetryFinished()).isTrue();
        verify(retryQueue, atLeast(2)).poll(anyLong(), eq(TimeUnit.SECONDS));
        verifyNoInteractions(imageDownloadService, retryService);
    }

    @Test
    void GivenRetryQueueEmptyInitiallyAndUpstreamLaterFinishes_WhenStart_ThenLoopContinuesThenStops() throws Exception {
        // Arrange
        PipelineState pipelineState = new PipelineState();

        when(pipelineQueues.retryQueue()).thenReturn(retryQueue);
        when(retryQueue.poll(anyLong(), eq(TimeUnit.SECONDS)))
                .thenReturn(null)
                .thenAnswer(invocation -> {
                    pipelineState.setProcessImageDownloadWorkerFinished(true);
                    return null;
                });
        when(retryQueue.isEmpty()).thenReturn(true);

        ProcessRetry processRetry = new ProcessRetry(pipelineQueues, pipelineState, imageDownloadService, retryService);

        // Act
        processRetry.start();

        // Assert
        assertThat(pipelineState.isProcessRetryFinished()).isTrue();
        verify(retryQueue, atLeast(2)).poll(anyLong(), eq(TimeUnit.SECONDS));
        verifyNoInteractions(imageDownloadService, retryService);
    }

    private ExcelJob jobWithStatuses(DownloadStatus... statuses) {
        ExcelJob excelJob = ExcelJobTestBuilder.anExcelJob().build();
        for (int i = 0; i < statuses.length; i++) {
            ImageRecord imageRecord = ImageRecordTestBuilder.anImageRecord()
                    .withRowNumber(i + 1)
                    .withDownloadStatus(statuses[i])
                    .build();
            excelJob.getImageRecords().add(imageRecord);
        }
        return excelJob;
    }
}




