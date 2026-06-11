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
 * The {@code PipelineQueues} Class created for
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
public class PipelineQueues
{
    private final BlockingQueue<ExcelJob> excelReadyQueue;
    private final BlockingQueue<ExcelJob> docxReadyQueue;
    private final BlockingQueue<ExcelJob> retryQueue;
    private final BlockingQueue<ExcelJob> failedQueue;

    public PipelineQueues(PipelineConfig config) {
        this.excelReadyQueue = new ArrayBlockingQueue<>(config.excelQueueCapacity);
        this.docxReadyQueue = new ArrayBlockingQueue<>(config.docxQueueCapacity);
        this.retryQueue = new ArrayBlockingQueue<>(config.retryQueueCapacity);
        this.failedQueue = new ArrayBlockingQueue<>(config.failedQueueCapacity);
    }

    public BlockingQueue<ExcelJob> excelReadyQueue() {
        return excelReadyQueue;
    }

    public BlockingQueue<ExcelJob> docxReadyQueue() {
        return docxReadyQueue;
    }

    public BlockingQueue<ExcelJob> retryQueue() {
        return retryQueue;
    }

    public BlockingQueue<ExcelJob> failedQueue() {
        return failedQueue;
    }
}
