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

/**
 * The {@code ProcessDocxProducerTest} test class is created for verifying the
 * DOCX producer workflow implemented by {@link ProcessDocxProducer}.
 * <p>
 * It focuses on the worker loop that consumes {@link PipelineQueues#docxReadyQueue()},
 * creates DOCX reports through {@link DocxService}, persists success and failure
 * outcomes through {@link JobPersistenceService}, performs cleanup through
 * {@link CleanupService}, and marks the DOCX producer finished in
 * {@link PipelineState}.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Verifies shutdown behavior when the DOCX queue is empty and upstream stages are finished.</li>
 *     <li>Verifies successful DOCX creation persists success and triggers cleanup.</li>
 *     <li>Verifies DOCX failures mark the job failed, enqueue it, and persist failure details.</li>
 *     <li>Verifies the producer keeps polling while upstream stages may still publish DOCX jobs.</li>
 *     <li>Verifies interrupted failed-queue handoff skips immediate failure persistence.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link ProcessDocxProducer}<br>
 * 2 - {@link PipelineQueues}<br>
 * 3 - {@link PipelineState}<br>
 * 4 - {@link DocxService}<br>
 * 5 - {@link CleanupService}<br>
 * 6 - {@link JobPersistenceService}<br>
 * 7 - {@link ExcelJob}<br>
 * 8 - {@link JobStatus}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link ProcessDocxProducerTest#GivenDocxQueueEmptyAndUpstreamFinished_WhenStart_ThenProducerStopsAndMarksFinished()}</li>
 *     <li>{@link ProcessDocxProducerTest#GivenDocxCreationSucceeds_WhenStart_ThenSuccessIsPersistedAndCleanupRuns()}</li>
 *     <li>{@link ProcessDocxProducerTest#GivenDocxCreationFails_WhenStart_ThenJobMovesToFailedQueueAndFailureIsPersisted()}</li>
 *     <li>{@link ProcessDocxProducerTest#GivenDocxQueueEmptyInitiallyAndUpstreamLaterFinishes_WhenStart_ThenProducerKeepsPollingAndStops()}</li>
 *     <li>{@link ProcessDocxProducerTest#GivenDocxCreationFailsAndFailedQueuePutInterrupted_WhenStart_ThenFailurePersistenceIsSkipped()}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>Tests mock static {@link PipelineConfig} values so no environment-backed configuration is required.</li>
 *     <li>Queue and service collaborators are mocked, so no DOCX file is generated and no cleanup or persistence file I/O occurs.</li>
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
class ProcessDocxProducerTest {

    /**
     * Mocked queue container used to provide DOCX-ready and failed-job queues.
     */
    @Mock
    private PipelineQueues pipelineQueues;

    /**
     * Mocked DOCX-ready queue used to control worker poll results.
     */
    @Mock
    private BlockingQueue<ExcelJob> docxReadyQueue;

    /**
     * Mocked failed-job queue used to verify terminal DOCX failure routing.
     */
    @Mock
    private BlockingQueue<ExcelJob> failedQueue;

    /**
     * Mocked DOCX service used to verify document generation calls and simulate
     * conversion failures.
     */
    @Mock
    private DocxService docxService;

    /**
     * Mocked cleanup service used to verify cleanup only runs after successful
     * DOCX creation.
     */
    @Mock
    private CleanupService cleanupService;

    /**
     * Mocked persistence service used to verify success and failure job logging
     * without writing files.
     */
    @Mock
    private JobPersistenceService jobPersistenceService;

    /**
     * <h3>Stop DOCX producer on empty completed queue</h3>
     * Verifies that {@link ProcessDocxProducer#start()} stops and marks the
     * DOCX producer finished when the DOCX-ready queue is empty and both
     * upstream image and retry stages are already finished.
     * <p>
     * This test targets the idle shutdown path without invoking DOCX creation.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates a {@link PipelineState} with image and retry stages marked finished.</li>
     *     <li>Mocks the DOCX-ready queue poll to return {@code null}.</li>
     *     <li>Starts the producer with one configured DOCX worker thread.</li>
     *     <li>Asserts the DOCX producer finished flag is set and no DOCX creation occurs.</li>
     * </ul>
     *
     * @throws Exception when mocked queue polling or producer execution reports a failure
     * @since 1.0
     *
     * @see ProcessDocxProducer#start()
     */
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

    /**
     * <h3>Persist success and cleanup after DOCX creation</h3>
     * Verifies that {@link ProcessDocxProducer#start()} creates a DOCX report,
     * marks the job as {@link JobStatus#DOCX_CREATED}, persists success, and
     * runs cleanup when document generation succeeds.
     * <p>
     * This test targets the successful DOCX job processing path.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Returns one {@link ExcelJob} from the DOCX-ready queue and then returns {@code null}.</li>
     *     <li>Mocks DOCX output configuration values required by the producer.</li>
     *     <li>Starts the producer with one worker thread.</li>
     *     <li>Verifies DOCX creation, success persistence, cleanup, and final job status.</li>
     * </ul>
     *
     * @throws Exception when mocked queue polling or producer execution reports a failure
     * @since 1.0
     *
     * @see ProcessDocxProducer#start()
     */
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

    /**
     * <h3>Route DOCX creation failure to failed queue</h3>
     * Verifies that a DOCX generation exception causes the job to be marked
     * failed, published to the failed queue, and persisted as a failed job.
     * <p>
     * This test targets terminal failure handling for document conversion.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Returns one job from the DOCX-ready queue.</li>
     *     <li>Mocks {@link DocxService#createDocx(ExcelJob, Path, String, String, String)} to throw a conversion error.</li>
     *     <li>Starts the producer with required DOCX configuration values.</li>
     *     <li>Asserts failed status, DOCX failure message, failed-queue handoff, and failure persistence.</li>
     * </ul>
     *
     * @throws Exception when mocked queue polling or producer execution reports a failure
     * @since 1.0
     *
     * @see ProcessDocxProducer#start()
     */
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

    /**
     * <h3>Keep polling until upstream stages finish</h3>
     * Verifies that {@link ProcessDocxProducer#start()} continues polling an
     * empty DOCX-ready queue while upstream image and retry stages are not yet
     * finished, then stops after those flags become true.
     * <p>
     * This test targets the worker-loop waiting behavior that prevents early
     * shutdown while more DOCX jobs may still arrive.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Starts with image and retry completion flags unset.</li>
     *     <li>Mocks the first DOCX-ready poll to return {@code null} while upstream is still active.</li>
     *     <li>Updates upstream completion flags during the next poll answer.</li>
     *     <li>Verifies the producer polls at least twice, finishes, and never calls DOCX generation.</li>
     * </ul>
     *
     * @throws Exception when mocked queue polling or producer execution reports a failure
     * @since 1.0
     *
     * @see ProcessDocxProducer#start()
     */
    @Test
    void GivenDocxQueueEmptyInitiallyAndUpstreamLaterFinishes_WhenStart_ThenProducerKeepsPollingAndStops() throws Exception {
        // Arrange
        PipelineState pipelineState = new PipelineState();

        when(pipelineQueues.docxReadyQueue()).thenReturn(docxReadyQueue);
        when(docxReadyQueue.poll(anyLong(), eq(TimeUnit.SECONDS)))
                .thenReturn(null)
                .thenAnswer(invocation -> {
                    // Simulate upstream completion after the producer has already observed one empty poll.
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

    /**
     * <h3>Skip immediate persistence when failed queue handoff is interrupted</h3>
     * Verifies that when DOCX creation fails and publishing the job to the
     * failed queue is interrupted, immediate failure persistence is not invoked.
     * <p>
     * This test targets the nested interruption handling inside DOCX failure
     * routing.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Returns one job from the DOCX-ready queue.</li>
     *     <li>Mocks DOCX creation to throw a conversion error.</li>
     *     <li>Mocks failed-queue {@code put} to throw {@link InterruptedException}.</li>
     *     <li>Verifies the job is marked failed and failure persistence is skipped after interrupted queue handoff.</li>
     * </ul>
     *
     * @throws Exception when mocked queue polling or producer execution reports a failure
     * @since 1.0
     *
     * @see ProcessDocxProducer#start()
     */
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



