/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 3:48 PM
 */
package com.slmora.patreonpostautodownloader.process;

import com.slmora.common.logging.MoraLogger;
import com.slmora.common.logging.MoraLoggerThreadInfo;
import com.slmora.patreonpostautodownloader.controller.PatreonPostDownloadPipelineController;
import com.slmora.patreonpostautodownloader.model.ExcelJob;
import com.slmora.patreonpostautodownloader.pipeline.PipelineQueues;
import com.slmora.patreonpostautodownloader.pipeline.PipelineState;
import com.slmora.patreonpostautodownloader.service.JobPersistenceService;

import java.util.concurrent.TimeUnit;

/**
 * The {@code FailedJobMonitor} class is created for draining terminal failed
 * jobs from the Patreon post download pipeline and persisting their details.
 * <p>
 * This monitor polls {@link PipelineQueues#failedQueue()} for failed
 * {@link ExcelJob} instances and writes detailed failure information through
 * {@link JobPersistenceService}. It remains active until upstream image
 * download, retry, and DOCX producer stages have all finished and the failed
 * queue is empty.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Consumes terminally failed jobs from the shared failed-job queue.</li>
 *     <li>Persists failed job metadata and image failure details through {@link JobPersistenceService}.</li>
 *     <li>Uses pipeline completion flags and queue emptiness to decide when monitoring can stop.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link PipelineQueues}<br>
 * 2 - {@link PipelineState}<br>
 * 3 - {@link ExcelJob}<br>
 * 4 - {@link JobPersistenceService}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link FailedJobMonitor#FailedJobMonitor(PipelineQueues, PipelineState, JobPersistenceService)}</li>
 *     <li>{@link FailedJobMonitor#start()}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>The monitor must remain alive after retry and DOCX processing because either stage can publish failed jobs.</li>
 *     <li>This class does not mark jobs failed; it only persists jobs already routed to the failed queue.</li>
 *     <li>Persistence errors are logged and do not stop the monitor loop.</li>
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
    /**
     * Class-scoped logger used for failed-job persistence and monitor lifecycle
     * diagnostics.
     */
    private final static MoraLogger LOGGER = MoraLogger.getLogger(FailedJobMonitor.class);

    /**
     * Shared pipeline queues used to consume terminally failed jobs.
     */
    private final PipelineQueues queues;

    /**
     * Shared pipeline state used to determine when all failed-job-producing
     * stages have finished.
     */
    private final PipelineState state;

    /**
     * Service used to write failed job details to persistent log files.
     */
    private final JobPersistenceService jobPersistenceService;

    /**
     * <h3>Create failed job monitor</h3>
     * Creates a monitor that drains the failed-job queue for one pipeline
     * execution.
     * <p>
     * The monitor uses shared queues and state from the same pipeline execution
     * so it can determine when failed-job producers are finished and no failed
     * jobs remain to persist.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Stores the shared failed queue container.</li>
     *     <li>Stores completion state used to determine monitor shutdown.</li>
     *     <li>Stores the persistence service used for failed-job output.</li>
     * </ul>
     *
     * @param queues shared queues containing the failed-job queue
     * @param state shared pipeline state used for completion checks
     * @param jobPersistenceService service used to persist failed job details
     *
     * @apiNote The monitor should be started once for the same queue and state
     * set used by image download, retry, and DOCX producer stages.
     * @since 1.0
     */
    public FailedJobMonitor(
            PipelineQueues queues,
            PipelineState state,
            JobPersistenceService jobPersistenceService
    ) {
        this.queues = queues;
        this.state = state;
        this.jobPersistenceService = jobPersistenceService;
    }

    /**
     * <h3>Start failed job monitoring</h3>
     * Polls the failed-job queue and persists every failed job until the
     * pipeline can no longer produce additional failures.
     * <p>
     * The monitor exits only after image download, retry, and DOCX producer
     * stages have all finished and the failed-job queue is empty.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Polls {@link PipelineQueues#failedQueue()} with a two-second timeout.</li>
     *     <li>Persists each received job through {@link JobPersistenceService#saveFailedJob(ExcelJob)}.</li>
     *     <li>Continues polling while any upstream failed-job-producing stage may still publish work.</li>
     *     <li>Logs and continues when persistence or polling errors occur.</li>
     * </ul>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * FailedJobMonitor monitor = new FailedJobMonitor(
     *         queues,
     *         state,
     *         jobPersistenceService
     * );
     * monitor.start();
     * }</pre>
     *
     * @implNote The timed poll allows the monitor to periodically inspect
     * upstream completion flags instead of blocking forever on an empty failed
     * queue.
     * @since 1.0
     */
    public void start() {
        while (true) {
            try {
                // Use a timed poll so the monitor can stop once all failed-job producers are finished.
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

                LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()),
                        "Failed job persisted: {}", job.getJobId());

            } catch (Exception e) {
                LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()), e);
            }
        }
        LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                        Thread.currentThread().threadId(),
                        Thread.currentThread().getStackTrace()),
                "Failed job monitor finished.");
    }
}
