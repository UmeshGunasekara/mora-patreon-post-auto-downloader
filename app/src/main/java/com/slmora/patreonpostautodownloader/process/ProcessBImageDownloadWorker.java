/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 3:30 PM
 */
package com.slmora.patreonpostautodownloader.process;

import com.slmora.patreonpostautodownloader.config.PipelineConfig;
import com.slmora.patreonpostautodownloader.model.DownloadStatus;
import com.slmora.patreonpostautodownloader.model.ExcelJob;
import com.slmora.patreonpostautodownloader.model.JobStatus;
import com.slmora.patreonpostautodownloader.pipeline.PipelineQueues;
import com.slmora.patreonpostautodownloader.pipeline.PipelineState;
import com.slmora.patreonpostautodownloader.service.ExcelService;
import com.slmora.patreonpostautodownloader.service.ImageDownloadService;
import com.slmora.patreonpostautodownloader.service.RetryService;

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
public class ProcessBImageDownloadWorker
{
    private final PipelineConfig config;
    private final PipelineQueues queues;
    private final PipelineState state;
    private final ExcelService excelService;
    private final ImageDownloadService imageDownloadService;
    private final RetryService retryService;

    private final ExecutorService processBPool;

    public ProcessBImageDownloadWorker(
            PipelineConfig config,
            PipelineQueues queues,
            PipelineState state,
            ExcelService excelService,
            ImageDownloadService imageDownloadService,
            RetryService retryService
    ) {
        this.config = config;
        this.queues = queues;
        this.state = state;
        this.excelService = excelService;
        this.imageDownloadService = imageDownloadService;
        this.retryService = retryService;
        this.processBPool = Executors.newFixedThreadPool(config.processBThreads);
    }

    public void start() {
        for (int i = 0; i < config.processBThreads; i++) {
            processBPool.submit(this::workerLoop);
        }

        processBPool.shutdown();

        try {
            processBPool.awaitTermination(24, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        state.setProcessBFinished(true);
        System.out.println("Process B finished.");
    }

    private void workerLoop() {
        while (true) {
            try {
                ExcelJob job = queues.excelReadyQueue().poll(2, TimeUnit.SECONDS);

                if (job == null) {
                    if (state.isProcessAFinished() && queues.excelReadyQueue().isEmpty()) {
                        break;
                    }
                    continue;
                }

                processJob(job);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void processJob(ExcelJob job) {
        try {
            job.setStatus(JobStatus.IMAGE_DOWNLOAD_IN_PROGRESS);

            job.getImageRecords().clear();
            job.getImageRecords().addAll(
                    excelService.readImageRecords(job.getExcelFile(), config.excelPostSheetName)
            );

            imageDownloadService.downloadImages(job, config.imageOutputDir);

            boolean hasSuccess = job.getImageRecords()
                    .stream()
                    .anyMatch(r -> r.getDownloadStatus() == DownloadStatus.SUCCESS);

            if (hasSuccess) {
                job.setStatus(JobStatus.IMAGES_DOWNLOADED);
                queues.docxReadyQueue().put(job);
            } else {
                retryService.sendToRetryOrFailed(job, "No images downloaded successfully");
            }

        } catch (Exception e) {
            retryService.sendToRetryOrFailed(job, e.getMessage());
        }
    }
}
