/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 2:30 PM
 */
package com.slmora.patreonpostautodownloader.process;

import com.slmora.common.logging.MoraLogger;
import com.slmora.common.logging.MoraLoggerThreadInfo;
import com.slmora.patreonpostautodownloader.config.PipelineConfig;
import com.slmora.patreonpostautodownloader.controller.PatreonPostDownloadPipelineController;
import com.slmora.patreonpostautodownloader.model.DownloadStatus;
import com.slmora.patreonpostautodownloader.model.ExcelJob;
import com.slmora.patreonpostautodownloader.model.JobStatus;
import com.slmora.patreonpostautodownloader.pipeline.PipelineQueues;
import com.slmora.patreonpostautodownloader.pipeline.PipelineState;
import com.slmora.patreonpostautodownloader.service.ImageDownloadService;
import com.slmora.patreonpostautodownloader.service.RetryService;

import java.util.concurrent.TimeUnit;

/**
 * The {@code ProcessRetry} class is created for retrying image downloads that
 * failed during the image download worker stage.
 * <p>
 * This process consumes {@link ExcelJob} instances from
 * {@link PipelineQueues#retryQueue()}, retries only failed image records through
 * {@link ImageDownloadService}, and then routes the job either to
 * {@link PipelineQueues#docxReadyQueue()} or back through
 * {@link RetryService#sendToRetryOrFailed(ExcelJob, String)}.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Polls the retry queue for jobs with failed image downloads.</li>
 *     <li>Retries failed image records using the configured image output directory.</li>
 *     <li>Moves fully recovered jobs to DOCX generation.</li>
 *     <li>Delegates repeated failure handling to {@link RetryService} for retry-count and failed-queue decisions.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link PipelineQueues}<br>
 * 2 - {@link PipelineState}<br>
 * 3 - {@link ExcelJob}<br>
 * 4 - {@link DownloadStatus}<br>
 * 5 - {@link JobStatus}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link ProcessRetry#ProcessRetry(PipelineQueues, PipelineState, ImageDownloadService, RetryService)}</li>
 *     <li>{@link ProcessRetry#start()}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>The retry loop stops after the image download worker has finished and the retry queue is empty.</li>
 *     <li>This class performs retry execution; {@link RetryService} decides whether a job should retry again or become failed.</li>
 *     <li>The process finished flag is set only after retry polling exits.</li>
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
public class ProcessRetry
{
    /**
     * Class-scoped logger used for retry lifecycle, failed image diagnostics, and
     * retry routing failures.
     */
    private final static MoraLogger LOGGER = MoraLogger.getLogger(ProcessRetry.class);

    /**
     * Shared pipeline queues used to consume retry jobs and publish recovered
     * DOCX-ready jobs.
     */
    private final PipelineQueues queues;

    /**
     * Shared pipeline state used to determine when image download workers have
     * finished producing retry work.
     */
    private final PipelineState state;

    /**
     * Service used to retry image records that remain in
     * {@link DownloadStatus#FAILED} state.
     */
    private final ImageDownloadService imageDownloadService;

    /**
     * Service used to route jobs that still fail after retry execution.
     */
    private final RetryService retryService;

    /**
     * <h3>Create retry process</h3>
     * Creates a retry process with shared pipeline coordination objects and
     * image retry services.
     * <p>
     * The process uses the supplied queues and state from the same pipeline
     * execution so it can safely observe when upstream image download processing
     * has completed.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Stores the shared retry, DOCX-ready, and failed-job queue container.</li>
     *     <li>Stores the shared pipeline state used for upstream completion checks.</li>
     *     <li>Stores services for retrying images and routing jobs after retry outcomes are known.</li>
     * </ul>
     *
     * @param queues shared queues used to consume retry jobs and publish next-stage jobs
     * @param state shared pipeline state used to detect upstream completion
     * @param imageDownloadService service used to retry failed image records
     * @param retryService service used to route jobs to retry or failed queues
     *
     * @apiNote The retry process should share the same {@link PipelineQueues}
     * instance used by the image download worker.
     * @since 1.0
     */
    public ProcessRetry(
            PipelineQueues queues,
            PipelineState state,
            ImageDownloadService imageDownloadService,
            RetryService retryService
    ) {
        this.queues = queues;
        this.state = state;
        this.imageDownloadService = imageDownloadService;
        this.retryService = retryService;
    }

    /**
     * <h3>Start retry processing</h3>
     * Starts polling the retry queue and processes retry jobs until no more
     * retry work can arrive.
     * <p>
     * The loop exits when the image download worker has finished and the retry
     * queue is empty. After the loop exits, this method sets
     * {@link PipelineState#setProcessRetryFinished(boolean)} so DOCX and
     * failed-job monitor stages can finish when their queues are drained.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Polls {@link PipelineQueues#retryQueue()} with a two-second timeout.</li>
     *     <li>Continues waiting while image download workers may still publish retry jobs.</li>
     *     <li>Retries each received job through {@link ProcessRetry#retryJob(ExcelJob)}.</li>
     *     <li>Sets the retry-finished flag after retry queue consumption ends.</li>
     * </ul>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * ProcessRetry retryProcess = new ProcessRetry(
     *         queues,
     *         state,
     *         imageDownloadService,
     *         retryService
     * );
     * retryProcess.start();
     * }</pre>
     *
     * @implNote The timed poll allows this process to periodically inspect the
     * upstream finished flag instead of blocking forever on an empty retry queue.
     * @since 1.0
     */
    public void start() {
        while (true) {
            try {
                // Use a timed poll so retry processing can stop once upstream image workers are done.
                ExcelJob job = queues.retryQueue().poll(2, TimeUnit.SECONDS);

                if (job == null) {
                    if (state.isProcessImageDownloadWorkerFinished() && queues.retryQueue().isEmpty()) {
                        break;
                    }
                    continue;
                }

                retryJob(job);

            } catch (InterruptedException e) {
                LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()), e);
            }
        }

        state.setProcessRetryFinished(true);

        LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                        Thread.currentThread().threadId(),
                        Thread.currentThread().getStackTrace()),
                "Process Retry Finished");
    }

    /**
     * <h3>Retry failed image downloads</h3>
     * Retries failed image records for a job and routes the job based on the
     * resulting image statuses.
     * <p>
     * A job whose image records all recover successfully is moved to
     * {@link PipelineQueues#docxReadyQueue()}. A job with any remaining
     * {@link DownloadStatus#FAILED} records is sent through {@link RetryService}
     * so it can either retry again or move to the failed queue after the maximum
     * retry count.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Marks the job as {@link JobStatus#IMAGE_DOWNLOAD_IN_PROGRESS} while retrying.</li>
     *     <li>Retries failed image records into {@link PipelineConfig#getImageOutputDirPath()}.</li>
     *     <li>Logs each non-success image record after retry execution.</li>
     *     <li>Publishes recovered jobs to DOCX processing.</li>
     *     <li>Delegates failed retry outcomes to {@link RetryService#sendToRetryOrFailed(ExcelJob, String)}.</li>
     * </ul>
     *
     * @param job Excel job containing image records to retry
     *
     * @implNote The method treats any remaining failed image as a job-level
     * retry failure, even when some images are successful.
     * @since 1.0
     */
    private void retryJob(ExcelJob job) {
        try {
            LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()),
                    "Start ProcessRetry for excel Job {}", job);

            job.setStatus(JobStatus.IMAGE_DOWNLOAD_IN_PROGRESS);

            imageDownloadService.retryFailedImages(job, PipelineConfig.getImageOutputDirPath());

