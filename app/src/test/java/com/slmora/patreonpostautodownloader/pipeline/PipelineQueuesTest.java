package com.slmora.patreonpostautodownloader.pipeline;

import com.slmora.patreonpostautodownloader.config.PipelineConfig;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

/**
 * The {@code PipelineQueuesTest} test class is created for verifying the queue
 * capacity behavior implemented by {@link PipelineQueues}.
 * <p>
 * It focuses on the constructor path that reads queue capacities from
 * {@link PipelineConfig} and creates the bounded queues used by the Patreon post
 * download producer-worker pipeline.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Verifies the Excel-ready queue uses the configured capacity.</li>
 *     <li>Verifies the DOCX-ready queue uses the configured capacity.</li>
 *     <li>Verifies retry and failed-job queues use their configured capacities.</li>
 *     <li>Uses static configuration mocking so the test is independent of environment properties.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link PipelineQueues}<br>
 * 2 - {@link PipelineConfig}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link PipelineQueuesTest#GivenConfiguredCapacities_WhenPipelineQueuesCreated_ThenQueuesRespectConfiguredSizes()}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>The test asserts {@code remainingCapacity()} immediately after construction, before any jobs are inserted.</li>
 *     <li>No producer or worker process is started; this test covers only queue initialization.</li>
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
class PipelineQueuesTest {

    /**
     * <h3>Create queues with configured capacities</h3>
     * Verifies that {@link PipelineQueues#PipelineQueues()} creates each shared
     * queue with the capacity returned by {@link PipelineConfig}.
     * <p>
     * This test targets the queue initialization boundary used by the pipeline
     * controller before process stages begin exchanging {@code ExcelJob}
     * instances.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Mocks all four queue capacity accessors on {@link PipelineConfig}.</li>
     *     <li>Constructs a new {@link PipelineQueues} instance.</li>
     *     <li>Asserts that each empty queue's remaining capacity matches the configured value.</li>
     * </ul>
     *
     * @since 1.0
     *
     * @see PipelineQueues#PipelineQueues()
     */
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

