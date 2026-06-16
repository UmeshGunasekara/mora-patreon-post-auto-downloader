/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 3:30 PM
 */
package com.slmora.patreonpostautodownloader.process;

import com.slmora.common.logging.MoraLogger;
import com.slmora.common.logging.MoraLoggerThreadInfo;
import com.slmora.patreonpostautodownloader.config.PipelineConfig;
import com.slmora.patreonpostautodownloader.model.DownloadStatus;
import com.slmora.patreonpostautodownloader.model.ExcelJob;
import com.slmora.patreonpostautodownloader.model.ImageRecord;
import com.slmora.patreonpostautodownloader.model.JobStatus;
import com.slmora.patreonpostautodownloader.pipeline.PipelineQueues;
import com.slmora.patreonpostautodownloader.pipeline.PipelineState;
import com.slmora.patreonpostautodownloader.service.ExcelService;
import com.slmora.patreonpostautodownloader.service.ImageDownloadService;
import com.slmora.patreonpostautodownloader.service.RetryService;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The {@code ProcessImageDownloadWorker} class is created for consuming
 * completed Excel jobs and downloading the images referenced by those jobs.
 * <p>
 * This process polls {@link PipelineQueues#excelReadyQueue()}, reads
 * {@link ImageRecord} values from each Excel workbook through
 * {@link ExcelService}, downloads images through {@link ImageDownloadService},
 * and routes the job to either {@link PipelineQueues#docxReadyQueue()} or retry
 * handling through {@link RetryService}.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Runs multiple image-download worker loops using the configured worker count.</li>
 *     <li>Reads image records from Excel files before each download attempt.</li>
 *     <li>Moves fully downloaded jobs to the DOCX-ready queue.</li>
 *     <li>Sends jobs with failed image records to retry or failed-job handling.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link PipelineQueues}<br>
 * 2 - {@link PipelineState}<br>
 * 3 - {@link ExcelJob}<br>
 * 4 - {@link ImageRecord}<br>
 * 5 - {@link DownloadStatus}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link ProcessImageDownloadWorker#ProcessImageDownloadWorker(PipelineQueues, PipelineState, ExcelService, ImageDownloadService, RetryService)}</li>
 *     <li>{@link ProcessImageDownloadWorker#start()}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>The worker stops only after the Excel producer has finished and the Excel-ready queue is empty.</li>
 *     <li>Image download failures are not terminal in this class; retry routing is delegated to {@link RetryService}.</li>
 *     <li>The process-finished flag is set after all worker loop tasks complete.</li>
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
public class ProcessImageDownloadWorker
{
    /**
     * Class-scoped logger used for image worker lifecycle, job processing, and
     * failed image diagnostics.
     */
    private final static MoraLogger LOGGER = MoraLogger.getLogger(ProcessImageDownloadWorker.class);

    /**
     * Shared pipeline queues used to consume Excel jobs and publish DOCX-ready
     * or retry jobs.
     */
    private final PipelineQueues queues;

    /**
     * Shared pipeline state used to determine when upstream Excel production has
     * finished.
     */
    private final PipelineState state;

    /**
     * Service used to read image records from completed Excel workbooks.
     */
    private final ExcelService excelService;

    /**
     * Service used to download images for each job.
     */
    private final ImageDownloadService imageDownloadService;

    /**
     * Service used to send partially failed jobs to retry or failed-job queues.
     */
    private final RetryService retryService;

    /**
     * Process-level executor used to run multiple image download worker loops.
     */
    private final ExecutorService processImageDownloadPool;

    /**
     * <h3>Create image download worker process</h3>
     * Creates the process that consumes Excel jobs and downloads referenced
     * Patreon images.
     * <p>
     * The constructor captures shared queues, shared state, and the services used
     * to read Excel image records, perform downloads, and route failures.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Stores queue and state dependencies shared with the rest of the pipeline.</li>
     *     <li>Stores Excel, image download, and retry services used by job processing.</li>
     *     <li>Creates a fixed-size worker pool using {@link PipelineConfig#getProcessImageDownloadThreads()}.</li>
     * </ul>
     *
     * @param queues shared queues used for consuming and publishing jobs
     * @param state shared pipeline state used for upstream completion checks
     * @param excelService service used to read image records from Excel files
     * @param imageDownloadService service used to download images
     * @param retryService service used to route failed image jobs
     *
     * @apiNote The supplied queues and state should belong to the same pipeline
     * execution as the Excel producer and downstream DOCX producer.
     * @since 1.0
     */
    public ProcessImageDownloadWorker(
            PipelineQueues queues,
            PipelineState state,
            ExcelService excelService,
            ImageDownloadService imageDownloadService,
            RetryService retryService
    ) {
        this.queues = queues;
        this.state = state;
        this.excelService = excelService;
        this.imageDownloadService = imageDownloadService;
        this.retryService = retryService;
        this.processImageDownloadPool = Executors.newFixedThreadPool(PipelineConfig.getProcessImageDownloadThreads());
    }

    /**
     * <h3>Start image download workers</h3>
     * Starts the configured number of image download worker loops and waits for
     * them to finish.
     * <p>
     * Worker loops consume jobs from the Excel-ready queue until the Excel
     * producer has finished and no queued Excel jobs remain. After all submitted
     * worker loops complete, this method marks the image download stage as
     * finished in {@link PipelineState}.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Submits one worker loop per configured image download process thread.</li>
     *     <li>Shuts down the executor after all worker loops are submitted.</li>
     *     <li>Waits up to 24 hours for all worker loops to finish.</li>
     *     <li>Sets {@link PipelineState#setProcessImageDownloadWorkerFinished(boolean)} after the worker pool terminates.</li>
     * </ul>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * ProcessImageDownloadWorker worker = new ProcessImageDownloadWorker(
     *         queues,
     *         state,
     *         excelService,
     *         imageDownloadService,
     *         retryService
     * );
     * worker.start();
     * }</pre>
     *
     * @implNote The finished flag is set only after all worker loops have
     * returned, allowing retry and DOCX processes to combine this signal with
     * queue-empty checks.
     * @since 1.0
     */
    public void start() {
        for (int i = 0; i < PipelineConfig.getProcessImageDownloadThreads(); i++) {
            processImageDownloadPool.submit(this::workerLoop);
        }

        processImageDownloadPool.shutdown();

        try {
            processImageDownloadPool.awaitTermination(24, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        state.setProcessImageDownloadWorkerFinished(true);

        LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                        Thread.currentThread().threadId(),
                        Thread.currentThread().getStackTrace()),
                "ProcessImageDownloadWorker Finished");
    }

    /**
     * <h3>Poll and process Excel jobs</h3>
     * Repeatedly polls the Excel-ready queue and processes available jobs.
     * <p>
     * The loop exits only when no job is available, the Excel producer has
     * finished, and the Excel-ready queue is empty.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Polls {@link PipelineQueues#excelReadyQueue()} with a two-second timeout.</li>
     *     <li>Continues polling while the upstream producer may still add jobs.</li>
     *     <li>Processes each received job through {@link ProcessImageDownloadWorker#processJob(ExcelJob)}.</li>
     * </ul>
     *
     * @implNote The timeout prevents the worker from blocking forever while it
     * waits for the Excel producer completion flag.
     * @since 1.0
     */
    private void workerLoop() {
        while (true) {
            try {
                // Use a timed poll so the loop can periodically observe upstream completion.
                ExcelJob job = queues.excelReadyQueue().poll(2, TimeUnit.SECONDS);

                if (job == null) {
                    if (state.isProcessExcelProducerFinished() && queues.excelReadyQueue().isEmpty()) {
                        break;
                    }
                    continue;
                }
                processJob(job);
            } catch (InterruptedException e) {
                LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()), e);
            }
        }
    }

    /**
     * <h3>Download images for Excel job</h3>
     * Reads image records from the job Excel file, downloads each image, and
     * routes the job to the next pipeline stage.
     * <p>
     * When all image records complete without a {@link DownloadStatus#FAILED}
     * status, the job moves to {@link PipelineQueues#docxReadyQueue()}. When any
     * image fails, retry or failed-job routing is delegated to
     * {@link RetryService#sendToRetryOrFailed(ExcelJob, String)}.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Marks the job as {@link JobStatus#IMAGE_DOWNLOAD_IN_PROGRESS}.</li>
     *     <li>Clears stale image records before reading the current Excel file.</li>
     *     <li>Downloads images into {@link PipelineConfig#getImageOutputDirPath()}.</li>
     *     <li>Logs every non-success image record for diagnostics.</li>
     *     <li>Routes successful jobs to DOCX processing and failed jobs to retry handling.</li>
     * </ul>
     *
     * @param job Excel job whose workbook contains image URL rows
     *
     * @implNote The method treats any {@link DownloadStatus#FAILED} image as a
     * job-level retry condition, even if other images downloaded successfully.
     * @since 1.0
     */
    private void processJob(ExcelJob job) {
        try {
            LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()),
                    "Start ProcessImageDownloadWorker for excel Job {}", job);

            job.setStatus(JobStatus.IMAGE_DOWNLOAD_IN_PROGRESS);
            job.getImageRecords().clear();

            LOGGER.debug(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()),
                    "Clear image collection in excel Job {}",job.getJobId());

            job.getImageRecords().addAll(
                    excelService.readImageRecords(job.getExcelFile(), PipelineConfig.getExcelPostSheetName())
            );

            imageDownloadService.downloadImages(job, PipelineConfig.getImageOutputDirPath());

//            boolean hasSuccess = job.getImageRecords()
//                    .stream()
//                    .anyMatch(r -> r.getDownloadStatus() == DownloadStatus.SUCCESS);
//
//            if (hasSuccess) {
//                job.setStatus(JobStatus.IMAGES_DOWNLOADED);
//                queues.docxReadyQueue().put(job);
//            } else {
//                retryService.sendToRetryOrFailed(job, "No images downloaded successfully");
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
                            "Image download failed for Job {}, Image URL: {}",
                            job.getJobId(),imageRecord.getImageUrl()
                    ));

            if (!hasJobSuccess) {
                LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()),
                        "There are some images has not been downloaded successfully with full check");
            }

            boolean hasFails = job.getImageRecords()
                    .stream()
                    .anyMatch(r -> r.getDownloadStatus() == DownloadStatus.FAILED);

            if(hasFails){
                // A single failed image keeps the job out of DOCX generation until retry handling decides the final route.
                retryService.sendToRetryOrFailed(job, "There are some images has not been downloaded successfully");
                LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()),
                        "There are some images has not been downloaded successfully");
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
