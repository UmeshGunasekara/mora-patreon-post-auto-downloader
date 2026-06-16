package com.slmora.patreonpostautodownloader.pipeline;

import com.slmora.patreonpostautodownloader.process.FailedJobMonitor;
import com.slmora.patreonpostautodownloader.process.ProcessDocxProducer;
import com.slmora.patreonpostautodownloader.process.ProcessExcelProducer;
import com.slmora.patreonpostautodownloader.process.ProcessImageDownloadWorker;
import com.slmora.patreonpostautodownloader.process.ProcessRetry;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * The {@code ExcelPipelineTest} test class is created for verifying stage
 * submission behavior implemented by {@link ExcelPipeline}.
 * <p>
 * It focuses on confirming that the pipeline coordinator invokes every
 * configured process stage when {@link ExcelPipeline#start()} is called, without
 * running the real Excel, image download, retry, DOCX, or failed-job workflows.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Verifies that all five configured process stages are submitted for execution.</li>
 *     <li>Uses Mockito mocks so no file-system, network, image, or DOCX work is performed.</li>
 *     <li>Uses timeout verification to account for asynchronous executor submission.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link ExcelPipeline}<br>
 * 2 - {@link ProcessExcelProducer}<br>
 * 3 - {@link ProcessImageDownloadWorker}<br>
 * 4 - {@link ProcessRetry}<br>
 * 5 - {@link ProcessDocxProducer}<br>
 * 6 - {@link FailedJobMonitor}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link ExcelPipelineTest#GivenAllStagesConfigured_WhenStartCalled_ThenEveryStageIsSubmittedForExecution()}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>The test verifies stage lifecycle invocation, not the internals of each process stage.</li>
 *     <li>Timeout-based assertions are used because {@link ExcelPipeline#start()} submits work to an executor.</li>
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
class ExcelPipelineTest {

    /**
     * <h3>Submit every configured pipeline stage</h3>
     * Verifies that {@link ExcelPipeline#start()} calls {@code start()} once on
     * the Excel producer, image download worker, retry process, DOCX producer,
     * and failed-job monitor.
     * <p>
     * This test targets the pipeline coordinator's executor submission behavior
     * while each process dependency is represented by a Mockito mock.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates mocks for all five process stages required by {@link ExcelPipeline}.</li>
     *     <li>Constructs the pipeline coordinator with the mocked stages.</li>
     *     <li>Invokes {@link ExcelPipeline#start()}.</li>
     *     <li>Verifies each stage receives exactly one asynchronous {@code start()} call.</li>
     * </ul>
     *
     * @since 1.0
     *
     * @see ExcelPipeline#start()
     */
    @Test
    void GivenAllStagesConfigured_WhenStartCalled_ThenEveryStageIsSubmittedForExecution() {
        // Arrange
        ProcessExcelProducer excelProducer = mock(ProcessExcelProducer.class);
        ProcessImageDownloadWorker imageDownloadWorker = mock(ProcessImageDownloadWorker.class);
        ProcessRetry retryProcess = mock(ProcessRetry.class);
        ProcessDocxProducer docxProducer = mock(ProcessDocxProducer.class);
        FailedJobMonitor failedJobMonitor = mock(FailedJobMonitor.class);

        ExcelPipeline pipeline = new ExcelPipeline(
                excelProducer,
                imageDownloadWorker,
                retryProcess,
                docxProducer,
                failedJobMonitor
        );

        // Act
        pipeline.start();

        // Assert
        verify(excelProducer, timeout(2000).times(1)).start();
        verify(imageDownloadWorker, timeout(2000).times(1)).start();
        verify(retryProcess, timeout(2000).times(1)).start();
        verify(docxProducer, timeout(2000).times(1)).start();
        verify(failedJobMonitor, timeout(2000).times(1)).start();
    }
}

