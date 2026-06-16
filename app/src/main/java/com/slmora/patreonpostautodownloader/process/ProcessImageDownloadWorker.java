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
 * The {@code ProcessBImageDownloadWorker} Class created for
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
public class ProcessImageDownloadWorker
{
    private final static MoraLogger LOGGER = MoraLogger.getLogger(ProcessImageDownloadWorker.class);

    private final PipelineQueues queues;
    private final PipelineState state;
    private final ExcelService excelService;
    private final ImageDownloadService imageDownloadService;
    private final RetryService retryService;

    private final ExecutorService processImageDownloadPool;

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

    private void workerLoop() {
        while (true) {
            try {
                //Setting 2 second poll timeout for waiting to available Excel Job
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
