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

/**
 * The {@code FailedJobMonitorTest} test class is created for verifying the
 * failed-job queue monitoring behavior implemented by {@link FailedJobMonitor}.
 * <p>
 * It focuses on the monitor loop that polls {@link PipelineQueues#failedQueue()},
 * persists failed {@link ExcelJob} instances through {@link JobPersistenceService},
 * and stops only after failed-job-producing pipeline stages are finished and the
 * queue is empty.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Verifies monitor shutdown when no failed jobs remain and upstream stages are complete.</li>
 *     <li>Verifies failed jobs are persisted before the monitor exits.</li>
 *     <li>Confirms persistence exceptions do not stop the monitor loop prematurely.</li>
 *     <li>Confirms polling exceptions are handled and the monitor can recover on the next poll.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link FailedJobMonitor}<br>
 * 2 - {@link PipelineQueues}<br>
 * 3 - {@link PipelineState}<br>
 * 4 - {@link JobPersistenceService}<br>
 * 5 - {@link ExcelJob}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link FailedJobMonitorTest#setUp()}</li>
 *     <li>{@link FailedJobMonitorTest#GivenNoFailedJobsAndAllUpstreamFinished_WhenStart_ThenMonitorStopsWithoutPersisting()}</li>
 *     <li>{@link FailedJobMonitorTest#GivenFailedJobExists_WhenStart_ThenJobIsPersistedBeforeStop()}</li>
 *     <li>{@link FailedJobMonitorTest#GivenPersistenceThrowsException_WhenStart_ThenMonitorContinuesAndStopsGracefully()}</li>
 *     <li>{@link FailedJobMonitorTest#GivenPollThrowsExceptionOnce_WhenStart_ThenMonitorRecoversAndStops()}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>The shared {@link PipelineState} fixture marks upstream stages finished so each test can drive monitor exit deterministically.</li>
 *     <li>Mocks are used for queues and persistence so the tests do not write failed-job log files.</li>
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
class FailedJobMonitorTest {

    /**
     * Mocked queue container used to expose the failed-job queue to the monitor.
     */
    @Mock
    private PipelineQueues pipelineQueues;

    /**
     * Mocked failed-job queue used to control poll results and queue emptiness.
     */
    @Mock
    private BlockingQueue<ExcelJob> failedQueue;

    /**
     * Mocked persistence service used to verify failed-job save attempts without
     * writing files.
     */
    @Mock
    private JobPersistenceService jobPersistenceService;

    /**
     * Real pipeline state fixture used to model upstream stage completion.
     */
    private PipelineState pipelineState;

    /**
     * <h3>Prepare completed upstream state</h3>
     * Creates a fresh {@link PipelineState}, marks every failed-job-producing
     * upstream stage as finished, and connects the mocked failed queue to the
     * mocked {@link PipelineQueues}.
     * <p>
     * This setup lets each test decide monitor behavior only through mocked
     * failed-queue polling and emptiness responses.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Marks image download worker completion as {@code true}.</li>
     *     <li>Marks retry process completion as {@code true}.</li>
     *     <li>Marks DOCX producer completion as {@code true}.</li>
     *     <li>Returns the mocked failed queue from {@link PipelineQueues#failedQueue()}.</li>
     * </ul>
     *
     * @since 1.0
     *
     * @see FailedJobMonitor#start()
     */
    @BeforeEach
    void setUp() {
        pipelineState = new PipelineState();
        // Finished upstream flags allow the monitor to stop as soon as the failed queue is empty.
        pipelineState.setProcessImageDownloadWorkerFinished(true);
        pipelineState.setProcessRetryFinished(true);
        pipelineState.setProcessDocxProducerFinished(true);
        when(pipelineQueues.failedQueue()).thenReturn(failedQueue);
    }

    /**
     * <h3>Stop monitor when no failed jobs exist</h3>
     * Verifies that {@link FailedJobMonitor#start()} stops without persistence
     * when the failed queue is empty and all upstream stages are finished.
     * <p>
     * This test targets the normal empty-queue shutdown branch of the monitor
     * loop.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Mocks timed polling to return {@code null}.</li>
     *     <li>Mocks the failed queue as empty.</li>
     *     <li>Starts the monitor.</li>
     *     <li>Verifies no failed-job persistence occurs and polling was attempted.</li>
     * </ul>
     *
     * @throws InterruptedException when mocked queue polling declares interruption
     * @since 1.0
     *
     * @see FailedJobMonitor#start()
     */
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

    /**
     * <h3>Persist failed job before monitor shutdown</h3>
     * Verifies that {@link FailedJobMonitor#start()} persists a failed job
     * received from the failed queue before stopping after the queue becomes
     * empty.
     * <p>
     * This test targets the successful failed-job persistence path.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates a representative {@link ExcelJob} fixture.</li>
     *     <li>Mocks the first poll to return the failed job and the next poll to return {@code null}.</li>
     *     <li>Mocks queue emptiness to become true after the persisted job is consumed.</li>
     *     <li>Verifies the job is persisted once and the monitor keeps polling until shutdown conditions are met.</li>
     * </ul>
     *
     * @throws InterruptedException when mocked queue polling declares interruption
     * @since 1.0
     *
     * @see FailedJobMonitor#start()
     */
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

    /**
     * <h3>Recover from failed-job persistence exception</h3>
     * Verifies that {@link FailedJobMonitor#start()} catches a persistence
     * exception, continues the monitor loop, and exits gracefully when no failed
     * jobs remain.
     * <p>
     * This test targets the monitor's defensive error handling around
     * {@link JobPersistenceService#saveFailedJob(ExcelJob)}.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates a failed job fixture and returns it from the first queue poll.</li>
     *     <li>Mocks {@link JobPersistenceService#saveFailedJob(ExcelJob)} to throw a runtime exception.</li>
     *     <li>Returns {@code null} on the next poll and marks the queue empty.</li>
     *     <li>Verifies the persistence attempt occurs and the monitor continues to poll afterward.</li>
     * </ul>
     *
     * @throws InterruptedException when mocked queue polling declares interruption
     * @since 1.0
     *
     * @see FailedJobMonitor#start()
     */
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

    /**
     * <h3>Recover from failed queue poll exception</h3>
     * Verifies that {@link FailedJobMonitor#start()} catches an exception thrown
     * while polling the failed queue and then retries polling until normal
     * shutdown conditions are met.
     * <p>
     * This test targets the monitor's defensive error handling around queue
     * access before any job is available to persist.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Mocks the first failed-queue poll to throw a runtime exception.</li>
     *     <li>Mocks the next poll to return {@code null} with the queue empty.</li>
     *     <li>Starts the monitor.</li>
     *     <li>Verifies no failed-job persistence occurs and polling is retried.</li>
     * </ul>
     *
     * @throws InterruptedException when mocked queue polling declares interruption
     * @since 1.0
     *
     * @see FailedJobMonitor#start()
     */
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



