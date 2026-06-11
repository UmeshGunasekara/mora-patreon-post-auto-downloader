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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
public class ProcessAExcelProducer
{

    private final static MoraLogger LOGGER = MoraLogger.getLogger(ProcessAExcelProducer.class);

    private final String urlInputPath = "E:\\MORA\\MyBusiness\\Investment\\JU\\AUTO\\input_url.txt";
    String outputDirPath = "E:\\MORA\\MyBusiness\\Investment\\JU\\AUTO\\";
    String fileName = "patreon_posts_output_temp.xlsx";
    String sheetName = "Posts"; // or specify your sheet name



    private final PipelineConfig config;
    private final PipelineQueues queues;
    private final PipelineState state;
    private final UrlExecutionService urlExecutionService;
    private final ExcelService excelService;
    private final JobPersistenceService jobPersistenceService;

    private final AtomicLong jobIdGenerator = new AtomicLong(1);

    public ProcessAExcelProducer(
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
        String fileStart=null;
        String fileEnd=null;
        try {
            String initUrl = Files.readString(config.urlInputPath).trim();

            Files.createDirectories(config.excelOutputDir);///k
            long jobId = jobIdGenerator.getAndIncrement();

            processPostUrlExcel(initUrl, config.excelPostSheetName, config.excelOutputDir, config.excelPostFileName, index, fileStart, fileEnd, jobId);

//            System.out.printf("Index : "+index);
            LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                    Thread.currentThread().getId(),
                    Thread.currentThread().getStackTrace()),"Index {}", index);




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

        } catch (Exception e) {
//            e.printStackTrace();
            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                    Thread.currentThread().getId(),
                    Thread.currentThread().getStackTrace()), e);
        } finally {
            state.setProcessAFinished(true);
            System.out.println("Process A finished.");
        }
    }

    private void processPostUrlExcel(String initUrl, String sheetName, Path outputDirPath, String fileName, int index, String fileStart, String fileEnd, long jobId)
    {
        try {
            List<PostRecord> allPosts;
            URLExecute urlExecute;

            Optional<URLExecute> records;
            do{
                records = urlExecutionService.executeUrl(initUrl);
            }while(!records.isPresent());

            urlExecute = records.get();
            allPosts = urlExecute.getPostRecordList();

            if(fileStart == null){
                fileStart = OffsetDateTime
                        .parse(allPosts.getFirst().getPublishedAt())
                        .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            }

            fileEnd =OffsetDateTime
                    .parse(allPosts.getLast().getPublishedAt())
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            Path outputFilePath = outputDirPath.resolve(fileName);

            excelService.createExcelFromRecords(allPosts,  outputFilePath, sheetName);

//            System.out.println("Excel file created successfully: " + outputDirPath+fileName);
//            System.out.println("processPostUrlExcel execute File Start : "+fileStart+" Index : "+index);

            LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                    Thread.currentThread().getId(),
                    Thread.currentThread().getStackTrace()),"Excel file created successfully: {}", outputFilePath.toString());

            LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                    Thread.currentThread().getId(),
                    Thread.currentThread().getStackTrace()),"processPostUrlExcel execute File Start :  {}", fileStart+" Index : "+index);
            if(index<=30) {
                if(index==30){
                    File oldFile = outputDirPath.resolve(fileName).toFile();
                    String newFileName = fileName.replace("temp",fileStart+"_"+fileEnd+"_J"+jobId);
                    File newFile = outputDirPath.resolve(newFileName).toFile();


                    LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().getId(),
                            Thread.currentThread().getStackTrace()),"Excel file created finalizing for Index {}", index);

                    // Perform the rename operation
//                    Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

                    if (oldFile.renameTo(newFile)) {
//                        System.out.println("Renamed successfully");

                        LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().getId(),
                                Thread.currentThread().getStackTrace()),"Renamed successfully Excel file name: {}", newFile.getAbsolutePath());
//                        if (oldFile.delete()) {
//                            System.out.println("Deleted successfully.");
//                        } else {
//                            System.out.println("Delete failed.");
//                        }
                    } else {
//                        System.out.println("Rename failed");
                        LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().getId(),
                                Thread.currentThread().getStackTrace()), "Rename failed for File Name : {}", newFileName);
                    }
                    index=1;
                    fileStart=null;
                    fileEnd=null;

                    System.out.println("Start");

                    ExcelJob job = new ExcelJob(jobId, outputDirPath.resolve(newFileName));
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
                    index++;
                }
                processPostUrlExcel(urlExecute.getNextUrl(), sheetName, outputDirPath, fileName, index, fileStart, fileEnd, jobId);
            }
        } catch (Exception e) {
//            System.err.println("Fatal error: " + e.getMessage());
//            e.printStackTrace();
            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                    Thread.currentThread().getId(),
                    Thread.currentThread().getStackTrace()), e);
        }
    }
}
