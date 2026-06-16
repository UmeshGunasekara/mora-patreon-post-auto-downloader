/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 3:27 PM
 */
package com.slmora.patreonpostautodownloader.service;

import com.slmora.common.logging.MoraLogger;
import com.slmora.common.logging.MoraLoggerThreadInfo;
import com.slmora.patreonpostautodownloader.config.PipelineConfig;
import com.slmora.patreonpostautodownloader.model.ExcelJob;
import com.slmora.patreonpostautodownloader.model.JobStatus;
import com.slmora.patreonpostautodownloader.pipeline.PipelineQueues;
import com.slmora.patreonpostautodownloader.process.ProcessImageDownloadWorker;

/**
 * The {@code RetryService} Class created for
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
public class RetryService
{
    private final static MoraLogger LOGGER = MoraLogger.getLogger(RetryService.class);

    private final PipelineQueues queues;

    public RetryService(PipelineQueues queues) {
        this.queues = queues;
    }

    public void sendToRetryOrFailed(ExcelJob job, String reason) {
        try {
            job.setErrorMessage(reason);

            LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()),
                    "Retry or failed Job {} and reason", job.getJobId(), reason);

            if (job.getRetryCount() < PipelineConfig.getMaxRetry()) {
                job.incrementRetryCount();
                job.setStatus(JobStatus.RETRY_PENDING);
                queues.retryQueue().put(job);
            } else {
                job.setStatus(JobStatus.FAILED);
                queues.failedQueue().put(job);
            }

        } catch (InterruptedException e) {
            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()), e);
            Thread.currentThread().interrupt();
        }
    }
}
