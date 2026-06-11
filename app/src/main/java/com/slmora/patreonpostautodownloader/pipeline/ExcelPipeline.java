/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 2:22 PM
 */
package com.slmora.patreonpostautodownloader.pipeline;

import com.slmora.patreonpostautodownloader.process.FailedJobMonitor;
import com.slmora.patreonpostautodownloader.process.ProcessAExcelProducer;
import com.slmora.patreonpostautodownloader.process.ProcessBImageDownloadWorker;
import com.slmora.patreonpostautodownloader.process.ProcessCDocxProducer;
import com.slmora.patreonpostautodownloader.process.RetryProcess;

import java.util.List;
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
    private final ProcessAExcelProducer processA;
    private final ProcessBImageDownloadWorker processB;
    private final RetryProcess retryProcess;
    private final ProcessCDocxProducer processC;
    private final FailedJobMonitor failedJobMonitor;

    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    public ExcelPipeline(
            ProcessAExcelProducer processA,
            ProcessBImageDownloadWorker processB,
            RetryProcess retryProcess,
            ProcessCDocxProducer processC,
            FailedJobMonitor failedJobMonitor
    ) {
        this.processA = processA;
        this.processB = processB;
        this.retryProcess = retryProcess;
        this.processC = processC;
        this.failedJobMonitor = failedJobMonitor;
    }

    public void start() {
        executor.submit(processA::start);
        executor.submit(processB::start);
        executor.submit(retryProcess::start);
        executor.submit(processC::start);
        executor.submit(failedJobMonitor::start);

        executor.shutdown();
    }
}
