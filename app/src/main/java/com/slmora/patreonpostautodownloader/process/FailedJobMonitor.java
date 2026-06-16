/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 3:48 PM
 */
package com.slmora.patreonpostautodownloader.process;

import com.slmora.common.logging.MoraLogger;
import com.slmora.common.logging.MoraLoggerThreadInfo;
import com.slmora.patreonpostautodownloader.controller.PatreonPostDownloadPipelineController;
import com.slmora.patreonpostautodownloader.model.ExcelJob;
import com.slmora.patreonpostautodownloader.pipeline.PipelineQueues;
import com.slmora.patreonpostautodownloader.pipeline.PipelineState;
import com.slmora.patreonpostautodownloader.service.JobPersistenceService;

import java.util.concurrent.TimeUnit;

/**
 * The {@code FailedJobMonitor} Class created for
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
public class FailedJobMonitor
{
    private final static MoraLogger LOGGER = MoraLogger.getLogger(FailedJobMonitor.class);

    private final PipelineQueues queues;
    private final PipelineState state;
    private final JobPersistenceService jobPersistenceService;

    public FailedJobMonitor(
            PipelineQueues queues,
            PipelineState state,
            JobPersistenceService jobPersistenceService
    ) {
        this.queues = queues;
        this.state = state;
        this.jobPersistenceService = jobPersistenceService;
    }

    public void start() {
        while (true) {
            try {
                ExcelJob job = queues.failedQueue().poll(2, TimeUnit.SECONDS);

                if (job == null) {
                    if (state.isProcessImageDownloadWorkerFinished()
                            && state.isProcessRetryFinished()
                            && state.isProcessDocxProducerFinished()
                            && queues.failedQueue().isEmpty()) {
                        break;
                    }
                    continue;
                }

                jobPersistenceService.saveFailedJob(job);

                LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()),
                        "Failed job persisted: {}", job.getJobId());

            } catch (Exception e) {
                LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()), e);
            }
        }
        LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                        Thread.currentThread().threadId(),
                        Thread.currentThread().getStackTrace()),
                "Failed job monitor finished.");
    }
}
