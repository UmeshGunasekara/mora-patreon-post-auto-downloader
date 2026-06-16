/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 2:32 PM
 */
package com.slmora.patreonpostautodownloader.process;

import com.slmora.common.logging.MoraLogger;
import com.slmora.common.logging.MoraLoggerThreadInfo;
import com.slmora.patreonpostautodownloader.config.PipelineConfig;
import com.slmora.patreonpostautodownloader.model.ExcelJob;
import com.slmora.patreonpostautodownloader.model.JobStatus;
import com.slmora.patreonpostautodownloader.pipeline.PipelineQueues;
import com.slmora.patreonpostautodownloader.pipeline.PipelineState;
import com.slmora.patreonpostautodownloader.service.CleanupService;
import com.slmora.patreonpostautodownloader.service.DocxService;
import com.slmora.patreonpostautodownloader.service.JobPersistenceService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The {@code ProcessDocxProducer} class is created for consuming image-complete
 * Excel jobs and generating DOCX reports.
 * <p>
 * This process polls {@link PipelineQueues#docxReadyQueue()}, delegates DOCX
 * creation to {@link DocxService}, persists successful jobs through
 * {@link JobPersistenceService}, performs success cleanup through
 * {@link CleanupService}, and routes DOCX failures to the failed-job queue.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Runs multiple DOCX worker loops using the configured DOCX process thread count.</li>
 *     <li>Generates DOCX reports from completed Excel jobs and downloaded image state.</li>
 *     <li>Persists successful report completion and performs post-success cleanup.</li>
 *     <li>Marks failed DOCX jobs and publishes them to failed-job handling.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link PipelineQueues}<br>
 * 2 - {@link PipelineState}<br>
 * 3 - {@link ExcelJob}<br>
 * 4 - {@link DocxService}<br>
 * 5 - {@link JobStatus}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link ProcessDocxProducer#ProcessDocxProducer(PipelineQueues, PipelineState, DocxService, CleanupService, JobPersistenceService)}</li>
 *     <li>{@link ProcessDocxProducer#start()}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>The worker stops only after image download and retry stages are finished and the DOCX-ready queue is empty.</li>
 *     <li>DOCX output naming depends on the configured Excel file-name pattern and DOCX template file name.</li>
 *     <li>The process-finished flag is set after all DOCX worker loop tasks complete.</li>
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
public class ProcessDocxProducer
{
    /**
     * Class-scoped logger used for DOCX worker lifecycle, report generation, and
     * failure routing diagnostics.
     */
    private final static MoraLogger LOGGER = MoraLogger.getLogger(ProcessDocxProducer.class);

    /**
     * Shared pipeline queues used to consume DOCX-ready jobs and publish failed
     * jobs.
     */
    private final PipelineQueues queues;

    /**
     * Shared pipeline state used to determine when upstream image and retry
     * stages have finished.
     */
    private final PipelineState state;

    /**
     * Service used to generate DOCX files from Excel job content.
     */
    private final DocxService docxService;

    /**
     * Service used to run cleanup after successful DOCX creation.
     */
    private final CleanupService cleanupService;

    /**
     * Service used to persist successful and failed job records.
     */
    private final JobPersistenceService jobPersistenceService;

    /**
     * Process-level executor used to run multiple DOCX worker loops.
     */
    private final ExecutorService processDocxPool;

    /**
     * <h3>Create DOCX producer process</h3>
     * Creates the process that consumes DOCX-ready jobs and generates report
     * documents.
     * <p>
     * The constructor captures shared queue and state dependencies along with
     * the services needed for document generation, cleanup, and persistence.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Stores queue and state dependencies shared with upstream and failed-job stages.</li>
     *     <li>Stores DOCX, cleanup, and persistence services used by job processing.</li>
     *     <li>Creates a fixed-size worker pool using {@link PipelineConfig#getProcessDocxThreads()}.</li>
     * </ul>
     *
     * @param queues shared queues used for consuming DOCX-ready jobs and publishing failed jobs
     * @param state shared pipeline state used for upstream completion checks
     * @param docxService service used to create DOCX reports
     * @param cleanupService service used after successful DOCX creation
     * @param jobPersistenceService service used to persist success and failure details
     *
     * @apiNote The supplied queues and state should belong to the same pipeline
     * execution as the image download and retry processes.
     * @since 1.0
     */
    public ProcessDocxProducer(
            PipelineQueues queues,
            PipelineState state,
            DocxService docxService,
            CleanupService cleanupService,
            JobPersistenceService jobPersistenceService
    ) {
        this.queues = queues;
        this.state = state;
        this.docxService = docxService;
        this.cleanupService = cleanupService;
        this.jobPersistenceService = jobPersistenceService;
        this.processDocxPool = Executors.newFixedThreadPool(PipelineConfig.getProcessDocxThreads());
    }

    /**
     * <h3>Start DOCX workers</h3>
     * Starts the configured number of DOCX worker loops and waits for them to
     * finish.
     * <p>
     * Worker loops consume jobs from the DOCX-ready queue until image download
     * and retry processing are finished and no queued DOCX jobs remain. After
     * all submitted worker loops complete, this method marks the DOCX producer
     * as finished in {@link PipelineState}.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Submits one worker loop per configured DOCX process thread.</li>
     *     <li>Shuts down the executor after all worker loops are submitted.</li>
     *     <li>Waits up to 24 hours for worker completion.</li>
     *     <li>Sets {@link PipelineState#setProcessDocxProducerFinished(boolean)} after the worker pool terminates.</li>
     * </ul>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * ProcessDocxProducer producer = new ProcessDocxProducer(
     *         queues,
     *         state,
     *         docxService,
     *         cleanupService,
     *         jobPersistenceService
     * );
     * producer.start();
     * }</pre>
     *
     * @implNote The finished flag is set after submitted worker loops complete,
     * allowing the failed-job monitor to combine this signal with failed-queue
     * emptiness checks.
     * @since 1.0
     */
    public void start() {
        for (int i = 0; i < PipelineConfig.getProcessDocxThreads(); i++) {
            processDocxPool.submit(this::workerLoop);
        }

        processDocxPool.shutdown();

        try {
            processDocxPool.awaitTermination(24, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        state.setProcessDocxProducerFinished(true);

        LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                        Thread.currentThread().threadId(),
                        Thread.currentThread().getStackTrace()),
                "ProcessDocx Finished");
    }

    /**
     * <h3>Poll and process DOCX jobs</h3>
     * Repeatedly polls the DOCX-ready queue and creates DOCX reports for
     * available jobs.
     * <p>
     * The loop exits only when no job is available, image download and retry
     * stages have both finished, and the DOCX-ready queue is empty.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Polls {@link PipelineQueues#docxReadyQueue()} with a two-second timeout.</li>
     *     <li>Continues polling while upstream image or retry stages may still add jobs.</li>
     *     <li>Processes each received job through {@link ProcessDocxProducer#createDocx(ExcelJob)}.</li>
     * </ul>
     *
     * @implNote The timeout prevents the worker from blocking forever while it
     * waits for upstream completion flags.
     * @since 1.0
     */
    private void workerLoop() {
        while (true) {
            try {
                // Use a timed poll so the worker can periodically observe upstream completion.
                ExcelJob job = queues.docxReadyQueue().poll(2, TimeUnit.SECONDS);

                if (job == null) {
                    if (state.isProcessImageDownloadWorkerFinished()
                            && state.isProcessRetryFinished()
                            && queues.docxReadyQueue().isEmpty()) {
                        break;
                    }
                    continue;
                }

                createDocx(job);

            } catch (Exception e) {
                LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()), e);
            }
        }
    }

    /**
     * <h3>Create DOCX report for job</h3>
     * Generates a DOCX report for a completed image job and records the outcome.
     * <p>
     * On success, the job is marked as {@link JobStatus#DOCX_CREATED}, persisted
     * as successful, and passed to cleanup. On failure, the job is marked as
     * {@link JobStatus#FAILED}, enriched with an error message, added to the
     * failed queue, and persisted as a failed job.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Marks the job as {@link JobStatus#DOCX_CREATION_IN_PROGRESS} before generation.</li>
     *     <li>Calls {@link DocxService#createDocx(ExcelJob, java.nio.file.Path, String, String, String)} using configured DOCX paths and naming values.</li>
     *     <li>Persists successful jobs with {@link JobPersistenceService#saveSuccessJob(ExcelJob)}.</li>
     *     <li>Runs {@link CleanupService#cleanupAfterSuccess(ExcelJob)} after success persistence.</li>
     *     <li>Publishes failed DOCX jobs to {@link PipelineQueues#failedQueue()}.</li>
     * </ul>
     *
     * @param job Excel job whose content should be converted into a DOCX report
     *
     * @implNote Failure handling both enqueues the job and persists the failure
     * immediately, so maintainers should keep this behavior aligned with
     * {@link FailedJobMonitor}.
     * @since 1.0
     */
    private void createDocx(ExcelJob job) {
        try {

            LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()),
                    "Start ProcessDocx for excel Job {}", job);

            job.setStatus(JobStatus.DOCX_CREATION_IN_PROGRESS);

            docxService.createDocx(job, PipelineConfig.getDocxOutputDirPath(), PipelineConfig.getDocxPostFileNamePattern(), PipelineConfig.getDocxPostFileName(), PipelineConfig.getExcelPostSheetName());

            job.setStatus(JobStatus.DOCX_CREATED);

            jobPersistenceService.saveSuccessJob(job);

            cleanupService.cleanupAfterSuccess(job);

            LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()),
                    "ProcessDocx completed for Job {}", job.getJobId());

        } catch (Exception e) {
            try {
                LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()), e);
                job.setStatus(JobStatus.FAILED);
                job.setErrorMessage("DOCX failed: " + e.getMessage());

                // DOCX failures are terminal for this job and must be visible to failed-job monitoring.
                queues.failedQueue().put(job);
                jobPersistenceService.saveFailedJob(job);

            } catch (InterruptedException ex) {
                LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()), ex);
                Thread.currentThread().interrupt();
            }
        }
    }
}
