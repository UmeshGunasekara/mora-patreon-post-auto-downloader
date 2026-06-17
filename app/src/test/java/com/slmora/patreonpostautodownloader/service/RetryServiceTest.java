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

/**
 * The {@code RetryServiceTest} test class is created for verifying retry
 * routing behavior implemented by {@link RetryService}.
 * <p>
 * It focuses on how failed image-download jobs are mutated and routed based on
 * {@link PipelineConfig#getMaxRetry()}, including retry queue handoff, terminal
 * failed queue handoff, null-job behavior, and interrupted queue insertion.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Verifies retryable jobs have retry count incremented and move to {@link PipelineQueues#retryQueue()}.</li>
 *     <li>Verifies jobs at the retry limit move to {@link PipelineQueues#failedQueue()} without incrementing retry count.</li>
 *     <li>Verifies the latest failure reason is stored on the job before queue handoff.</li>
 *     <li>Verifies interrupted queue insertion restores the current thread interrupt flag.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link RetryService}<br>
 * 2 - {@link PipelineConfig}<br>
 * 3 - {@link PipelineQueues}<br>
 * 4 - {@link ExcelJob}<br>
 * 5 - {@link JobStatus}<br>
 * 6 - {@link ExcelJobTestBuilder}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link RetryServiceTest#clearInterruptedStatus()}</li>
 *     <li>{@link RetryServiceTest#GivenNullJob_WhenSendToRetryOrFailed_ThenNullPointerExceptionIsThrown()}</li>
 *     <li>{@link RetryServiceTest#GivenRetryCountLowerThanMax_WhenSendToRetryOrFailed_ThenJobMovesToRetryQueue()}</li>
 *     <li>{@link RetryServiceTest#GivenRetryCountAtMax_WhenSendToRetryOrFailed_ThenJobMovesToFailedQueue()}</li>
 *     <li>{@link RetryServiceTest#GivenInterruptedWhilePuttingIntoRetryQueue_WhenSendToRetryOrFailed_ThenCurrentThreadIsInterrupted()}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>Tests mock {@link PipelineConfig#getMaxRetry()} so retry-limit behavior is deterministic.</li>
 *     <li>Queue collaborators are mocked, so no real blocking queue capacity or worker lifecycle is involved.</li>
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
@ExtendWith(MockitoExtension.class)
class RetryServiceTest {

    /**
     * Mocked queue container used by the retry service under test.
     */
    @Mock
    private PipelineQueues pipelineQueues;

    /**
     * Mocked retry queue used to verify retry-pending job handoff.
     */
    @Mock
    private BlockingQueue<ExcelJob> retryQueue;

    /**
     * Mocked failed queue used to verify terminal failed-job handoff.
     */
    @Mock
    private BlockingQueue<ExcelJob> failedQueue;

    /**
     * <h3>Clear interrupted status after each test</h3>
     * Clears the current thread's interrupt flag after scenarios that
     * intentionally simulate interrupted queue insertion.
     * <p>
     * This cleanup prevents one interruption-focused test from influencing later
     * tests executed on the same JUnit worker thread.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Calls {@link Thread#interrupted()} to read and clear the interrupted status.</li>
     * </ul>
     *
     * @since 1.0
     */
    @AfterEach
    void clearInterruptedStatus() {
        Thread.interrupted();
    }

    /**
     * <h3>Reject null job through natural null handling</h3>
     * Verifies that {@link RetryService#sendToRetryOrFailed(ExcelJob, String)}
     * throws {@link NullPointerException} when the supplied job is null.
     * <p>
     * This test documents the current public behavior: the service expects a
     * non-null job and does not perform a defensive null check before mutation.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates a retry service with mocked queues.</li>
     *     <li>Invokes routing with a null job and failure reason.</li>
     *     <li>Asserts that a {@link NullPointerException} is thrown.</li>
     * </ul>
     *
     * @since 1.0
     *
     * @see RetryService#sendToRetryOrFailed(ExcelJob, String)
     */
    @Test
    void GivenNullJob_WhenSendToRetryOrFailed_ThenNullPointerExceptionIsThrown() {
        // Arrange
        RetryService retryService = new RetryService(pipelineQueues);

        // Act + Assert
        assertThatThrownBy(() -> retryService.sendToRetryOrFailed(null, "null-job"))
                .isInstanceOf(NullPointerException.class);
    }

    /**
     * <h3>Move retryable job to retry queue</h3>
     * Verifies that {@link RetryService#sendToRetryOrFailed(ExcelJob, String)}
     * increments retry count, sets {@link JobStatus#RETRY_PENDING}, stores the
     * failure reason, and publishes the job to the retry queue when the retry
     * count is below the configured maximum.
     * <p>
     * This test targets the retryable branch used after recoverable image
     * download failures.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates a job with retry count lower than {@link PipelineConfig#getMaxRetry()}.</li>
     *     <li>Mocks the retry queue and maximum retry value.</li>
     *     <li>Routes the job with a transient failure reason.</li>
     *     <li>Asserts retry metadata and verifies retry-queue handoff.</li>
     * </ul>
     *
     * @throws InterruptedException when mocked retry queue insertion declares interruption
     * @since 1.0
     *
     * @see RetryService#sendToRetryOrFailed(ExcelJob, String)
     */
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

    /**
     * <h3>Move exhausted job to failed queue</h3>
     * Verifies that {@link RetryService#sendToRetryOrFailed(ExcelJob, String)}
     * marks the job as {@link JobStatus#FAILED} and publishes it to the failed
     * queue when the retry count has reached the configured maximum.
     * <p>
     * This test targets the terminal failure branch after retry attempts are
     * exhausted.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates a job whose retry count equals the configured maximum.</li>
     *     <li>Mocks the failed queue and maximum retry value.</li>
     *     <li>Routes the job with a permanent failure reason.</li>
     *     <li>Asserts failed metadata, unchanged retry count, and failed-queue handoff.</li>
     * </ul>
     *
     * @throws InterruptedException when mocked failed queue insertion declares interruption
     * @since 1.0
     *
     * @see RetryService#sendToRetryOrFailed(ExcelJob, String)
     */
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

    /**
     * <h3>Restore interrupt flag when retry queue handoff is interrupted</h3>
     * Verifies that {@link RetryService#sendToRetryOrFailed(ExcelJob, String)}
     * restores the current thread interrupt flag when publishing a retryable job
     * to the retry queue throws {@link InterruptedException}.
     * <p>
     * This test targets the interruption handling path for bounded queue
     * handoff.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates a retryable job and mocks retry-queue insertion to throw {@link InterruptedException}.</li>
     *     <li>Routes the job with a queue interruption reason.</li>
     *     <li>Asserts the current thread is interrupted after the call.</li>
     *     <li>Verifies retry-queue insertion was attempted once.</li>
     * </ul>
     *
     * @throws InterruptedException when Mockito verifies the mocked queue insertion signature
     * @since 1.0
     *
     * @see RetryService#sendToRetryOrFailed(ExcelJob, String)
     */
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



