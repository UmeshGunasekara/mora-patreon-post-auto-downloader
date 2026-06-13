/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 3:45 PM
 */
package com.slmora.patreonpostautodownloader.service;

import com.slmora.common.logging.MoraLoggerThreadInfo;
import com.slmora.patreonpostautodownloader.model.ExcelJob;

import java.io.IOException;
import java.nio.file.Files;

/**
 * The {@code CleanupService} Class created for
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
public class CleanupService
{
    public void cleanupAfterSuccess(ExcelJob job) {
        try {
            // Optional:
            // Files.deleteIfExists(job.getExcelFile());

            System.out.println("Cleanup completed for job: " + job.getJobId());

        } catch (Exception e) {
            System.err.println(
                    "Cleanup failed for job "
                            + job.getJobId()
                            + ": "
                            + e.getMessage()
            );
        }
    }

    public void deleteExcelFile(ExcelJob job) throws Exception {
        if (job.getExcelFile() != null) {
            Files.deleteIfExists(job.getExcelFile());
        }
    }

    private static MoraLoggerThreadInfo threadInfo() {
        Thread t = Thread.currentThread();
        return new MoraLoggerThreadInfo(t.getName(), t.threadId(), t.getStackTrace());
    }
}
