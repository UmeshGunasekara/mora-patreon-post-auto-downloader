/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 3:45 PM
 */
package com.slmora.patreonpostautodownloader.service;

import com.slmora.common.logging.MoraLogger;
import com.slmora.common.logging.MoraLoggerThreadInfo;
import com.slmora.patreonpostautodownloader.config.PipelineConfig;
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
    private final static MoraLogger LOGGER = MoraLogger.getLogger(CleanupService.class);

    public void cleanupAfterSuccess(ExcelJob job) {
        try {
            // Optional:
            // Files.deleteIfExists(job.getExcelFile());
            LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()),
                    "Cleanup completed for job: {}", job.getJobId());

        } catch (Exception e) {
            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()),
                    "Cleanup failed for job: {} with {}", job.getJobId(), e);
        }
    }

    public void deleteExcelFile(ExcelJob job) throws Exception {
        if (job.getExcelFile() != null) {
            Files.deleteIfExists(job.getExcelFile());
        }
    }
}