//            boolean hasSuccess = job.getImageRecords()
//                    .stream()
//                    .anyMatch(r -> r.getDownloadStatus() == DownloadStatus.SUCCESS);
//
//            boolean hasFailed = job.getImageRecords()
//                    .stream()
//                    .anyMatch(r -> r.getDownloadStatus() == DownloadStatus.FAILED);
//
//            if (hasSuccess && !hasFailed) {
//                job.setStatus(JobStatus.IMAGES_DOWNLOADED);
//                queues.docxReadyQueue().put(job);
//            } else if (hasSuccess) {
//                job.setStatus(JobStatus.IMAGES_DOWNLOADED);
//                queues.docxReadyQueue().put(job);
//            } else {
//                retryService.sendToRetryOrFailed(job, "Retry failed. No successful image download.");
//            }

            boolean hasJobSuccess = job.getImageRecords()
                    .stream()
                    .allMatch(imageRecord -> imageRecord.getDownloadStatus() == DownloadStatus.SUCCESS);

            job.getImageRecords()
                    .stream()
                    .filter(imageRecord -> imageRecord.getDownloadStatus() != DownloadStatus.SUCCESS)
                    .forEach(imageRecord -> LOGGER.warn(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                    Thread.currentThread().threadId(),
                                    Thread.currentThread().getStackTrace()),
                            "Image download failed for Job {}, Image URL: {}",job.getJobId(),imageRecord.getImageUrl()
                    ));

            if (!hasJobSuccess) {
                LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()),
                        "Retry success. There are some images has not been downloaded successfully with full check");
            }

            boolean hasFails = job.getImageRecords()
                    .stream()
                    .anyMatch(r -> r.getDownloadStatus() == DownloadStatus.FAILED);

            if(hasFails){
                // Any remaining failed image prevents DOCX generation until RetryService decides the next route.
                retryService.sendToRetryOrFailed(job, "Retry failed. There are some images has not been downloaded successfully");
                LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()),
                        "Retry failed. There are some images has not been downloaded successfully");
            }else {
                job.setStatus(JobStatus.IMAGES_DOWNLOADED);
                queues.docxReadyQueue().put(job);
            }

        } catch (Exception e) {
            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()), e);
            retryService.sendToRetryOrFailed(job, e.getMessage());
        }
    }
}
