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
import com.slmora.patreonpostautodownloader.process.ProcessAExcelProducer;
import com.slmora.patreonpostautodownloader.process.ProcessBImageDownloadWorker;
import com.slmora.patreonpostautodownloader.process.ProcessCDocxProducer;
import com.slmora.patreonpostautodownloader.process.RetryProcess;
import com.slmora.patreonpostautodownloader.service.DocxService;
import com.slmora.patreonpostautodownloader.service.ExcelService;
import com.slmora.patreonpostautodownloader.service.ImageDownloadService;
import com.slmora.patreonpostautodownloader.service.JobPersistenceService;
import com.slmora.patreonpostautodownloader.service.RetryService;
import com.slmora.patreonpostautodownloader.service.UrlExecutionService;

import java.nio.file.Files;
import java.util.List;

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
public class PipelineApplication
{
    private final static MoraLogger LOGGER = MoraLogger.getLogger(PipelineApplication.class);

    public static void main(String[] args) throws Exception {

        PipelineConfig config = new PipelineConfig();

        LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                Thread.currentThread().getId(),
                Thread.currentThread().getStackTrace()),"Load Configuration \n {}", config);

        Files.createDirectories(config.excelOutputDir);
        Files.createDirectories(config.imageOutputDir);
        Files.createDirectories(config.docxOutputDir);
        Files.createDirectories(config.failedOutputDir);

        LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                Thread.currentThread().getId(),
                Thread.currentThread().getStackTrace()),"Created output directories if not exist");

        PipelineQueues queues = new PipelineQueues(config);
        PipelineState state = new PipelineState();

        UrlExecutionService urlExecutionService = new UrlExecutionService();
        ExcelService excelService = new ExcelService();
        ImageDownloadService imageDownloadService = new ImageDownloadService();
        DocxService docxService = new DocxService();
        CleanupService cleanupService = new CleanupService();
        JobPersistenceService jobPersistenceService = new JobPersistenceService(config);
        RetryService retryService = new RetryService(config, queues);

        ProcessAExcelProducer processA =
                new ProcessAExcelProducer(
                        config,
                        queues,
                        state,
                        urlExecutionService,
                        excelService,
                        jobPersistenceService
                );

        ProcessBImageDownloadWorker processB =
                new ProcessBImageDownloadWorker(
                        config,
                        queues,
                        state,
                        excelService,
                        imageDownloadService,
                        retryService
                );

        RetryProcess retryProcess =
                new RetryProcess(
                        config,
                        queues,
                        state,
                        imageDownloadService,
                        retryService
                );

        ProcessCDocxProducer processC =
                new ProcessCDocxProducer(
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
                        processA,
                        processB,
                        retryProcess,
                        processC,
                        failedJobMonitor
                );

        pipeline.start();
    }
}
