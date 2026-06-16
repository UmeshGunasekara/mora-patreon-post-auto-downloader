package com.slmora.patreonpostautodownloader.pipeline;

import com.slmora.patreonpostautodownloader.config.PipelineConfig;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class PipelineQueuesTest {

    @Test
    void GivenConfiguredCapacities_WhenPipelineQueuesCreated_ThenQueuesRespectConfiguredSizes() {
        try (MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class)) {
            // Arrange
            pipelineConfigMock.when(PipelineConfig::getExcelQueueCapacity).thenReturn(11);
            pipelineConfigMock.when(PipelineConfig::getDocxQueueCapacity).thenReturn(12);
            pipelineConfigMock.when(PipelineConfig::getRetryQueueCapacity).thenReturn(13);
            pipelineConfigMock.when(PipelineConfig::getFailedQueueCapacity).thenReturn(14);

            // Act
            PipelineQueues pipelineQueues = new PipelineQueues();

            // Assert
            assertThat(pipelineQueues.excelReadyQueue().remainingCapacity()).isEqualTo(11);
            assertThat(pipelineQueues.docxReadyQueue().remainingCapacity()).isEqualTo(12);
            assertThat(pipelineQueues.retryQueue().remainingCapacity()).isEqualTo(13);
            assertThat(pipelineQueues.failedQueue().remainingCapacity()).isEqualTo(14);
        }
    }
}

