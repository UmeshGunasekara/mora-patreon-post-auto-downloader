package com.slmora.patreonpostautodownloader.process;

import com.slmora.patreonpostautodownloader.model.ExcelJob;
import com.slmora.patreonpostautodownloader.pipeline.PipelineQueues;
import com.slmora.patreonpostautodownloader.pipeline.PipelineState;
import com.slmora.patreonpostautodownloader.service.JobPersistenceService;
import com.slmora.patreonpostautodownloader.support.ExcelJobTestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FailedJobMonitorTest {

    @Mock
    private PipelineQueues pipelineQueues;

    @Mock
    private BlockingQueue<ExcelJob> failedQueue;

    @Mock
    private JobPersistenceService jobPersistenceService;

    private PipelineState pipelineState;

    @BeforeEach
    void setUp() {
        pipelineState = new PipelineState();
        pipelineState.setProcessImageDownloadWorkerFinished(true);
        pipelineState.setProcessRetryFinished(true);
        pipelineState.setProcessDocxProducerFinished(true);
        when(pipelineQueues.failedQueue()).thenReturn(failedQueue);
    }

    @Test
    void GivenNoFailedJobsAndAllUpstreamFinished_WhenStart_ThenMonitorStopsWithoutPersisting() throws InterruptedException {
        // Arrange
        when(failedQueue.poll(anyLong(), eq(TimeUnit.SECONDS))).thenReturn(null);
        when(failedQueue.isEmpty()).thenReturn(true);
        FailedJobMonitor failedJobMonitor = new FailedJobMonitor(pipelineQueues, pipelineState, jobPersistenceService);

        // Act
        failedJobMonitor.start();

        // Assert
        verify(jobPersistenceService, never()).saveFailedJob(org.mockito.ArgumentMatchers.any());
        verify(failedQueue, atLeast(1)).poll(anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    void GivenFailedJobExists_WhenStart_ThenJobIsPersistedBeforeStop() throws InterruptedException {
        // Arrange
        ExcelJob failedJob = ExcelJobTestBuilder.anExcelJob().build();
        when(failedQueue.poll(anyLong(), eq(TimeUnit.SECONDS))).thenReturn(failedJob).thenReturn(null);
        when(failedQueue.isEmpty()).thenReturn(false, true);
        FailedJobMonitor failedJobMonitor = new FailedJobMonitor(pipelineQueues, pipelineState, jobPersistenceService);

        // Act
        failedJobMonitor.start();

        // Assert
        verify(jobPersistenceService, times(1)).saveFailedJob(failedJob);
        verify(failedQueue, atLeast(2)).poll(anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    void GivenPersistenceThrowsException_WhenStart_ThenMonitorContinuesAndStopsGracefully() throws InterruptedException {
        // Arrange
        ExcelJob failedJob = ExcelJobTestBuilder.anExcelJob().build();
        when(failedQueue.poll(anyLong(), eq(TimeUnit.SECONDS))).thenReturn(failedJob).thenReturn(null);
        when(failedQueue.isEmpty()).thenReturn(false, true);
        org.mockito.Mockito.doThrow(new RuntimeException("write failed"))
                .when(jobPersistenceService)
                .saveFailedJob(failedJob);
        FailedJobMonitor failedJobMonitor = new FailedJobMonitor(pipelineQueues, pipelineState, jobPersistenceService);

        // Act
        failedJobMonitor.start();

        // Assert
        verify(jobPersistenceService, times(1)).saveFailedJob(failedJob);
        verify(failedQueue, atLeast(2)).poll(anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    void GivenPollThrowsExceptionOnce_WhenStart_ThenMonitorRecoversAndStops() throws InterruptedException {
        // Arrange
        when(failedQueue.poll(anyLong(), eq(TimeUnit.SECONDS)))
                .thenThrow(new RuntimeException("poll failed"))
                .thenReturn(null);
        when(failedQueue.isEmpty()).thenReturn(true);
        FailedJobMonitor failedJobMonitor = new FailedJobMonitor(pipelineQueues, pipelineState, jobPersistenceService);

        // Act
        failedJobMonitor.start();

        // Assert
        verify(jobPersistenceService, never()).saveFailedJob(org.mockito.ArgumentMatchers.any());
        verify(failedQueue, atLeast(2)).poll(anyLong(), eq(TimeUnit.SECONDS));
    }
}



