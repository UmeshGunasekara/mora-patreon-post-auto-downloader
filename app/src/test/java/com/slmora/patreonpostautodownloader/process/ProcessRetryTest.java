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

/**
 * The {@code ProcessRetryTest} test class is created for verifying retry-stage
 * behavior implemented by {@link ProcessRetry}.
 * <p>
 * It focuses on polling {@link PipelineQueues#retryQueue()}, retrying image
 * downloads through {@link ImageDownloadService}, routing recovered jobs to
 * {@link PipelineQueues#docxReadyQueue()}, delegating still-failed jobs to
 * {@link RetryService}, and setting the retry finished flag in
 * {@link PipelineState}.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Verifies retry-stage shutdown when upstream image processing is finished and the retry queue is empty.</li>
 *     <li>Verifies recovered retry jobs move to DOCX generation.</li>
 *     <li>Verifies jobs with remaining failed images are sent back through retry routing.</li>
 *     <li>Verifies retry execution exceptions are routed through {@link RetryService}.</li>
 *     <li>Verifies interrupted polling and delayed upstream completion behavior.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link ProcessRetry}<br>
 * 2 - {@link PipelineQueues}<br>
 * 3 - {@link PipelineState}<br>
 * 4 - {@link ImageDownloadService}<br>
 * 5 - {@link RetryService}<br>
 * 6 - {@link ExcelJob}<br>
 * 7 - {@link ImageRecord}<br>
 * 8 - {@link DownloadStatus}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link ProcessRetryTest#GivenRetryQueueEmptyAndUpstreamFinished_WhenStart_ThenRetryProcessStopsAndSetsFinishedFlag()}</li>
 *     <li>{@link ProcessRetryTest#GivenRetrySucceedsWithoutFailedImages_WhenStart_ThenJobMovesToDocxQueue()}</li>
 *     <li>{@link ProcessRetryTest#GivenRetryStillHasFailedImages_WhenStart_ThenJobIsSentToRetryService()}</li>
 *     <li>{@link ProcessRetryTest#GivenRetryServiceThrowsException_WhenStart_ThenJobIsRoutedToRetryServiceWithErrorMessage()}</li>
 *     <li>{@link ProcessRetryTest#GivenRetryPollInterruptedOnce_WhenStart_ThenLoopContinuesAndFinishes()}</li>
 *     <li>{@link ProcessRetryTest#GivenRetryQueueEmptyInitiallyAndUpstreamLaterFinishes_WhenStart_ThenLoopContinuesThenStops()}</li>
 *     <li>{@link ProcessRetryTest#jobWithStatuses(DownloadStatus...)}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>Tests mock queue and service collaborators, so no image files are downloaded.</li>
 *     <li>Static {@link PipelineConfig} mocking is used only where the retry path needs the configured image output directory.</li>
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
class ProcessRetryTest {

    /**
     * Mocked queue container used to provide retry and DOCX-ready queues.
     */
    @Mock
    private PipelineQueues pipelineQueues;

    /**
     * Mocked retry queue used to control retry-process poll results.
     */
    @Mock
    private BlockingQueue<ExcelJob> retryQueue;

    /**
     * Mocked DOCX-ready queue used to verify recovered job handoff.
     */
    @Mock
    private BlockingQueue<ExcelJob> docxReadyQueue;

    /**
     * Mocked image download service used to avoid real image retry operations
     * and simulate retry execution failures.
     */
    @Mock
    private ImageDownloadService imageDownloadService;

    /**
     * Mocked retry service used to verify routing for still-failed jobs and
     * retry execution errors.
     */
    @Mock
    private RetryService retryService;

    /**
     * <h3>Stop retry process on empty completed queue</h3>
     * Verifies that {@link ProcessRetry#start()} stops and marks retry
     * processing finished when the image download worker is finished and the
     * retry queue is empty.
     * <p>
     * This test targets the normal idle shutdown branch without invoking image
     * retry work.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates a {@link PipelineState} with image download worker completion set to {@code true}.</li>
     *     <li>Mocks retry queue polling to return {@code null} and report empty.</li>
     *     <li>Starts the retry process.</li>
     *     <li>Asserts the retry finished flag and verifies no retry service work is invoked.</li>
     * </ul>
     *
     * @throws InterruptedException when mocked queue polling declares interruption
     * @since 1.0
     *
     * @see ProcessRetry#start()
     */
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

    /**
     * <h3>Move recovered retry job to DOCX queue</h3>
     * Verifies that {@link ProcessRetry#start()} sends a job to the DOCX-ready
     * queue when retry processing leaves no failed image records.
     * <p>
     * This test targets the recovered retry path where every image record is in
     * {@link DownloadStatus#SUCCESS}.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates a job fixture containing only successful image records.</li>
     *     <li>Returns the job from the retry queue and then returns {@code null}.</li>
     *     <li>Starts the retry process with the configured image output path.</li>
     *     <li>Asserts the job status becomes {@link JobStatus#IMAGES_DOWNLOADED} and the job is queued for DOCX generation.</li>
     * </ul>
     *
     * @throws Exception when mocked queue handoff or retry execution reports a failure
     * @since 1.0
     *
     * @see ProcessRetry#start()
     */
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

    /**
     * <h3>Route job with remaining failed images</h3>
     * Verifies that {@link ProcessRetry#start()} delegates to
     * {@link RetryService#sendToRetryOrFailed(ExcelJob, String)} when retry
     * processing still leaves at least one failed image.
     * <p>
     * This test targets the branch that prevents DOCX generation until retry
     * routing decides whether another retry or terminal failure should happen.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates a job fixture containing successful and failed image records.</li>
     *     <li>Returns the job from the retry queue and then returns {@code null}.</li>
     *     <li>Starts the retry process with the configured image output path.</li>
     *     <li>Verifies retry routing receives the expected remaining-failure reason.</li>
     * </ul>
     *
     * @throws Exception when mocked queue polling or retry execution reports a failure
     * @since 1.0
     *
     * @see ProcessRetry#start()
     */
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

    /**
     * <h3>Route retry execution exception</h3>
     * Verifies that an exception thrown by
     * {@link ImageDownloadService#retryFailedImages(ExcelJob, Path)} is caught
     * and routed through {@link RetryService} with the original error message.
     * <p>
     * This test targets the retry job error-handling path.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates a retry job fixture with a pending image record.</li>
     *     <li>Mocks image retry execution to throw a runtime exception.</li>
     *     <li>Starts the retry process with the configured image output path.</li>
     *     <li>Verifies retry routing receives the job and exception message.</li>
     * </ul>
     *
     * @throws Exception when mocked queue polling or retry execution reports a failure
     * @since 1.0
     *
     * @see ProcessRetry#start()
     */
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

    /**
     * <h3>Continue after interrupted retry poll</h3>
     * Verifies that {@link ProcessRetry#start()} continues after one
     * interrupted retry queue poll and then finishes on the next empty poll.
     * <p>
     * This test targets interruption handling around timed retry queue polling
     * before any job is available.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Marks upstream image download worker completion as {@code true}.</li>
     *     <li>Mocks the first retry queue poll to throw {@link InterruptedException}.</li>
     *     <li>Mocks the next poll to return {@code null} with the queue empty.</li>
     *     <li>Asserts the retry finished flag and verifies polling was retried.</li>
     * </ul>
     *
     * @throws Exception when mocked queue polling reports a failure
     * @since 1.0
     *
     * @see ProcessRetry#start()
     */
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

    /**
     * <h3>Wait until upstream image worker finishes</h3>
     * Verifies that {@link ProcessRetry#start()} keeps polling an empty retry
     * queue while image download workers may still publish retry jobs, then
     * stops after the upstream finished flag becomes true.
     * <p>
     * This test targets the retry loop's delayed-shutdown behavior.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Starts with the image download worker finished flag unset.</li>
     *     <li>Mocks the first retry queue poll to return {@code null} while upstream may still be active.</li>
     *     <li>Sets the upstream finished flag during the second poll answer.</li>
     *     <li>Asserts retry process completion and verifies no image retry work is invoked.</li>
     * </ul>
     *
     * @throws Exception when mocked queue polling reports a failure
     * @since 1.0
     *
     * @see ProcessRetry#start()
     */
    @Test
    void GivenRetryQueueEmptyInitiallyAndUpstreamLaterFinishes_WhenStart_ThenLoopContinuesThenStops() throws Exception {
        // Arrange
        PipelineState pipelineState = new PipelineState();

        when(pipelineQueues.retryQueue()).thenReturn(retryQueue);
        when(retryQueue.poll(anyLong(), eq(TimeUnit.SECONDS)))
                .thenReturn(null)
                .thenAnswer(invocation -> {
                    // Simulate image workers finishing after the retry process observes one empty poll.
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

    /**
     * <h3>Create retry job with image statuses</h3>
     * Builds an {@link ExcelJob} fixture containing one image record per
     * supplied download status.
     *
     * @param statuses image download statuses to attach to the job fixture
     *
     * @return Excel job populated with image records matching the supplied statuses
     * @since 1.0
     */
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




