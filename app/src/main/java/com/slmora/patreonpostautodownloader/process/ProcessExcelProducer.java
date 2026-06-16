/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 2:24 PM
 */
package com.slmora.patreonpostautodownloader.process;

import com.slmora.common.logging.MoraLogger;
import com.slmora.common.logging.MoraLoggerThreadInfo;
import com.slmora.common.uuid.MoraUuidUtilities;
import com.slmora.patreonpostautodownloader.config.PipelineConfig;
import com.slmora.patreonpostautodownloader.model.ExcelJob;
import com.slmora.patreonpostautodownloader.model.JobStatus;
import com.slmora.patreonpostautodownloader.model.PostRecord;
import com.slmora.patreonpostautodownloader.model.URLExecute;
import com.slmora.patreonpostautodownloader.pipeline.PipelineQueues;
import com.slmora.patreonpostautodownloader.pipeline.PipelineState;
import com.slmora.patreonpostautodownloader.service.ExcelService;
import com.slmora.patreonpostautodownloader.service.JobPersistenceService;
import com.slmora.patreonpostautodownloader.service.UrlExecutionService;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The {@code ProcessExcelProducer} class is created for producing Excel batch
 * jobs from Patreon post API responses.
 * <p>
 * This process reads seed Patreon API URLs from the configured input file,
 * retrieves paginated post data through {@link UrlExecutionService}, appends
 * records into temporary Excel workbooks through {@link ExcelService}, finalizes
 * each batch file after the configured page window, and places the resulting
 * {@link ExcelJob} on {@link PipelineQueues#excelReadyQueue()} for image
 * processing.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Reads and sanitizes seed Patreon API URLs from {@link PipelineConfig#getUrlInputPath()}.</li>
 *     <li>Processes multiple seed URLs concurrently using the configured Excel producer thread count.</li>
 *     <li>Writes paginated Patreon post records into temporary Excel files and finalizes them every 30 pages.</li>
 *     <li>Persists initial job status and publishes completed Excel jobs for image downloading.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link PipelineConfig}<br>
 * 2 - {@link PipelineQueues}<br>
 * 3 - {@link PipelineState}<br>
 * 4 - {@link UrlExecutionService}<br>
 * 5 - {@link ExcelService}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link ProcessExcelProducer#ProcessExcelProducer(PipelineQueues, PipelineState, UrlExecutionService, ExcelService, JobPersistenceService)}</li>
 *     <li>{@link ProcessExcelProducer#start()}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>The class uses manual recursion for Patreon pagination and batch rollover.</li>
 *     <li>Temporary Excel file names use a UUID key until the batch is finalized with post date range and job id.</li>
 *     <li>The producer always marks {@link PipelineState#setProcessExcelProducerFinished(boolean)} in the {@code finally} block of {@link ProcessExcelProducer#start()}.</li>
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
public class ProcessExcelProducer
{
    /**
     * Class-scoped logger used for producer lifecycle, URL processing, file
     * finalization, and queue publication diagnostics.
     */
    private final static MoraLogger LOGGER = MoraLogger.getLogger(ProcessExcelProducer.class);

    /**
     * Shared pipeline queues used to publish finalized Excel jobs.
     */
    private final PipelineQueues queues;

    /**
     * Shared pipeline state used to signal that this producer has no more Excel
     * jobs to publish.
     */
    private final PipelineState state;

    /**
     * Service used to execute Patreon API URLs and parse post records.
     */
    private final UrlExecutionService urlExecutionService;

    /**
     * Service used to append Patreon post records into Excel workbooks.
     */
    private final ExcelService excelService;

    /**
     * Service used to persist job status when an Excel batch is finalized.
     */
    private final JobPersistenceService jobPersistenceService;

    /**
     * Process-level executor used to handle multiple configured seed URLs in
     * parallel.
     */
    private final ExecutorService processExcelPool;

    /**
     * Monotonic job id generator used for Excel batches created by this producer
     * instance.
     */
    private final AtomicLong jobIdGenerator = new AtomicLong(1);

    /**
     * <h3>Create Excel producer process</h3>
     * Creates an Excel producer with the dependencies required for Patreon URL
     * retrieval, workbook creation, status persistence, and queue coordination.
     * <p>
     * The constructor also creates a fixed-size executor whose size is resolved
     * from {@link PipelineConfig#getProcessExcelThreads()}.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Stores shared queues and state used by the producer-worker pipeline.</li>
     *     <li>Stores services for URL execution, Excel writing, and job persistence.</li>
     *     <li>Creates the process executor used by {@link ProcessExcelProducer#start()}.</li>
     * </ul>
     *
     * @param queues shared queues used to publish completed Excel jobs
     * @param state shared pipeline state used to signal producer completion
     * @param urlExecutionService service used to execute Patreon API requests
     * @param excelService service used to write post records into Excel workbooks
     * @param jobPersistenceService service used to persist job status updates
     *
     * @apiNote All process instances in one pipeline execution should share the
     * same {@link PipelineQueues} and {@link PipelineState}.
     * @since 1.0
     */
    public ProcessExcelProducer(
            PipelineQueues queues,
            PipelineState state,
            UrlExecutionService urlExecutionService,
            ExcelService excelService,
            JobPersistenceService jobPersistenceService
    ) {
        this.queues = queues;
        this.state = state;
        this.urlExecutionService = urlExecutionService;
        this.excelService = excelService;
        this.jobPersistenceService = jobPersistenceService;
        this.processExcelPool = Executors.newFixedThreadPool(PipelineConfig.getProcessExcelThreads());
    }

    /**
     * <h3>Start Excel production</h3>
     * Starts URL ingestion and Excel batch production for every configured seed
     * URL.
     * <p>
     * The method reads seed URLs from {@link PipelineConfig#getUrlInputPath()},
     * removes blank entries, submits one task per seed URL to the producer pool,
     * waits for those tasks to finish, and always marks this producer as finished
     * before returning.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Loads all lines from the configured URL input file using UTF-8.</li>
     *     <li>Trims blank entries and stops early when no valid seed URL exists.</li>
     *     <li>Assigns an initial job id for each seed URL and starts recursive page processing.</li>
     *     <li>Shuts down the producer executor and waits up to 24 hours for completion.</li>
     *     <li>Sets the Excel producer finished flag in {@link PipelineState} even when setup fails.</li>
     * </ul>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * ProcessExcelProducer producer = new ProcessExcelProducer(
     *         queues,
     *         state,
     *         urlExecutionService,
     *         excelService,
     *         jobPersistenceService
     * );
     * producer.start();
     * }</pre>
     *
     * @implNote The producer waits for submitted seed URL tasks before setting
     * the finished flag so downstream consumers can combine this flag with queue
     * emptiness checks.
     * @since 1.0
     */
    public void start() {
        try {
            List<String> initUrlList = Files.readAllLines(PipelineConfig.getUrlInputPath(), StandardCharsets.UTF_8);

            List<String> sanitizedInitUrlList = initUrlList.stream()
                    .map(String::trim)
                    .filter(url -> !url.isEmpty())
                    .toList();

            if (sanitizedInitUrlList.isEmpty()) {
                LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()),
                        "No valid seed URLs found at path: {}", PipelineConfig.getUrlInputPath());
                return;
            }

            for (String initUrl : sanitizedInitUrlList) {
                final long initialJobId = jobIdGenerator.getAndIncrement();
                // Each seed URL owns its first job id; recursive pagination may allocate later ids for batch rollover.
                processExcelPool.submit(() -> {
                    try {
                        processPostUrlForExcel(
                                initUrl,
                                PipelineConfig.getExcelPostSheetName(),
                                PipelineConfig.getExcelOutputDirPath(),
                                PipelineConfig.getExcelPostFileName(),
                                1,
                                null,
                                initialJobId,
                                null
                        );
                    } catch (RuntimeException e) {
                        LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                        Thread.currentThread().threadId(),
                                        Thread.currentThread().getStackTrace()), e);
                    }
                });
            }

            processExcelPool.shutdown();

            try {
                boolean completed = processExcelPool.awaitTermination(24, TimeUnit.HOURS);
                if (!completed) {
                    LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                    Thread.currentThread().threadId(),
                                    Thread.currentThread().getStackTrace()),
                            "ProcessExcelProducer timeout reached. Forcing shutdown.");
                    processExcelPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                processExcelPool.shutdownNow();
            }

        } catch (IOException e) {
            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()), e);
        } finally {
            state.setProcessExcelProducerFinished(true);
            LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()),
                    "Excel producer process finished");
        }
    }

    /**
     * <h3>Process Patreon pages into Excel batches</h3>
     * Processes Patreon post pages into temporary Excel files and finalizes
     * completed batches as pipeline jobs.
     * <p>
     * Each invocation fetches the current page, writes records to the active
     * temporary workbook, finalizes the workbook when the 30-page batch window is
     * reached, enqueues the completed job, and continues with the next Patreon
     * pagination URL when present.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Retries the current URL execution until {@link UrlExecutionService#executeUrl(String, int)} returns records.</li>
     *     <li>Uses the first and last post publication dates to build the finalized Excel file name.</li>
     *     <li>Creates a UUID-backed temporary file name for the active batch.</li>
     *     <li>Writes parsed post records to Excel through {@link ExcelService#createExcelFromRecords(List, Path, String)}.</li>
     *     <li>Finalizes and queues a job after 30 pages or when Patreon pagination ends.</li>
     * </ul>
     *
     * @param initUrl current Patreon API URL to execute
     * @param excelSheetName Excel sheet name for post records
     * @param excelOutputDirPath directory where Excel files are written
     * @param excelFileName temporary Excel file name used during batch creation
     * @param recursiveIndex current page index inside the active Excel batch
     * @param filePostStartDate first post date for the active Excel batch
     * @param jobId job identifier assigned to the active Excel batch
     * @param uuidTempKey temporary UUID key embedded in the active Excel file name
     *
     * @implNote The method uses recursion for pagination, carrying batch state
     * through method parameters instead of storing it on the producer instance.
     * @since 1.0
     */
    private void processPostUrlForExcel(String initUrl, String excelSheetName, Path excelOutputDirPath, String excelFileName, int recursiveIndex, String filePostStartDate, long jobId, String uuidTempKey)
    {
        try {
            Optional<URLExecute> records;
            do{
                // Empty Optional signals a recoverable URL execution failure, so this producer retries the same page.
                records = urlExecutionService.executeUrl(initUrl, recursiveIndex);
            }while(records.isEmpty());

            URLExecute urlExecute = records.get();
            List<PostRecord> allPosts = urlExecute.getPostRecordList();

            if(filePostStartDate == null){
                filePostStartDate = OffsetDateTime
                        .parse(allPosts.getFirst().getPublishedAt())
                        .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            }

            String filePostEndDate =OffsetDateTime
                    .parse(allPosts.getLast().getPublishedAt())
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            if(uuidTempKey == null || uuidTempKey.isEmpty()){
                MoraUuidUtilities uuidUtils = new MoraUuidUtilities();

                uuidTempKey = uuidUtils.getUniqueStringUUID(false);
                LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()),
                        "uuidTempKey generated :  {}", uuidTempKey);

                excelFileName = excelFileName.replace("temp",uuidTempKey);
            }

            Path excelOutputFilePath = excelOutputDirPath.resolve(excelFileName);

            excelService.createExcelFromRecords(allPosts,  excelOutputFilePath, excelSheetName);

            LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()),
                    "processPostUrlExcel execute File Start :  {},  Index : {}", filePostStartDate, recursiveIndex);

            if(recursiveIndex<=30) {
                if(recursiveIndex==30){
                    // Finalize at 30 pages to keep downstream Excel, image, and DOCX batches at a manageable size.
                    excelFileFinalizeForJob(excelOutputDirPath,
                            excelFileName,
                            recursiveIndex,
                            filePostStartDate,
                            jobId,
                            uuidTempKey,
                            filePostEndDate);

                    jobId = jobIdGenerator.getAndIncrement();
                    recursiveIndex=1;
                    filePostStartDate=null;
                    uuidTempKey=null;
                    excelFileName=PipelineConfig.getExcelPostFileName();

                    LOGGER.debug(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                    Thread.currentThread().threadId(),
                                    Thread.currentThread().getStackTrace()),
                            "Reset recursive index for next iteration with index : {}",recursiveIndex);

                    LOGGER.debug(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                    Thread.currentThread().threadId(),
                                    Thread.currentThread().getStackTrace()),
                            "Increment new Job ID : {}",jobId);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                    Thread.currentThread().threadId(),
                                    Thread.currentThread().getStackTrace()),
                            "After 1000 ms");

                }else {
                    recursiveIndex++;
                }
                LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()),
                        "processPostUrlExcel execute for next URL :  {}", urlExecute.getNextUrl());
                if(urlExecute.getNextUrl()!=null && !urlExecute.getNextUrl().isEmpty() && !urlExecute.getNextUrl().toLowerCase().equals("null")) {
                    processPostUrlForExcel(urlExecute.getNextUrl(),
                            excelSheetName,
                            excelOutputDirPath,
                            excelFileName,
                            recursiveIndex,
                            filePostStartDate,
                            jobId,
                            uuidTempKey);
                }else {
                    LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                    Thread.currentThread().threadId(),
                                    Thread.currentThread().getStackTrace()),
                            "processPostUrlExcel execute for next URL is null for recursive index {}", recursiveIndex);
                    // Patreon has no next page, so the partially filled batch still needs to become a job.
                    excelFileFinalizeForJob(excelOutputDirPath,
                            excelFileName,
                            recursiveIndex,
                            filePostStartDate,
                            jobId,
                            uuidTempKey,
                            filePostEndDate);
                }
            }
        } catch (InterruptedException | IOException e) {
            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()), e);
        }
    }

    /**
     * <h3>Finalize Excel file for job</h3>
     * Renames the active temporary Excel file into its final job file name and
     * publishes the created {@link ExcelJob}.
     * <p>
     * The final file name replaces the temporary UUID key with the post date
     * range and job id, then the job is marked as {@link JobStatus#EXCEL_CREATED},
     * persisted, and placed on {@link PipelineQueues#excelReadyQueue()}.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Builds the final Excel file name from start date, end date, and job id.</li>
     *     <li>Retries the rename up to three times to handle short-lived file locks.</li>
     *     <li>Creates and persists an {@link ExcelJob} after the rename succeeds.</li>
     *     <li>Publishes the job to the Excel-ready queue for image download processing.</li>
     * </ul>
     *
     * @param excelOutputDirPath directory containing the temporary and final Excel files
     * @param excelFileName active temporary Excel file name
     * @param recursiveIndex current page index used for diagnostic logging
     * @param filePostStartDate first post date included in the finalized Excel file
     * @param jobId job identifier assigned to the finalized batch
     * @param uuidTempKey temporary UUID key to replace in the file name
     * @param filePostEndDate last post date included in the finalized Excel file
     *
     * @throws InterruptedException when queue publication is interrupted
     * @throws RuntimeException when the temporary file cannot be renamed after retry attempts
     * @since 1.0
     */
    private void excelFileFinalizeForJob(Path excelOutputDirPath,
                           String excelFileName,
                           int recursiveIndex,
                           String filePostStartDate,
                           long jobId,
                           String uuidTempKey,
                           String filePostEndDate) throws InterruptedException
    {
        File tempExcelFile = excelOutputDirPath.resolve(excelFileName).toFile();
        String newFileExcelName = excelFileName.replace(uuidTempKey,
                filePostStartDate +"_"+ filePostEndDate +"_J"+ jobId);
        File newExcelFile = excelOutputDirPath.resolve(newFileExcelName).toFile();

        LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                        Thread.currentThread().threadId(),
                        Thread.currentThread().getStackTrace()),
                "Excel file created finalizing for Index {}", recursiveIndex);

        // Retry rename because Windows file handles can briefly remain locked after workbook writes.
        boolean renamed = false;
        for (int attempt = 1; attempt <= 3; attempt++) {
            if (tempExcelFile.renameTo(newExcelFile)) {
                renamed = true;
                LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()),
                        "Renamed successfully Excel file name: {} on attempt {}",newExcelFile.getAbsolutePath(), attempt);
                break;
            }

            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()),
                    "Rename failed for File Name : {} on attempt {}/3",newFileExcelName, attempt);

            if (attempt < 3) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                    Thread.currentThread().threadId(),
                                    Thread.currentThread().getStackTrace()),
                            "Rename retry interrupted for File Name : {}",newFileExcelName);
                    throw new RuntimeException("Rename retry interrupted for file: " + newFileExcelName, e);
                }
            }
        }

        if (!renamed) {
            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()),
                    "Rename failed after 3 attempts for File Name : {}",newFileExcelName);
            throw new RuntimeException("Rename failed after 3 attempts for file: " + newFileExcelName);
        }

        ExcelJob job = new ExcelJob(jobId, excelOutputDirPath.resolve(newFileExcelName));
        job.setStatus(JobStatus.EXCEL_CREATED);
        jobPersistenceService.saveJobStatus(job);
        queues.excelReadyQueue().put(job);

        LOGGER.debug(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                        Thread.currentThread().threadId(),
                        Thread.currentThread().getStackTrace()),
                "Added new ExcelJob in to ready queue : {}",job);
    }
}
