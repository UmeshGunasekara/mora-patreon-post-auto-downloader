/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 2:20 PM
 */
package com.slmora.patreonpostautodownloader.model;

import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The {@code ExcelJob} Class created for
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
@Data
public class ExcelJob
{
    private final long jobId;
//    private final String sourceUrl;
    private final Path excelFile;

    private final List<ImageRecord> imageRecords =
            Collections.synchronizedList(new ArrayList<>());

    private volatile JobStatus status;
    private volatile int retryCount;
    private volatile String errorMessage;

//    public ExcelJob(long jobId, String sourceUrl, Path excelFile) {
//        this.jobId = jobId;
//        this.sourceUrl = sourceUrl;
//        this.excelFile = excelFile;
//    }

    public ExcelJob(long jobId, Path excelFile) {
        this.jobId = jobId;
        this.excelFile = excelFile;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
