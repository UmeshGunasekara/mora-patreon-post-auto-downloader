/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 2:32 PM
 */
package com.slmora.patreonpostautodownloader.process;

import com.slmora.common.logging.MoraLogger;
import com.slmora.common.logging.MoraLoggerThreadInfo;
import com.slmora.patreonpostautodownloader.config.PipelineConfig;
import com.slmora.patreonpostautodownloader.model.ExcelJob;
import com.slmora.patreonpostautodownloader.model.JobStatus;
import com.slmora.patreonpostautodownloader.pipeline.PipelineQueues;
import com.slmora.patreonpostautodownloader.pipeline.PipelineState;
import com.slmora.patreonpostautodownloader.service.CleanupService;
import com.slmora.patreonpostautodownloader.service.DocxService;
import com.slmora.patreonpostautodownloader.service.JobPersistenceService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The {@code ProcessCRDocxProducer} Class created for
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
public class ProcessDocxProducer
{
    private final static MoraLogger LOGGER = MoraLogger.getLogger(ProcessDocxProducer.class);

    private final PipelineQueues queues;
    private final PipelineState state;
    private final DocxService docxService;
    private final CleanupService cleanupService;
    private final JobPersistenceService jobPersistenceService;

    private final ExecutorService processDocxPool;

    public ProcessDocxProducer(
            PipelineQueues queues,
            PipelineState state,
            DocxService docxService,
            CleanupService cleanupService,
            JobPersistenceService jobPersistenceService
    ) {
        this.queues = queues;
        this.state = state;
        this.docxService = docxService;
        this.cleanupService = cleanupService;
        this.jobPersistenceService = jobPersistenceService;
        this.processDocxPool = Executors.newFixedThreadPool(PipelineConfig.getProcessDocxThreads());
    }

    public void start() {
        for (int i = 0; i < PipelineConfig.getProcessDocxThreads(); i++) {
            processDocxPool.submit(this::workerLoop);
        }

        processDocxPool.shutdown();

        try {
            processDocxPool.awaitTermination(24, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        state.setProcessDocxProducerFinished(true);

        LOGGER.info(threadInfo(),"ProcessDocx Finished");
    }

    private void workerLoop() {
        while (true) {
            try {
                ExcelJob job = queues.docxReadyQueue().poll(2, TimeUnit.SECONDS);

                if (job == null) {
                    if (state.isProcessImageDownloadWorkerFinished()
                            && state.isProcessRetryFinished()
                            && queues.docxReadyQueue().isEmpty()) {
                        break;
                    }
                    continue;
                }

                createDocx(job);

            } catch (Exception e) {
                LOGGER.error(threadInfo(), e);
            }
        }
    }

    private void createDocx(ExcelJob job) {
        try {

            LOGGER.info(threadInfo(),"Start ProcessDocx for excel Job {}", job);

            job.setStatus(JobStatus.DOCX_CREATION_IN_PROGRESS);

            docxService.createDocx(job, PipelineConfig.getDocxOutputDirPath(), PipelineConfig.getDocxPostFileNamePattern(), PipelineConfig.getDocxPostFileName(), PipelineConfig.getExcelPostSheetName());

            job.setStatus(JobStatus.DOCX_CREATED);

            jobPersistenceService.saveSuccessJob(job);

            cleanupService.cleanupAfterSuccess(job);

            LOGGER.info(threadInfo(),"ProcessDocx completed for Job {}", job.getJobId());

        } catch (Exception e) {
            try {
                LOGGER.error(threadInfo(), e);
                job.setStatus(JobStatus.FAILED);
                job.setErrorMessage("DOCX failed: " + e.getMessage());

                queues.failedQueue().put(job);
                jobPersistenceService.saveFailedJob(job);

            } catch (InterruptedException ex) {
                LOGGER.error(threadInfo(), ex);
                Thread.currentThread().interrupt();
            }
        }
    }

    private static MoraLoggerThreadInfo threadInfo() {
        Thread t = Thread.currentThread();
        return new MoraLoggerThreadInfo(t.getName(), t.threadId(), t.getStackTrace());
    }
}
