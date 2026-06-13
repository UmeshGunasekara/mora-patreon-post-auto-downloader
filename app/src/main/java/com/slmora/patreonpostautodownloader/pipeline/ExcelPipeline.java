/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 2:22 PM
 */
package com.slmora.patreonpostautodownloader.pipeline;

import com.slmora.common.logging.MoraLoggerThreadInfo;
import com.slmora.patreonpostautodownloader.process.FailedJobMonitor;
import com.slmora.patreonpostautodownloader.process.ProcessExcelProducer;
import com.slmora.patreonpostautodownloader.process.ProcessImageDownloadWorker;
import com.slmora.patreonpostautodownloader.process.ProcessDocxProducer;
import com.slmora.patreonpostautodownloader.process.ProcessRetry;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The {@code ExcelPipeline} Class created for
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
public class ExcelPipeline
{
    private final ProcessExcelProducer excelProducer;
    private final ProcessImageDownloadWorker imageDownloadWorker;
    private final ProcessRetry retryProcess;
    private final ProcessDocxProducer docxProducer;
    private final FailedJobMonitor failedJobMonitor;

    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    public ExcelPipeline(
            ProcessExcelProducer excelProducer,
            ProcessImageDownloadWorker imageDownloadWorker,
            ProcessRetry retryProcess,
            ProcessDocxProducer docxProducer,
            FailedJobMonitor failedJobMonitor
    ) {
        this.excelProducer = excelProducer;
        this.imageDownloadWorker = imageDownloadWorker;
        this.retryProcess = retryProcess;
        this.docxProducer = docxProducer;
        this.failedJobMonitor = failedJobMonitor;
    }

    public void start() {
        executor.submit(excelProducer::start);
        executor.submit(imageDownloadWorker::start);
        executor.submit(retryProcess::start);
        executor.submit(docxProducer::start);
        executor.submit(failedJobMonitor::start);

        executor.shutdown();
    }

    private static MoraLoggerThreadInfo threadInfo() {
        Thread t = Thread.currentThread();
        return new MoraLoggerThreadInfo(t.getName(), t.threadId(), t.getStackTrace());
    }
}
