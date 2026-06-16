/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 2:15 PM
 */
package com.slmora.patreonpostautodownloader.pipeline;

import com.slmora.patreonpostautodownloader.config.PipelineConfig;
import com.slmora.patreonpostautodownloader.model.ExcelJob;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The {@code PipelineQueues} class is created for holding the shared job queues
 * used by the Patreon post download producer-worker pipeline.
 * <p>
 * Each queue carries {@link ExcelJob} instances between process stages. Queue
 * capacities are loaded from {@link PipelineConfig}, allowing the pipeline to
 * apply bounded back-pressure between Excel creation, image download, retry,
 * DOCX creation, and failed-job persistence.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Creates the Excel-ready queue consumed by the image download worker.</li>
 *     <li>Creates the DOCX-ready queue consumed by the DOCX producer.</li>
 *     <li>Creates retry and failed-job queues used by retry and persistence processes.</li>
 *     <li>Uses bounded {@link ArrayBlockingQueue} instances to limit in-memory job buffering.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link PipelineConfig}<br>
 * 2 - {@link ExcelJob}<br>
 * 3 - {@link BlockingQueue}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link PipelineQueues#PipelineQueues()}</li>
 *     <li>{@link PipelineQueues#excelReadyQueue()}</li>
 *     <li>{@link PipelineQueues#docxReadyQueue()}</li>
 *     <li>{@link PipelineQueues#retryQueue()}</li>
 *     <li>{@link PipelineQueues#failedQueue()}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>Queue instances are shared by all process stages created for one pipeline execution.</li>
 *     <li>Blocking queue operations may wait when a queue is full or empty, depending on the caller method used.</li>
 *     <li>This class exposes the queues directly so process stages can use {@code put}, {@code poll}, and {@code isEmpty} according to their lifecycle logic.</li>
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
public class PipelineQueues
{
    /**
     * Queue carrying completed Excel jobs from the Excel producer to the image
     * download worker.
     */
    private final BlockingQueue<ExcelJob> excelReadyQueue;

    /**
     * Queue carrying jobs whose images are ready for DOCX report generation.
     */
    private final BlockingQueue<ExcelJob> docxReadyQueue;

    /**
     * Queue carrying jobs that need failed image downloads retried.
     */
    private final BlockingQueue<ExcelJob> retryQueue;

    /**
     * Queue carrying jobs that reached failed-job persistence handling.
     */
    private final BlockingQueue<ExcelJob> failedQueue;

    /**
     * <h3>Create pipeline queues</h3>
     * Creates all bounded queues required by one pipeline execution.
     * <p>
     * Queue capacities are read from {@link PipelineConfig}. The queues are
     * constructed once and shared with all process stages through dependency
     * wiring in the pipeline controller.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates the Excel-ready queue using {@link PipelineConfig#getExcelQueueCapacity()}.</li>
     *     <li>Creates the DOCX-ready queue using {@link PipelineConfig#getDocxQueueCapacity()}.</li>
     *     <li>Creates the retry queue using {@link PipelineConfig#getRetryQueueCapacity()}.</li>
     *     <li>Creates the failed-job queue using {@link PipelineConfig#getFailedQueueCapacity()}.</li>
     * </ul>
     *
     * @implNote Bounded queues are used to provide back-pressure between stages
     * when a downstream process is slower than an upstream producer.
     * @since 1.0
     */
    public PipelineQueues() {
        // Keep queue capacities configuration-driven so pipeline memory pressure can be tuned without code changes.
        this.excelReadyQueue = new ArrayBlockingQueue<>(PipelineConfig.getExcelQueueCapacity());
        this.docxReadyQueue = new ArrayBlockingQueue<>(PipelineConfig.getDocxQueueCapacity());
        this.retryQueue = new ArrayBlockingQueue<>(PipelineConfig.getRetryQueueCapacity());
        this.failedQueue = new ArrayBlockingQueue<>(PipelineConfig.getFailedQueueCapacity());
    }

    /**
     * <h3>Access Excel-ready queue</h3>
     * Returns the queue used to hand completed Excel jobs to the image download
     * worker.
     *
     * @return shared Excel-ready job queue
     * @since 1.0
     */
    public BlockingQueue<ExcelJob> excelReadyQueue() {
        return excelReadyQueue;
    }

    /**
     * <h3>Access DOCX-ready queue</h3>
     * Returns the queue used to hand image-complete jobs to the DOCX producer.
     *
     * @return shared DOCX-ready job queue
     * @since 1.0
     */
    public BlockingQueue<ExcelJob> docxReadyQueue() {
        return docxReadyQueue;
    }

    /**
     * <h3>Access retry queue</h3>
     * Returns the queue used to hand jobs with failed image downloads to retry
     * processing.
     *
     * @return shared retry job queue
     * @since 1.0
     */
    public BlockingQueue<ExcelJob> retryQueue() {
        return retryQueue;
    }

    /**
     * <h3>Access failed-job queue</h3>
     * Returns the queue used to hand terminally failed jobs to the failed-job
     * monitor.
     *
     * @return shared failed-job queue
     * @since 1.0
     */
    public BlockingQueue<ExcelJob> failedQueue() {
        return failedQueue;
    }
}
