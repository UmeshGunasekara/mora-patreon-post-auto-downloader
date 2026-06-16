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
 * The {@code RetryService} class is created for routing failed image-download
 * jobs either back to retry processing or to terminal failed-job handling.
 * <p>
 * Image download and retry processes delegate their failure-routing decision to
 * this service so retry count updates, status transitions, and queue handoff
 * remain consistent across the pipeline.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Stores the latest failure reason on the affected {@link ExcelJob}.</li>
 *     <li>Compares the current retry count against {@link PipelineConfig#getMaxRetry()}.</li>
 *     <li>Moves retryable jobs to {@link PipelineQueues#retryQueue()} with {@link JobStatus#RETRY_PENDING}.</li>
 *     <li>Moves exhausted jobs to {@link PipelineQueues#failedQueue()} with {@link JobStatus#FAILED}.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link PipelineConfig}<br>
 * 2 - {@link ExcelJob}<br>
 * 3 - {@link JobStatus}<br>
 * 4 - {@link PipelineQueues}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link RetryService#RetryService(PipelineQueues)}</li>
 *     <li>{@link RetryService#sendToRetryOrFailed(ExcelJob, String)}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>This service does not execute image retries; it only routes jobs after a failure decision.</li>
 *     <li>Queue writes may block because {@link PipelineQueues} uses bounded blocking queues.</li>
 *     <li>Interrupted queue writes are logged and the current thread interrupt flag is restored.</li>
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
    /**
     * Class-scoped logger used for retry-routing decisions and interrupted
     * queue handoff failures.
     */
    private final static MoraLogger LOGGER = MoraLogger.getLogger(RetryService.class);

    /**
     * Shared pipeline queues used to publish retry-pending and terminal failed
     * jobs.
     */
    private final PipelineQueues queues;

    /**
     * <h3>Create retry routing service</h3>
     * Creates a retry service backed by the shared pipeline queue container.
     * <p>
     * The same queue container should be shared with {@link ProcessImageDownloadWorker}
     * and retry processing so jobs move through one coordinated pipeline.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Stores the queue container used for retry and failed-job handoff.</li>
     *     <li>Does not create queues or read retry configuration during construction.</li>
     * </ul>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * RetryService retryService = new RetryService(queues);
     * }</pre>
     *
     * @param queues shared pipeline queues used by all process stages
     *
     * @apiNote The supplied queue container is expected to be non-null and shared with the active pipeline.
     * @since 1.0
     */
    public RetryService(PipelineQueues queues) {
        this.queues = queues;
    }

    /**
     * <h3>Route job to retry or failed queue</h3>
     * Updates the job with the supplied failure reason and routes it according
     * to the configured maximum retry count.
     * <p>
     * If the job has remaining retry attempts, the retry counter is incremented
     * and the job is placed on the retry queue. When the retry limit is already
     * reached, the job is marked failed and placed on the failed queue.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Stores the failure reason on {@link ExcelJob#setErrorMessage(String)}.</li>
     *     <li>Uses {@link PipelineConfig#getMaxRetry()} to decide whether another retry is allowed.</li>
     *     <li>Sets {@link JobStatus#RETRY_PENDING} before publishing retryable jobs to {@link PipelineQueues#retryQueue()}.</li>
     *     <li>Sets {@link JobStatus#FAILED} before publishing exhausted jobs to {@link PipelineQueues#failedQueue()}.</li>
     *     <li>Logs and restores the interrupt flag when blocked queue insertion is interrupted.</li>
     * </ul>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * retryService.sendToRetryOrFailed(job, "Image download failed");
     * }</pre>
     *
     * @param job job to update and route after an image-processing failure
     * @param reason failure reason to store on the job before queue handoff
     *
     * @throws NullPointerException when {@code job} is {@code null}
     *
     * @implNote The retry count is incremented only for jobs sent back to the retry queue.
     * @apiNote This method mutates the supplied job before placing it on the next queue.
     * @since 1.0
     */
    public void sendToRetryOrFailed(ExcelJob job, String reason) {
        try {
            // Preserve the latest failure reason before publishing the job to another process stage.
            job.setErrorMessage(reason);

            LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()),
                    "Retry or failed Job {} and reason", job.getJobId(), reason);

            if (job.getRetryCount() < PipelineConfig.getMaxRetry()) {
                // Count only attempts that are actually routed back to retry processing.
                job.incrementRetryCount();
                job.setStatus(JobStatus.RETRY_PENDING);
                queues.retryQueue().put(job);
            } else {
                // Once the configured retry limit is reached, failed-job monitoring owns persistence.
                job.setStatus(JobStatus.FAILED);
                queues.failedQueue().put(job);
            }

        } catch (InterruptedException e) {
            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()), e);
            // Restore the interrupt flag so outer process loops can observe shutdown/interruption.
            Thread.currentThread().interrupt();
        }
    }
}
