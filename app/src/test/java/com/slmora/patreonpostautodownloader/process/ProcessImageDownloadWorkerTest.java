package com.slmora.patreonpostautodownloader.process;

import com.slmora.patreonpostautodownloader.config.PipelineConfig;
import com.slmora.patreonpostautodownloader.model.DownloadStatus;
import com.slmora.patreonpostautodownloader.model.ExcelJob;
import com.slmora.patreonpostautodownloader.model.ImageRecord;
import com.slmora.patreonpostautodownloader.model.JobStatus;
import com.slmora.patreonpostautodownloader.pipeline.PipelineQueues;
import com.slmora.patreonpostautodownloader.pipeline.PipelineState;
import com.slmora.patreonpostautodownloader.service.ExcelService;
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
import java.util.List;
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
class ProcessImageDownloadWorkerTest {

    @Mock
    private PipelineQueues pipelineQueues;

    @Mock
    private BlockingQueue<ExcelJob> excelReadyQueue;

    @Mock
    private BlockingQueue<ExcelJob> docxReadyQueue;

    @Mock
    private ExcelService excelService;

    @Mock
    private ImageDownloadService imageDownloadService;

    @Mock
    private RetryService retryService;

    @Test
    void GivenQueueEmptyAndProducerFinished_WhenStart_ThenWorkerStopsAndSetsFinishedFlag() throws Exception {
        // Arrange
        PipelineState pipelineState = new PipelineState();
        pipelineState.setProcessExcelProducerFinished(true);

        when(pipelineQueues.excelReadyQueue()).thenReturn(excelReadyQueue);
        when(excelReadyQueue.poll(anyLong(), eq(TimeUnit.SECONDS))).thenReturn(null);
        when(excelReadyQueue.isEmpty()).thenReturn(true);

        try (MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class)) {
            pipelineConfigMock.when(PipelineConfig::getProcessImageDownloadThreads).thenReturn(1);
            ProcessImageDownloadWorker worker = new ProcessImageDownloadWorker(
                    pipelineQueues,
                    pipelineState,
                    excelService,
                    imageDownloadService,
                    retryService
            );

            // Act
            worker.start();

            // Assert
            assertThat(pipelineState.isProcessImageDownloadWorkerFinished()).isTrue();
            verify(excelReadyQueue, atLeast(1)).poll(anyLong(), eq(TimeUnit.SECONDS));
            verifyNoInteractions(excelService, imageDownloadService, retryService);
        }
    }

    @Test
    void GivenAllImagesDownloaded_WhenStart_ThenJobMovesToDocxQueue() throws Exception {
        // Arrange
        ExcelJob excelJob = ExcelJobTestBuilder.anExcelJob().build();
        PipelineState pipelineState = new PipelineState();
        pipelineState.setProcessExcelProducerFinished(true);

        ImageRecord success1 = ImageRecordTestBuilder.anImageRecord().withDownloadStatus(DownloadStatus.SUCCESS).build();
        ImageRecord success2 = ImageRecordTestBuilder.anImageRecord().withRowNumber(2).withDownloadStatus(DownloadStatus.SUCCESS).build();

        when(pipelineQueues.excelReadyQueue()).thenReturn(excelReadyQueue);
        when(pipelineQueues.docxReadyQueue()).thenReturn(docxReadyQueue);
        when(excelReadyQueue.poll(anyLong(), eq(TimeUnit.SECONDS))).thenReturn(excelJob).thenReturn(null);
        when(excelReadyQueue.isEmpty()).thenReturn(true);
        when(excelService.readImageRecords(eq(excelJob.getExcelFile()), eq("Posts"))).thenReturn(List.of(success1, success2));

        try (MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class)) {
            pipelineConfigMock.when(PipelineConfig::getProcessImageDownloadThreads).thenReturn(1);
            pipelineConfigMock.when(PipelineConfig::getExcelPostSheetName).thenReturn("Posts");
            pipelineConfigMock.when(PipelineConfig::getImageOutputDirPath).thenReturn(Path.of("C:/tmp/images"));

            ProcessImageDownloadWorker worker = new ProcessImageDownloadWorker(
                    pipelineQueues,
                    pipelineState,
                    excelService,
                    imageDownloadService,
                    retryService
            );

            // Act
            worker.start();

            // Assert
            assertThat(excelJob.getStatus()).isEqualTo(JobStatus.IMAGES_DOWNLOADED);
            verify(docxReadyQueue, times(1)).put(excelJob);
            verify(retryService, never()).sendToRetryOrFailed(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
        }
    }

    @Test
    void GivenFailedImageExists_WhenStart_ThenJobIsSentToRetryService() throws Exception {
        // Arrange
        ExcelJob excelJob = ExcelJobTestBuilder.anExcelJob().build();
        PipelineState pipelineState = new PipelineState();
        pipelineState.setProcessExcelProducerFinished(true);

        ImageRecord failed = ImageRecordTestBuilder.anImageRecord().withDownloadStatus(DownloadStatus.FAILED).build();

        when(pipelineQueues.excelReadyQueue()).thenReturn(excelReadyQueue);
        when(excelReadyQueue.poll(anyLong(), eq(TimeUnit.SECONDS))).thenReturn(excelJob).thenReturn(null);
        when(excelReadyQueue.isEmpty()).thenReturn(true);
        when(excelService.readImageRecords(eq(excelJob.getExcelFile()), eq("Posts"))).thenReturn(List.of(failed));

        try (MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class)) {
            pipelineConfigMock.when(PipelineConfig::getProcessImageDownloadThreads).thenReturn(1);
            pipelineConfigMock.when(PipelineConfig::getExcelPostSheetName).thenReturn("Posts");
            pipelineConfigMock.when(PipelineConfig::getImageOutputDirPath).thenReturn(Path.of("C:/tmp/images"));

            ProcessImageDownloadWorker worker = new ProcessImageDownloadWorker(
                    pipelineQueues,
                    pipelineState,
                    excelService,
                    imageDownloadService,
                    retryService
            );

            // Act
            worker.start();

            // Assert
            verify(retryService, times(1))
                    .sendToRetryOrFailed(excelJob, "There are some images has not been downloaded successfully");
        }
    }

    @Test
    void GivenProcessingThrowsException_WhenStart_ThenErrorIsSentToRetryService() throws Exception {
        // Arrange
        ExcelJob excelJob = ExcelJobTestBuilder.anExcelJob().build();
        PipelineState pipelineState = new PipelineState();
        pipelineState.setProcessExcelProducerFinished(true);

        when(pipelineQueues.excelReadyQueue()).thenReturn(excelReadyQueue);
        when(excelReadyQueue.poll(anyLong(), eq(TimeUnit.SECONDS))).thenReturn(excelJob).thenReturn(null);
        when(excelReadyQueue.isEmpty()).thenReturn(true);
        doThrow(new RuntimeException("excel parse failed"))
                .when(excelService)
                .readImageRecords(eq(excelJob.getExcelFile()), eq("Posts"));

        try (MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class)) {
            pipelineConfigMock.when(PipelineConfig::getProcessImageDownloadThreads).thenReturn(1);
            pipelineConfigMock.when(PipelineConfig::getExcelPostSheetName).thenReturn("Posts");
            pipelineConfigMock.when(PipelineConfig::getImageOutputDirPath).thenReturn(Path.of("C:/tmp/images"));

            ProcessImageDownloadWorker worker = new ProcessImageDownloadWorker(
                    pipelineQueues,
                    pipelineState,
                    excelService,
                    imageDownloadService,
                    retryService
            );

            // Act
            worker.start();

            // Assert
            verify(retryService, times(1)).sendToRetryOrFailed(excelJob, "excel parse failed");
        }
    }

    @Test
    void GivenPollInterruptedOnce_WhenStart_ThenWorkerContinuesAndFinishesOnNextEmptyPoll() throws Exception {
        // Arrange
        PipelineState pipelineState = new PipelineState();
        pipelineState.setProcessExcelProducerFinished(true);

        when(pipelineQueues.excelReadyQueue()).thenReturn(excelReadyQueue);
        when(excelReadyQueue.poll(anyLong(), eq(TimeUnit.SECONDS)))
                .thenThrow(new InterruptedException("interrupted"))
                .thenReturn(null);
        when(excelReadyQueue.isEmpty()).thenReturn(true);

        try (MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class)) {
            pipelineConfigMock.when(PipelineConfig::getProcessImageDownloadThreads).thenReturn(1);
            ProcessImageDownloadWorker worker = new ProcessImageDownloadWorker(
                    pipelineQueues,
                    pipelineState,
                    excelService,
                    imageDownloadService,
                    retryService
            );

            // Act
            worker.start();

            // Assert
            assertThat(pipelineState.isProcessImageDownloadWorkerFinished()).isTrue();
            verify(excelReadyQueue, atLeast(2)).poll(anyLong(), eq(TimeUnit.SECONDS));
            verifyNoInteractions(excelService, imageDownloadService, retryService);
        }
    }
}

