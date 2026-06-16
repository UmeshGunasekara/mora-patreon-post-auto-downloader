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

/**
 * The {@code ProcessImageDownloadWorkerTest} test class is created for
 * verifying the image download worker workflow implemented by
 * {@link ProcessImageDownloadWorker}.
 * <p>
 * It focuses on the worker loop that consumes {@link PipelineQueues#excelReadyQueue()},
 * reads image rows through {@link ExcelService}, delegates image downloads to
 * {@link ImageDownloadService}, routes successful jobs to
 * {@link PipelineQueues#docxReadyQueue()}, and delegates failures to
 * {@link RetryService}.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Verifies idle shutdown when the Excel-ready queue is empty and the producer is finished.</li>
 *     <li>Verifies successful image jobs move to the DOCX-ready queue.</li>
 *     <li>Verifies failed image records are routed to retry handling.</li>
 *     <li>Verifies processing exceptions are routed to retry handling with the error message.</li>
 *     <li>Verifies interrupted polling does not prevent final worker shutdown.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link ProcessImageDownloadWorker}<br>
 * 2 - {@link PipelineQueues}<br>
 * 3 - {@link PipelineState}<br>
 * 4 - {@link ExcelService}<br>
 * 5 - {@link ImageDownloadService}<br>
 * 6 - {@link RetryService}<br>
 * 7 - {@link ExcelJob}<br>
 * 8 - {@link ImageRecord}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link ProcessImageDownloadWorkerTest#GivenQueueEmptyAndProducerFinished_WhenStart_ThenWorkerStopsAndSetsFinishedFlag()}</li>
 *     <li>{@link ProcessImageDownloadWorkerTest#GivenAllImagesDownloaded_WhenStart_ThenJobMovesToDocxQueue()}</li>
 *     <li>{@link ProcessImageDownloadWorkerTest#GivenFailedImageExists_WhenStart_ThenJobIsSentToRetryService()}</li>
 *     <li>{@link ProcessImageDownloadWorkerTest#GivenProcessingThrowsException_WhenStart_ThenErrorIsSentToRetryService()}</li>
 *     <li>{@link ProcessImageDownloadWorkerTest#GivenPollInterruptedOnce_WhenStart_ThenWorkerContinuesAndFinishesOnNextEmptyPoll()}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>Tests mock {@link PipelineConfig} values so no environment-backed configuration is required.</li>
 *     <li>Queue and service collaborators are mocked, so no Excel files are read and no images are downloaded.</li>
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
class ProcessImageDownloadWorkerTest {

    /**
     * Mocked queue container used to provide Excel-ready and DOCX-ready queues.
     */
    @Mock
    private PipelineQueues pipelineQueues;

    /**
     * Mocked Excel-ready queue used to control worker poll results.
     */
    @Mock
    private BlockingQueue<ExcelJob> excelReadyQueue;

    /**
     * Mocked DOCX-ready queue used to verify successful image job handoff.
     */
    @Mock
    private BlockingQueue<ExcelJob> docxReadyQueue;

    /**
     * Mocked Excel service used to provide image records or simulate Excel parse
     * failures.
     */
    @Mock
    private ExcelService excelService;

    /**
     * Mocked image download service used to avoid real HTTP image downloads.
     */
    @Mock
    private ImageDownloadService imageDownloadService;

    /**
     * Mocked retry service used to verify failed image and processing error
     * routing.
     */
    @Mock
    private RetryService retryService;

    /**
     * <h3>Stop worker on empty completed queue</h3>
     * Verifies that {@link ProcessImageDownloadWorker#start()} stops and marks
     * the image download worker finished when the Excel producer has finished
     * and the Excel-ready queue is empty.
     * <p>
     * This test targets the idle shutdown path without reading Excel image rows
     * or attempting image downloads.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates a {@link PipelineState} with Excel producer completion set to {@code true}.</li>
     *     <li>Mocks the Excel-ready queue poll to return {@code null} and the queue as empty.</li>
     *     <li>Starts the worker with one configured image download thread.</li>
     *     <li>Asserts the worker finished flag and verifies no service collaborators are called.</li>
     * </ul>
     *
     * @throws Exception when mocked queue polling or worker execution reports a failure
     * @since 1.0
     *
     * @see ProcessImageDownloadWorker#start()
     */
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

    /**
     * <h3>Move successful image job to DOCX queue</h3>
     * Verifies that {@link ProcessImageDownloadWorker#start()} routes a job to
     * the DOCX-ready queue when every image record has
     * {@link DownloadStatus#SUCCESS}.
     * <p>
     * This test targets the successful image processing path after image records
     * are read from the Excel file.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Returns one {@link ExcelJob} from the Excel-ready queue and then returns {@code null}.</li>
     *     <li>Mocks {@link ExcelService#readImageRecords(Path, String)} to return successful image records.</li>
     *     <li>Starts the worker with configured sheet and image output values.</li>
     *     <li>Asserts the job status becomes {@link JobStatus#IMAGES_DOWNLOADED} and the job is queued for DOCX generation.</li>
     * </ul>
     *
     * @throws Exception when mocked queue handoff, Excel reading, or worker execution fails
     * @since 1.0
     *
     * @see ProcessImageDownloadWorker#start()
     */
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

    /**
     * <h3>Route failed image job to retry service</h3>
     * Verifies that {@link ProcessImageDownloadWorker#start()} delegates to
     * {@link RetryService#sendToRetryOrFailed(ExcelJob, String)} when any image
     * record is marked {@link DownloadStatus#FAILED}.
     * <p>
     * This test targets the failed-image branch that keeps a job out of DOCX
     * generation until retry handling decides the next route.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Returns one job from the Excel-ready queue.</li>
     *     <li>Mocks Excel image records with one failed image record.</li>
     *     <li>Starts the worker with configured sheet and image output values.</li>
     *     <li>Verifies the job is sent to retry routing with the expected failed-image reason.</li>
     * </ul>
     *
     * @throws Exception when mocked queue polling, Excel reading, or worker execution fails
     * @since 1.0
     *
     * @see ProcessImageDownloadWorker#start()
     */
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

    /**
     * <h3>Route processing exception to retry service</h3>
     * Verifies that a processing exception while reading image records is caught
     * and sent to {@link RetryService#sendToRetryOrFailed(ExcelJob, String)}
     * with the exception message.
     * <p>
     * This test targets the worker's defensive error handling around per-job
     * processing.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Returns one job from the Excel-ready queue.</li>
     *     <li>Mocks {@link ExcelService#readImageRecords(Path, String)} to throw a runtime exception.</li>
     *     <li>Starts the worker with configured image worker settings.</li>
     *     <li>Verifies retry routing receives the job and the original error message.</li>
     * </ul>
     *
     * @throws Exception when mocked queue polling or worker execution fails
     * @since 1.0
     *
     * @see ProcessImageDownloadWorker#start()
     */
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

    /**
     * <h3>Continue after interrupted poll</h3>
     * Verifies that {@link ProcessImageDownloadWorker#start()} continues the
     * worker loop after one interrupted queue poll and then finishes on the next
     * empty poll once the Excel producer is complete.
     * <p>
     * This test targets interruption handling around timed queue polling before
     * any job is available.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Mocks the first Excel-ready queue poll to throw {@link InterruptedException}.</li>
     *     <li>Mocks the next poll to return {@code null} with the queue empty.</li>
     *     <li>Starts the worker with one configured image download thread.</li>
     *     <li>Asserts the worker finished flag and verifies polling was retried.</li>
     * </ul>
     *
     * @throws Exception when mocked queue polling or worker execution reports a failure
     * @since 1.0
     *
     * @see ProcessImageDownloadWorker#start()
     */
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

