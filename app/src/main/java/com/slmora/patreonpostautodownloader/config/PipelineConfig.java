/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 2:16 PM
 */
package com.slmora.patreonpostautodownloader.config;

import java.nio.file.Path;

/**
 * The {@code PipelineConfig} Class created for
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
public class PipelineConfig
{
    public int processBThreads = 10;
    public int processCThreads = 3;

    public int maxRetry = 3;

    public int excelQueueCapacity = 500;
    public int docxQueueCapacity = 500;
    public int retryQueueCapacity = 1000;
    public int failedQueueCapacity = 5000;

    public String excelPostSheetName = "posts";
    public String excelPostFileName = "patreon_posts_output_temp.xlsx";

    public String docxPostFileNamePattern = "patreon_posts_output(.*)\\.xlsx";
    public String docxPostFileName = "patreon_posts_report_temp.docx";

    public Path excelOutputDir = Path.of("E:\\MORA\\MyBusiness\\Investment\\JU\\AUTO02\\Post\\Excel\\");
    public Path imageOutputDir = Path.of("E:\\MORA\\MyBusiness\\Investment\\JU\\AUTO02\\Image\\");
    public Path docxOutputDir = Path.of("E:\\MORA\\MyBusiness\\Investment\\JU\\AUTO02\\Post\\Docx\\");
    public Path failedOutputDir = Path.of("E:\\MORA\\MyBusiness\\Investment\\JU\\AUTO02\\Faild\\");

    public final Path urlInputPath = Path.of("E:\\MORA\\MyBusiness\\Investment\\JU\\AUTO02\\input_url.txt");

    @Override
    public String toString()
    {
        return "PipelineConfig{" +
                "processBThreads=" + processBThreads +
                ", processCThreads=" + processCThreads +
                ", maxRetry=" + maxRetry +
                ", excelQueueCapacity=" + excelQueueCapacity +
                ", docxQueueCapacity=" + docxQueueCapacity +
                ", retryQueueCapacity=" + retryQueueCapacity +
                ", failedQueueCapacity=" + failedQueueCapacity +
                ", excelPostSheetName='" + excelPostSheetName + '\'' +
                ", excelPostFileName='" + excelPostFileName + '\'' +
                ", docxPostFileNamePattern='" + docxPostFileNamePattern + '\'' +
                ", docxPostFileName='" + docxPostFileName + '\'' +
                ", excelOutputDir=" + excelOutputDir +
                ", imageOutputDir=" + imageOutputDir +
                ", docxOutputDir=" + docxOutputDir +
                ", failedOutputDir=" + failedOutputDir +
                ", urlInputPath=" + urlInputPath +
                '}';
    }
}
