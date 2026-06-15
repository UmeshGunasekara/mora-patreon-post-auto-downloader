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
 * Coordinates startup for the Patreon post download pipeline.
 * <p>
 * This controller is responsible for constructing the pipeline configuration,
 * preparing required output directories, wiring queue and state objects,
 * creating the services used by each process stage, and starting the
 * {@link ExcelPipeline}. It keeps the application entry point lightweight while
 * preserving the project's manual orchestration style.
 * </p>
 *
 * <p>Methods:</p>
 * <ul>
 *     <li>{@link #execute()} - initializes and starts the Patreon post download pipeline.</li>
 * </ul>
 *
 * <p>Key responsibilities:</p>
 * <ul>
 *     <li>Load pipeline configuration and ensure required output directories exist.</li>
 *     <li>Create shared pipeline queues and execution state.</li>
 *     <li>Wire producer, worker, retry, DOCX, and failed-job monitor processes.</li>
 *     <li>Start the configured {@link ExcelPipeline}.</li>
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
    private final static MoraLogger LOGGER = MoraLogger.getLogger(PatreonPostDownloadPipelineController.class);

    /**
     * Initializes all pipeline dependencies and starts the producer-worker workflow.
     *
     * @throws IOException if required output directories cannot be created
     */
    public void execute() throws IOException
    {

        LOGGER.debug(threadInfo(),"Load Configuration \n {}", PipelineConfig.getToString());

        // Ensure every downstream process can write its artifacts before the pipeline starts.
        Files.createDirectories(PipelineConfig.getExcelOutputDirPath());
        Files.createDirectories(PipelineConfig.getImageOutputDirPath());
        Files.createDirectories(PipelineConfig.getDocxOutputDirPath());
        Files.createDirectories(PipelineConfig.getFailedOutputDirPath());

        LOGGER.debug(threadInfo(),"Created output directories if not exist");

        PipelineQueues queues = new PipelineQueues();
        PipelineState state = new PipelineState();

        LOGGER.debug(threadInfo(),"Configuration, Queues and State initialized");

        // Services are constructed here to keep process classes focused on stage-specific work.
        UrlExecutionService urlExecutionService = new UrlExecutionService();
        ExcelService excelService = new ExcelService();
        ImageDownloadService imageDownloadService = new ImageDownloadService();
        DocxService docxService = new DocxService();
        CleanupService cleanupService = new CleanupService();
        JobPersistenceService jobPersistenceService = new JobPersistenceService();
        RetryService retryService = new RetryService(queues);

        LOGGER.debug(threadInfo(),"Service initialized");

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

        pipeline.start();
    }

    private static MoraLoggerThreadInfo threadInfo() {
        Thread t = Thread.currentThread();
        return new MoraLoggerThreadInfo(t.getName(), t.threadId(), t.getStackTrace());
    }
}
