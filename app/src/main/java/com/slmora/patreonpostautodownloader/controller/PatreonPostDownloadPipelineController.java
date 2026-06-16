/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 2:36 PM
 */
package com.slmora.patreonpostautodownloader.controller;

import com.slmora.common.logging.MoraLogger;
import com.slmora.common.logging.MoraLoggerThreadInfo;
import com.slmora.patreonpostautodownloader.process.FailedJobMonitor;
import com.slmora.patreonpostautodownloader.service.CleanupService;
import com.slmora.patreonpostautodownloader.config.PipelineConfig;
import com.slmora.patreonpostautodownloader.pipeline.ExcelPipeline;
import com.slmora.patreonpostautodownloader.pipeline.PipelineQueues;
import com.slmora.patreonpostautodownloader.pipeline.PipelineState;
import com.slmora.patreonpostautodownloader.process.ProcessExcelProducer;
import com.slmora.patreonpostautodownloader.process.ProcessImageDownloadWorker;
import com.slmora.patreonpostautodownloader.process.ProcessDocxProducer;
import com.slmora.patreonpostautodownloader.process.ProcessRetry;
import com.slmora.patreonpostautodownloader.service.DocxService;
import com.slmora.patreonpostautodownloader.service.ExcelService;
import com.slmora.patreonpostautodownloader.service.ImageDownloadService;
import com.slmora.patreonpostautodownloader.service.JobPersistenceService;
import com.slmora.patreonpostautodownloader.service.RetryService;
import com.slmora.patreonpostautodownloader.service.UrlExecutionService;

import java.io.IOException;
import java.nio.file.Files;

/**
 * The {@code PatreonPostDownloadPipelineController} class is created for wiring
 * and starting the complete Patreon post download pipeline.
 * <p>
 * This controller is the application-level composition point. It prepares the
 * configured output directories, creates shared {@link PipelineQueues} and
 * {@link PipelineState}, constructs the services used by each stage, wires the
 * process objects, and delegates concurrent execution to {@link ExcelPipeline}.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Creates required output directories before any producer or worker starts writing files.</li>
 *     <li>Builds one shared queue set and one shared state object for the producer-worker workflow.</li>
 *     <li>Manually wires URL execution, Excel, image download, retry, DOCX, cleanup, and persistence services.</li>
 *     <li>Starts Excel production, image download, retry, DOCX production, and failed-job monitoring through {@link ExcelPipeline}.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link PipelineConfig}<br>
 * 2 - {@link PipelineQueues}<br>
 * 3 - {@link PipelineState}<br>
 * 4 - {@link ExcelPipeline}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link PatreonPostDownloadPipelineController#execute()}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>This class preserves the project's manual wiring style and does not introduce a dependency injection framework.</li>
 *     <li>Runtime behavior depends on values resolved by {@link PipelineConfig}, including directories, queue sizes, worker counts, retry limits, and Patreon authentication.</li>
 *     <li>The started pipeline owns process execution and executor shutdown after this controller delegates to {@link ExcelPipeline#start()}.</li>
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
public class PatreonPostDownloadPipelineController
{
    /**
     * Class-scoped logger used for controller initialization, configuration, and
     * service wiring diagnostics.
     */
    private final static MoraLogger LOGGER = MoraLogger.getLogger(PatreonPostDownloadPipelineController.class);

    /**
     * <h3>Start Patreon post download pipeline</h3>
     * Initializes all pipeline dependencies and starts the producer-worker
     * workflow.
     * <p>
     * This method is called by the application entry point after the JVM starts.
     * It creates the runtime directory structure, shared coordination objects,
     * stage services, process instances, and the {@link ExcelPipeline} that runs
     * those stages concurrently.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Logs the masked {@link PipelineConfig} values before pipeline setup begins.</li>
     *     <li>Creates Excel, image, DOCX, and failed-output directories if they do not already exist.</li>
     *     <li>Creates {@link PipelineQueues} and {@link PipelineState} for cross-stage job handoff and completion signaling.</li>
     *     <li>Constructs services used by the URL producer, Excel writer/reader, image downloader, retry handler, DOCX generator, cleanup handler, and job persistence layer.</li>
     *     <li>Wires all process stages and starts them through {@link ExcelPipeline#start()}.</li>
     * </ul>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * PatreonPostDownloadPipelineController controller = new PatreonPostDownloadPipelineController();
     * controller.execute();
     * }</pre>
     *
     * @throws IOException when one of the configured output directories cannot be created
     *
     * @implNote Service and process construction stays in this controller so the
     * individual process classes remain focused on stage-specific work instead
     * of application composition.
     * @apiNote Ensure required properties and environment values are available
     * before calling this method. The method starts the pipeline but does not
     * perform application-level retry for setup failures.
     * @since 1.0
     *
     * @see ExcelPipeline#start()
     * @see PipelineConfig
     */
    public void execute() throws IOException
    {

        LOGGER.debug(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                        Thread.currentThread().threadId(),
                        Thread.currentThread().getStackTrace()),
                "Load Configuration \n {}", PipelineConfig.getToString());

        // Ensure every downstream process can write its artifacts before the pipeline starts.
        Files.createDirectories(PipelineConfig.getExcelOutputDirPath());
        Files.createDirectories(PipelineConfig.getImageOutputDirPath());
        Files.createDirectories(PipelineConfig.getDocxOutputDirPath());
        Files.createDirectories(PipelineConfig.getFailedOutputDirPath());

        LOGGER.debug(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                        Thread.currentThread().threadId(),
                        Thread.currentThread().getStackTrace()),
                "Created output directories if not exist");

        PipelineQueues queues = new PipelineQueues();
        PipelineState state = new PipelineState();

        LOGGER.debug(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                        Thread.currentThread().threadId(),
                        Thread.currentThread().getStackTrace()),
                "Configuration, Queues and State initialized");

        // Services are constructed here to keep process classes focused on stage-specific work.
        UrlExecutionService urlExecutionService = new UrlExecutionService();
        ExcelService excelService = new ExcelService();
        ImageDownloadService imageDownloadService = new ImageDownloadService();
        DocxService docxService = new DocxService();
        CleanupService cleanupService = new CleanupService();
        JobPersistenceService jobPersistenceService = new JobPersistenceService();
        RetryService retryService = new RetryService(queues);

        LOGGER.debug(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                        Thread.currentThread().threadId(),
                        Thread.currentThread().getStackTrace()),
                "Service initialized");

        ProcessExcelProducer excelProducer =
                new ProcessExcelProducer(
                        queues,
                        state,
                        urlExecutionService,
                        excelService,
                        jobPersistenceService
                );

        ProcessImageDownloadWorker imageDownloadWorker =
                new ProcessImageDownloadWorker(
                        queues,
                        state,
                        excelService,
                        imageDownloadService,
                        retryService
                );

        ProcessRetry retryProcess =
                new ProcessRetry(
                        queues,
                        state,
                        imageDownloadService,
                        retryService
                );

        ProcessDocxProducer docxProducer =
                new ProcessDocxProducer(
                        queues,
                        state,
                        docxService,
                        cleanupService,
                        jobPersistenceService
                );

        FailedJobMonitor failedJobMonitor =
                new FailedJobMonitor(
                        queues,
                        state,
                        jobPersistenceService
                );

        ExcelPipeline pipeline =
                new ExcelPipeline(
                        excelProducer,
                        imageDownloadWorker,
                        retryProcess,
                        docxProducer,
                        failedJobMonitor
                );

        // ExcelPipeline owns concurrent stage submission and shuts down its executor after all stages are submitted.
        pipeline.start();
    }
}
