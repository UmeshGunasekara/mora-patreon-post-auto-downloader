/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 2:30 PM
 */
package com.slmora.patreonpostautodownloader.process;

import com.slmora.common.logging.MoraLoggerThreadInfo;
import com.slmora.patreonpostautodownloader.config.PipelineConfig;
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

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        state.setProcessRetryFinished(true);
        System.out.println("Retry process finished.");
    }

    private void retryJob(ExcelJob job) {
        try {
            job.setStatus(JobStatus.IMAGE_DOWNLOAD_IN_PROGRESS);

            imageDownloadService.retryFailedImages(job, PipelineConfig.getImageOutputDirPath());

            boolean hasSuccess = job.getImageRecords()
                    .stream()
                    .anyMatch(r -> r.getDownloadStatus() == DownloadStatus.SUCCESS);

            boolean hasFailed = job.getImageRecords()
                    .stream()
                    .anyMatch(r -> r.getDownloadStatus() == DownloadStatus.FAILED);

            if (hasSuccess && !hasFailed) {
                job.setStatus(JobStatus.IMAGES_DOWNLOADED);
                queues.docxReadyQueue().put(job);
            } else if (hasSuccess) {
                job.setStatus(JobStatus.IMAGES_DOWNLOADED);
                queues.docxReadyQueue().put(job);
            } else {
                retryService.sendToRetryOrFailed(job, "Retry failed. No successful image download.");
            }

        } catch (Exception e) {
            retryService.sendToRetryOrFailed(job, e.getMessage());
        }
    }

    private static MoraLoggerThreadInfo threadInfo() {
        Thread t = Thread.currentThread();
        return new MoraLoggerThreadInfo(t.getName(), t.threadId(), t.getStackTrace());
    }
}
