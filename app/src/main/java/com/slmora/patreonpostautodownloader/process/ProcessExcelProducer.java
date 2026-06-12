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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The {@code ProcessAExcelProducer} Class created for
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
public class ProcessExcelProducer
{
    private final static MoraLogger LOGGER = MoraLogger.getLogger(ProcessExcelProducer.class);

    private final PipelineConfig config;
    private final PipelineQueues queues;
    private final PipelineState state;
    private final UrlExecutionService urlExecutionService;
    private final ExcelService excelService;
    private final JobPersistenceService jobPersistenceService;

    private final AtomicLong jobIdGenerator = new AtomicLong(1);

    public ProcessExcelProducer(
            PipelineConfig config,
            PipelineQueues queues,
            PipelineState state,
            UrlExecutionService urlExecutionService,
            ExcelService excelService,
            JobPersistenceService jobPersistenceService
    ) {
        this.config = config;
        this.queues = queues;
        this.state = state;
        this.urlExecutionService = urlExecutionService;
        this.excelService = excelService;
        this.jobPersistenceService = jobPersistenceService;
    }

    public void start() {

        int index = 1;
        String filePostStartDate=null;
        try {
            String initUrl = Files.readString(config.urlInputPath).trim();

            if(initUrl.isEmpty()){
                return;
            }

            Files.createDirectories(config.excelOutputDir);
            long jobId = jobIdGenerator.getAndIncrement();

            LOGGER.debug(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                    Thread.currentThread().threadId(),
                    Thread.currentThread().getStackTrace()),"Post URL execution Index : {} and Job-Id : {}", index, jobId);

            processPostUrlForExcel(initUrl, config.excelPostSheetName, config.excelOutputDir, config.excelPostFileName, index, filePostStartDate, jobId);






//            try {
//                Path excelFile = config.excelOutputDir.resolve(
//                        "posts_" + jobId + ".xlsx"
//                );
//
//                List<URLExecute> records =
//                        urlExecutionService.executeUrl(initUrl);
//
//                excelService.createExcelFromRecords(records, excelFile);

//                ExcelJob job = new ExcelJob(jobId, url, excelFile);
//                job.setStatus(JobStatus.EXCEL_CREATED);
//
//                jobPersistenceService.saveJobStatus(job);
//
//                queues.excelReadyQueue().put(job);
//
//                System.out.println("Process A created Excel job: " + jobId);
//
//            } catch (Exception e) {
//                ExcelJob failedJob = new ExcelJob(jobId, url, null);
//                failedJob.setStatus(JobStatus.FAILED);
//                failedJob.setErrorMessage("Process A failed: " + e.getMessage());
//
//                queues.failedQueue().put(failedJob);
//                jobPersistenceService.saveFailedJob(failedJob);
//            }

        } catch (IOException e) {
            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                    Thread.currentThread().threadId(),
                    Thread.currentThread().getStackTrace()), e);
        } finally {
            state.setProcessExcelProducerFinished(true);
            LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                    Thread.currentThread().threadId(),
                    Thread.currentThread().getStackTrace()),"Excel producer process finished");
        }
    }

    private void processPostUrlForExcel(String initUrl, String excelSheetName, Path excelOutputDirPath, String excelFileName, int recursiveIndex, String filePostStartDate, long jobId)
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

            Path outputFilePath = excelOutputDirPath.resolve(excelFileName);
            excelService.createExcelFromRecords(allPosts,  outputFilePath, excelSheetName);

            LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                    Thread.currentThread().threadId(),
                    Thread.currentThread().getStackTrace()),"Excel file created successfully: {}", outputFilePath.toString());
            LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                    Thread.currentThread().threadId(),
                    Thread.currentThread().getStackTrace()),"processPostUrlExcel execute File Start :  {}", filePostStartDate+" Index : "+recursiveIndex);

            if(recursiveIndex<=30) {
                if(recursiveIndex==30){
                    File oldFile = excelOutputDirPath.resolve(excelFileName).toFile();
                    String newFileName = excelFileName.replace("temp",filePostStartDate+"_"+filePostEndDate+"_J"+jobId);
                    File newFile = excelOutputDirPath.resolve(newFileName).toFile();


                    LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()),"Excel file created finalizing for Index {}", recursiveIndex);

                    // Perform the rename operation
//                    Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

                    if (oldFile.renameTo(newFile)) {
//                        System.out.println("Renamed successfully");

                        LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()),"Renamed successfully Excel file name: {}", newFile.getAbsolutePath());
//                        if (oldFile.delete()) {
//                            System.out.println("Deleted successfully.");
//                        } else {
//                            System.out.println("Delete failed.");
//                        }
                    } else {
//                        System.out.println("Rename failed");
                        LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()), "Rename failed for File Name : {}", newFileName);
                    }
                    recursiveIndex=1;
                    filePostStartDate=null;
                    filePostEndDate=null;

                    System.out.println("Start");

                    ExcelJob job = new ExcelJob(jobId, excelOutputDirPath.resolve(newFileName));
                    job.setStatus(JobStatus.EXCEL_CREATED);

                    jobPersistenceService.saveJobStatus(job);

                    queues.excelReadyQueue().put(job);

                    System.out.println("Process A created Excel job: " + jobId);

                    jobId = jobIdGenerator.getAndIncrement();

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    System.out.println("After 500 ms");

                }else {
                    recursiveIndex++;
                }
                processPostUrlForExcel(urlExecute.getNextUrl(), excelSheetName, excelOutputDirPath, excelFileName, recursiveIndex, filePostStartDate, jobId);
            }
        } catch (InterruptedException | IOException e) {
            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                    Thread.currentThread().threadId(),
                    Thread.currentThread().getStackTrace()), e);
        }
    }
}
