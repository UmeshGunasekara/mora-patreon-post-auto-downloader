/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 3:25 PM
 */
package com.slmora.patreonpostautodownloader.pipeline;

import lombok.Data;

/**
 * The {@code PipelineState} class is created for sharing process-completion
 * flags across the Patreon post download pipeline.
 * <p>
 * Pipeline stages update these flags when their polling loops finish. Other
 * stages read the flags with queue emptiness checks to decide when no more work
 * can arrive and their own processing can stop.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Tracks completion of the Excel producer stage.</li>
 *     <li>Tracks completion of image download, retry, and DOCX producer stages.</li>
 *     <li>Uses volatile fields so updates made by one process thread are visible to other process threads.</li>
 *     <li>Uses Lombok {@link Data} to generate getters and setters used by the process classes.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link PipelineQueues}<br>
 * 2 - {@link ExcelPipeline}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>Lombok-generated getters and setters for all process-completion flags</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>These flags are coordination signals only; queue contents still determine whether pending work remains.</li>
 *     <li>The flags are not reset by this class. Create a new {@code PipelineState} for a new pipeline execution.</li>
 *     <li>This class does not enforce stage order or legal state transitions.</li>
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
@Data
public class PipelineState
{
    /**
     * Indicates that the Excel producer has finished reading source URLs and no
     * more jobs will be added to the Excel-ready queue.
     */
    private volatile boolean processExcelProducerFinished;

    /**
     * Indicates that the image download worker has finished consuming
     * Excel-ready jobs.
     */
    private volatile boolean processImageDownloadWorkerFinished;

    /**
     * Indicates that the retry process has finished consuming retry jobs.
     */
    private volatile boolean processRetryFinished;

    /**
     * Indicates that the DOCX producer has finished consuming DOCX-ready jobs.
     */
    private volatile boolean processDocxProducerFinished;
}
