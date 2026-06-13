/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 3:48 PM
 */
package com.slmora.patreonpostautodownloader.process;

import com.slmora.common.logging.MoraLoggerThreadInfo;
import com.slmora.patreonpostautodownloader.model.ExcelJob;
import com.slmora.patreonpostautodownloader.pipeline.PipelineQueues;
import com.slmora.patreonpostautodownloader.pipeline.PipelineState;
import com.slmora.patreonpostautodownloader.service.JobPersistenceService;

import java.util.concurrent.TimeUnit;

/**
 * The {@code FailedJobMonitor} Class created for
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
public class FailedJobMonitor
{
    private final PipelineQueues queues;
    private final PipelineState state;
    private final JobPersistenceService jobPersistenceService;

    public FailedJobMonitor(
            PipelineQueues queues,
            PipelineState state,
            JobPersistenceService jobPersistenceService
    ) {
        this.queues = queues;
        this.state = state;
        this.jobPersistenceService = jobPersistenceService;
    }

    public void start() {
        while (true) {
            try {
                ExcelJob job = queues.failedQueue().poll(2, TimeUnit.SECONDS);

                if (job == null) {
                    if (state.isProcessImageDownloadWorkerFinished()
                            && state.isProcessRetryFinished()
                            && state.isProcessDocxProducerFinished()
                            && queues.failedQueue().isEmpty()) {
                        break;
                    }
                    continue;
                }

                jobPersistenceService.saveFailedJob(job);

                System.err.println("Failed job persisted: " + job.getJobId());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("Failed job monitor finished.");
    }

    private static MoraLoggerThreadInfo threadInfo() {
        Thread t = Thread.currentThread();
        return new MoraLoggerThreadInfo(t.getName(), t.threadId(), t.getStackTrace());
    }
}
