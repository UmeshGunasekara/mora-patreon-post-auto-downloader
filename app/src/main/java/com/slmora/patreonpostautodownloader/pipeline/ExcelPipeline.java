/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 2:22 PM
 */
package com.slmora.patreonpostautodownloader.pipeline;

import com.slmora.patreonpostautodownloader.process.FailedJobMonitor;
import com.slmora.patreonpostautodownloader.process.ProcessExcelProducer;
import com.slmora.patreonpostautodownloader.process.ProcessImageDownloadWorker;
import com.slmora.patreonpostautodownloader.process.ProcessDocxProducer;
import com.slmora.patreonpostautodownloader.process.ProcessRetry;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The {@code ExcelPipeline} class is created for starting the concurrent
 * producer-worker stages of the Patreon post download pipeline.
 * <p>
 * This class receives the already wired process components from the controller
 * and submits each stage to a fixed executor. The stages coordinate through
 * shared queues and pipeline state objects owned by the process instances.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Starts Excel production, image downloading, retry handling, DOCX generation, and failed-job monitoring.</li>
 *     <li>Runs the five process stages concurrently through a fixed-size executor.</li>
 *     <li>Keeps stage construction outside this class so it only owns process submission and executor shutdown.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link ProcessExcelProducer}<br>
 * 2 - {@link ProcessImageDownloadWorker}<br>
 * 3 - {@link ProcessRetry}<br>
 * 4 - {@link ProcessDocxProducer}<br>
 * 5 - {@link FailedJobMonitor}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link ExcelPipeline#ExcelPipeline(ProcessExcelProducer, ProcessImageDownloadWorker, ProcessRetry, ProcessDocxProducer, FailedJobMonitor)}</li>
 *     <li>{@link ExcelPipeline#start()}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>The executor is shut down after all process stages are submitted, which prevents new submissions while allowing submitted stages to continue running.</li>
 *     <li>This class does not wait for all stages to finish; each process owns its own polling, completion, and shutdown behavior.</li>
 *     <li>A single {@code ExcelPipeline} instance should be started once because the executor cannot accept new work after shutdown.</li>
 * </ul>
 *
 * @author: SLMORA
 * @since 1.0
 *
 * <h4>Revision History</h4>
 * <blockquote><pre>
 * <br>Version      Date            Editor              Note
 * <br>-------------------------------------------------------
 * <br>1.0          6/6/2026      SLMORA                Initial Code
 * </pre></blockquote>
 */
public class ExcelPipeline
{
    /**
     * Producer stage that reads Patreon API URLs and creates Excel jobs.
     */
    private final ProcessExcelProducer excelProducer;

    /**
     * Worker stage that reads Excel jobs and downloads referenced images.
     */
    private final ProcessImageDownloadWorker imageDownloadWorker;

    /**
     * Retry stage that reprocesses failed image downloads up to the configured
     * retry limit.
     */
    private final ProcessRetry retryProcess;

    /**
     * Producer stage that creates DOCX reports after images are downloaded.
     */
    private final ProcessDocxProducer docxProducer;

    /**
     * Monitor stage that persists failed job details from the failed queue.
     */
    private final FailedJobMonitor failedJobMonitor;

    /**
     * Executor sized to match the five long-running pipeline stages submitted by
     * {@link ExcelPipeline#start()}.
     */
    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    /**
     * <h3>Create Excel pipeline coordinator</h3>
     * Captures the process stages that will be submitted when the pipeline
     * starts.
     * <p>
     * The controller is responsible for constructing these process objects with
     * shared queues, shared state, and service dependencies before creating this
     * pipeline coordinator.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Stores the Excel producer stage used to create Excel jobs.</li>
     *     <li>Stores the image download and retry stages used for image processing.</li>
     *     <li>Stores the DOCX producer and failed-job monitor stages used for final output and failure persistence.</li>
     * </ul>
     *
     * @param excelProducer producer that creates Excel jobs from Patreon API responses
     * @param imageDownloadWorker worker that downloads images referenced by Excel jobs
     * @param retryProcess process that retries failed image downloads
     * @param docxProducer producer that creates DOCX reports from completed jobs
     * @param failedJobMonitor monitor that persists failed job information
     *
     * @apiNote Provide fully initialized process instances that share the same
     * queue and state objects.
     * @since 1.0
     */
    public ExcelPipeline(
            ProcessExcelProducer excelProducer,
            ProcessImageDownloadWorker imageDownloadWorker,
            ProcessRetry retryProcess,
            ProcessDocxProducer docxProducer,
            FailedJobMonitor failedJobMonitor
    ) {
        this.excelProducer = excelProducer;
        this.imageDownloadWorker = imageDownloadWorker;
        this.retryProcess = retryProcess;
        this.docxProducer = docxProducer;
        this.failedJobMonitor = failedJobMonitor;
    }

    /**
     * <h3>Start pipeline stages</h3>
     * Submits all configured process stages to the internal executor.
     * <p>
     * Each submitted stage invokes its own {@code start()} lifecycle method.
     * Those stage methods perform the actual polling, queue handoff, processing,
     * and completion signaling.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Submits the Excel producer to create workbook jobs.</li>
     *     <li>Submits the image download worker and retry process for image handling.</li>
     *     <li>Submits the DOCX producer and failed-job monitor for final reporting and failure persistence.</li>
     *     <li>Shuts down the executor after submission so no additional stages can be added.</li>
     * </ul>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * ExcelPipeline pipeline = new ExcelPipeline(
     *         excelProducer,
     *         imageDownloadWorker,
     *         retryProcess,
     *         docxProducer,
     *         failedJobMonitor
     * );
     * pipeline.start();
     * }</pre>
     *
     * @implNote {@link ExecutorService#shutdown()} is called immediately after
     * all stage submissions. This does not interrupt submitted stages; it only
     * prevents future task submissions.
     * @apiNote Start each {@code ExcelPipeline} instance only once.
     * @since 1.0
     */
    public void start() {
        executor.submit(excelProducer::start);
        executor.submit(imageDownloadWorker::start);
        executor.submit(retryProcess::start);
        executor.submit(docxProducer::start);
        executor.submit(failedJobMonitor::start);

        // Prevent later task submission while allowing the submitted pipeline stages to finish normally.
        executor.shutdown();
    }
}
