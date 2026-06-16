package com.slmora.patreonpostautodownloader.service;

import com.slmora.patreonpostautodownloader.config.PipelineConfig;
import com.slmora.patreonpostautodownloader.model.ExcelJob;
import com.slmora.patreonpostautodownloader.model.JobStatus;
import com.slmora.patreonpostautodownloader.pipeline.PipelineQueues;
import com.slmora.patreonpostautodownloader.support.ExcelJobTestBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.BlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetryServiceTest {

    @Mock
    private PipelineQueues pipelineQueues;

    @Mock
    private BlockingQueue<ExcelJob> retryQueue;

    @Mock
    private BlockingQueue<ExcelJob> failedQueue;

    @AfterEach
    void clearInterruptedStatus() {
        Thread.interrupted();
    }

    @Test
    void GivenNullJob_WhenSendToRetryOrFailed_ThenNullPointerExceptionIsThrown() {
        // Arrange
        RetryService retryService = new RetryService(pipelineQueues);

        // Act + Assert
        assertThatThrownBy(() -> retryService.sendToRetryOrFailed(null, "null-job"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void GivenRetryCountLowerThanMax_WhenSendToRetryOrFailed_ThenJobMovesToRetryQueue() throws InterruptedException {
        // Arrange
        ExcelJob excelJob = ExcelJobTestBuilder.anExcelJob().withRetryCount(0).build();
        when(pipelineQueues.retryQueue()).thenReturn(retryQueue);

        try (MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class)) {
            pipelineConfigMock.when(PipelineConfig::getMaxRetry).thenReturn(3);
            RetryService retryService = new RetryService(pipelineQueues);

            // Act
            retryService.sendToRetryOrFailed(excelJob, "transient-network");

            // Assert
            assertThat(excelJob.getRetryCount()).isEqualTo(1);
            assertThat(excelJob.getStatus()).isEqualTo(JobStatus.RETRY_PENDING);
            assertThat(excelJob.getErrorMessage()).isEqualTo("transient-network");
            verify(retryQueue, times(1)).put(excelJob);
            verify(failedQueue, never()).put(excelJob);
        }
    }

    @Test
    void GivenRetryCountAtMax_WhenSendToRetryOrFailed_ThenJobMovesToFailedQueue() throws InterruptedException {
        // Arrange
        ExcelJob excelJob = ExcelJobTestBuilder.anExcelJob().withRetryCount(3).build();
        when(pipelineQueues.failedQueue()).thenReturn(failedQueue);

        try (MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class)) {
            pipelineConfigMock.when(PipelineConfig::getMaxRetry).thenReturn(3);
            RetryService retryService = new RetryService(pipelineQueues);

            // Act
            retryService.sendToRetryOrFailed(excelJob, "permanent-error");

            // Assert
            assertThat(excelJob.getRetryCount()).isEqualTo(3);
            assertThat(excelJob.getStatus()).isEqualTo(JobStatus.FAILED);
            assertThat(excelJob.getErrorMessage()).isEqualTo("permanent-error");
            verify(failedQueue, times(1)).put(excelJob);
            verify(retryQueue, never()).put(excelJob);
        }
    }

    @Test
    void GivenInterruptedWhilePuttingIntoRetryQueue_WhenSendToRetryOrFailed_ThenCurrentThreadIsInterrupted() throws InterruptedException {
        // Arrange
        ExcelJob excelJob = ExcelJobTestBuilder.anExcelJob().withRetryCount(0).build();
        when(pipelineQueues.retryQueue()).thenReturn(retryQueue);
        org.mockito.Mockito.doThrow(new InterruptedException("interrupted"))
                .when(retryQueue)
                .put(excelJob);

        try (MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class)) {
            pipelineConfigMock.when(PipelineConfig::getMaxRetry).thenReturn(3);
            RetryService retryService = new RetryService(pipelineQueues);

            // Act
            retryService.sendToRetryOrFailed(excelJob, "queue-interrupted");

            // Assert
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            verify(retryQueue, times(1)).put(excelJob);
        }
    }
}



