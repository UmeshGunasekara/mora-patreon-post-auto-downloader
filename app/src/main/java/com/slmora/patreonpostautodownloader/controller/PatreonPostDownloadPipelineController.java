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
 * The {@code PipelineApplication} Class created for
 * <h4>Key Features</h4>
 * <ul>
 *      <li>...</li>
 * </ul>
 * <h4>Codes</h4>
 * 1 - {@link }<br>
 * <h4>Methods</h4>
 * <ul>
 *      <li>{@link }</li>
 * </ul>
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>....</li>
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

    public void execute() throws IOException
    {

        PipelineConfig config = new PipelineConfig();

        LOGGER.debug(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                Thread.currentThread().threadId(),
                Thread.currentThread().getStackTrace()),"Load Configuration \n {}", config);

        Files.createDirectories(config.excelOutputDir);
        Files.createDirectories(config.imageOutputDir);
        Files.createDirectories(config.docxOutputDir);
        Files.createDirectories(config.failedOutputDir);

        LOGGER.debug(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                Thread.currentThread().threadId(),
                Thread.currentThread().getStackTrace()),"Created output directories if not exist");

        PipelineQueues queues = new PipelineQueues(config);
        PipelineState state = new PipelineState();

        LOGGER.debug(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                Thread.currentThread().threadId(),
                Thread.currentThread().getStackTrace()),"Configuration, Queues and State initialized");


        UrlExecutionService urlExecutionService = new UrlExecutionService();
        ExcelService excelService = new ExcelService();
        ImageDownloadService imageDownloadService = new ImageDownloadService();
        DocxService docxService = new DocxService();
        CleanupService cleanupService = new CleanupService();
        JobPersistenceService jobPersistenceService = new JobPersistenceService(config);
        RetryService retryService = new RetryService(config, queues);

        LOGGER.debug(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                Thread.currentThread().threadId(),
                Thread.currentThread().getStackTrace()),"Service initialized");

        ProcessExcelProducer excelProducer =
                new ProcessExcelProducer(
                        config,
                        queues,
                        state,
                        urlExecutionService,
                        excelService,
                        jobPersistenceService
                );

        ProcessImageDownloadWorker imageDownloadWorker =
                new ProcessImageDownloadWorker(
                        config,
                        queues,
                        state,
                        excelService,
                        imageDownloadService,
                        retryService
                );

        ProcessRetry retryProcess =
                new ProcessRetry(
                        config,
                        queues,
                        state,
                        imageDownloadService,
                        retryService
                );

        ProcessDocxProducer docxProducer =
                new ProcessDocxProducer(
                        config,
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
}
