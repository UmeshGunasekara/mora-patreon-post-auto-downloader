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
 * Produces Excel batches from Patreon post API responses.
 * <p>
 * This process reads the initial Patreon API URL from configuration, retrieves
 * paginated post data through {@link UrlExecutionService}, appends the records
 * into a temporary Excel workbook through {@link ExcelService}, finalizes each
 * batch file after the configured page window, and places the resulting
 * {@link ExcelJob} on the Excel-ready queue for image processing.
 * </p>
 *
 * <p>Methods:</p>
 * <ul>
 *     <li>{@link #ProcessExcelProducer(PipelineConfig, PipelineQueues, PipelineState, UrlExecutionService, ExcelService, JobPersistenceService)} - creates the producer with its shared pipeline dependencies.</li>
 *     <li>{@link #start()} - starts URL ingestion and marks the producer as finished when complete.</li>
 *     <li>{@link #processPostUrlForExcel(String, String, Path, String, int, String, long)} - recursively fetches post pages, writes Excel rows, finalizes batch files, and enqueues jobs.</li>
 * </ul>
 *
 * <p>Key responsibilities:</p>
 * <ul>
 *     <li>Read the seed Patreon API URL from {@link PipelineConfig#urlInputPath}.</li>
 *     <li>Create and maintain temporary Excel files for downloaded post records.</li>
 *     <li>Finalize each Excel batch with date and job identifiers in the file name.</li>
 *     <li>Persist Excel job status and publish completed jobs to {@link PipelineQueues#excelReadyQueue()}.</li>
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
    private final static MoraLogger LOGGER = MoraLogger.getLogger(ProcessExcelProducer.class);

    private final PipelineQueues queues;
    private final PipelineState state;
    private final UrlExecutionService urlExecutionService;
    private final ExcelService excelService;
    private final JobPersistenceService jobPersistenceService;
    private final ExecutorService processExcelPool;

    private final AtomicLong jobIdGenerator = new AtomicLong(1);

    /**
     * Creates an Excel producer with the dependencies required for URL retrieval,
     * workbook creation, status persistence, and queue coordination.
     *
     * @param config pipeline configuration containing input and output paths
     * @param queues shared queues used to publish completed Excel jobs
     * @param state shared pipeline state used to signal producer completion
     * @param urlExecutionService service used to execute Patreon API requests
     * @param excelService service used to write post records into Excel workbooks
     * @param jobPersistenceService service used to persist job status updates
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
     * Starts the Excel producer process.
     * <p>
     * The method reads the initial URL, prepares the Excel output directory,
     * starts recursive post-page processing, and always marks this producer as
     * finished before returning.
     * </p>
     */
    public void start() {
        try {
            List<String> initUrlList = Files.readAllLines(PipelineConfig.getUrlInputPath(), StandardCharsets.UTF_8);

            List<String> sanitizedInitUrlList = initUrlList.stream()
                    .map(String::trim)
                    .filter(url -> !url.isEmpty())
                    .toList();

            if (sanitizedInitUrlList.isEmpty()) {
                LOGGER.error(threadInfo(), "No valid seed URLs found at path: {}", PipelineConfig.getUrlInputPath());
                return;
            }

            for (String initUrl : sanitizedInitUrlList) {
                final long initialJobId = jobIdGenerator.getAndIncrement();
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
                        LOGGER.error(threadInfo(), e);
                    }
                });
            }

            processExcelPool.shutdown();

            try {
                boolean completed = processExcelPool.awaitTermination(24, TimeUnit.HOURS);
                if (!completed) {
                    LOGGER.error(threadInfo(), "ProcessExcelProducer timeout reached. Forcing shutdown.");
                    processExcelPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                processExcelPool.shutdownNow();
            }

        } catch (IOException e) {
            LOGGER.error(threadInfo(), e);
        } finally {
            state.setProcessExcelProducerFinished(true);
            LOGGER.info(threadInfo(),"Excel producer process finished");
        }
    }

    /**
     * Processes Patreon post pages into Excel batches.
     * <p>
     * Each invocation fetches the current page, writes records to the temporary
     * workbook, finalizes the workbook when the batch window is reached, enqueues
     * the completed job, and then continues with the next pagination URL.
     * </p>
     *
     * @param initUrl current Patreon API URL to execute
     * @param excelSheetName Excel sheet name for post records
     * @param excelOutputDirPath directory where Excel files are written
     * @param excelFileName temporary Excel file name used during batch creation
     * @param recursiveIndex current page index inside the active Excel batch
     * @param filePostStartDate first post date for the active Excel batch
     * @param jobId job identifier assigned to the active Excel batch
     */
    private void processPostUrlForExcel(String initUrl, String excelSheetName, Path excelOutputDirPath, String excelFileName, int recursiveIndex, String filePostStartDate, long jobId, String uuidTempKey)
    {
        try {
            Optional<URLExecute> records;
            do{
                records = urlExecutionService.executeUrl(initUrl);
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
                LOGGER.info(threadInfo(),"uuidTempKey generated :  {}", uuidTempKey);

                excelFileName = excelFileName.replace("temp",uuidTempKey);
            }


            Path excelOutputFilePath = excelOutputDirPath.resolve(excelFileName);

            excelService.createExcelFromRecords(allPosts,  excelOutputFilePath, excelSheetName);

            LOGGER.info(threadInfo(),"processPostUrlExcel execute File Start :  {},  Index : {}", filePostStartDate, recursiveIndex);

            if(recursiveIndex<=30) {
                if(recursiveIndex==30){
                    File tempExcelFile = excelOutputDirPath.resolve(excelFileName).toFile();
                    String newFileExcelName = excelFileName.replace(uuidTempKey,filePostStartDate+"_"+filePostEndDate+"_J"+jobId);
                    File newExcelFile = excelOutputDirPath.resolve(newFileExcelName).toFile();

                    LOGGER.info(threadInfo(),"Excel file created finalizing for Index {}", recursiveIndex);

                    // Retry rename because Windows file handles can briefly remain locked after workbook writes.
                    boolean renamed = false;
                    for (int attempt = 1; attempt <= 3; attempt++) {
                        if (tempExcelFile.renameTo(newExcelFile)) {
                            renamed = true;
                            LOGGER.info(threadInfo(),"Renamed successfully Excel file name: {} on attempt {}",newExcelFile.getAbsolutePath(), attempt);
                            break;
                        }

                        LOGGER.error(threadInfo(),"Rename failed for File Name : {} on attempt {}/3",newFileExcelName, attempt);

                        if (attempt < 3) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                LOGGER.error(threadInfo(),"Rename retry interrupted for File Name : {}",newFileExcelName);
                                throw new RuntimeException("Rename retry interrupted for file: " + newFileExcelName, e);
                            }
                        }
                    }

                    if (!renamed) {
                        LOGGER.error(threadInfo(),"Rename failed after 3 attempts for File Name : {}",newFileExcelName);
                        throw new RuntimeException("Rename failed after 3 attempts for file: " + newFileExcelName);
                    }

                    recursiveIndex=1;
                    filePostStartDate=null;
                    uuidTempKey=null;
                    excelFileName=PipelineConfig.getExcelPostFileName();

                    LOGGER.debug(threadInfo(),"Reset recursive index for next iteration with index : {}",recursiveIndex);

                    ExcelJob job = new ExcelJob(jobId, excelOutputDirPath.resolve(newFileExcelName));
                    job.setStatus(JobStatus.EXCEL_CREATED);
                    jobPersistenceService.saveJobStatus(job);
                    queues.excelReadyQueue().put(job);

                    LOGGER.debug(threadInfo(),"Added new ExcelJob in to ready queue : {}",job);

                    jobId = jobIdGenerator.getAndIncrement();
                    LOGGER.debug(threadInfo(),"Increment new Job ID : {}",jobId);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    LOGGER.info(threadInfo(),"After 1000 ms");

                }else {
                    recursiveIndex++;
                }
                LOGGER.info(threadInfo(),"processPostUrlExcel execute for next URL :  {}", urlExecute.getNextUrl());
                processPostUrlForExcel(urlExecute.getNextUrl(), excelSheetName, excelOutputDirPath, excelFileName, recursiveIndex, filePostStartDate, jobId, uuidTempKey);
            }
        } catch (InterruptedException | IOException e) {
            LOGGER.error(threadInfo(), e);
        }
    }

    private static MoraLoggerThreadInfo threadInfo() {
        Thread t = Thread.currentThread();
        return new MoraLoggerThreadInfo(t.getName(), t.threadId(), t.getStackTrace());
    }
}
