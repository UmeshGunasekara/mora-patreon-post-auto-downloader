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
 * The {@code RetryProcess} Class created for
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
public class ProcessRetry
{
    private final static MoraLogger LOGGER = MoraLogger.getLogger(ProcessRetry.class);

    private final PipelineQueues queues;
    private final PipelineState state;
    private final ImageDownloadService imageDownloadService;
    private final RetryService retryService;

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

    public void start() {
        while (true) {
            try {
                ExcelJob job = queues.retryQueue().poll(2, TimeUnit.SECONDS);

                if (job == null) {
                    if (state.isProcessImageDownloadWorkerFinished() && queues.retryQueue().isEmpty()) {
                        break;
                    }
                    continue;
                }

                retryJob(job);

            } catch (InterruptedException e) {
                LOGGER.error(threadInfo(), e);
            }
        }

        state.setProcessRetryFinished(true);

        LOGGER.info(threadInfo(),"Process Retry Finished");
    }

    private void retryJob(ExcelJob job) {
        try {
            LOGGER.info(threadInfo(),"Start ProcessRetry for excel Job {}", job);

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
                    .forEach(imageRecord -> LOGGER.warn(threadInfo(),
                            "Image download failed for Job {}, Image URL: {}",job.getJobId(),imageRecord.getImageUrl()
                    ));

            if (!hasJobSuccess) {
                LOGGER.error(threadInfo(),"Retry success. There are some images has not been downloaded successfully with full check");
            }

            boolean hasFails = job.getImageRecords()
                    .stream()
                    .anyMatch(r -> r.getDownloadStatus() == DownloadStatus.FAILED);

            if(hasFails){
                retryService.sendToRetryOrFailed(job, "Retry failed. There are some images has not been downloaded successfully");
                LOGGER.error(threadInfo(),"Retry failed. There are some images has not been downloaded successfully");
            }else {
                job.setStatus(JobStatus.IMAGES_DOWNLOADED);
                queues.docxReadyQueue().put(job);
            }

        } catch (Exception e) {
            LOGGER.error(threadInfo(), e);
            retryService.sendToRetryOrFailed(job, e.getMessage());
        }
    }

    private static MoraLoggerThreadInfo threadInfo() {
        Thread t = Thread.currentThread();
        return new MoraLoggerThreadInfo(t.getName(), t.threadId(), t.getStackTrace());
    }
}
