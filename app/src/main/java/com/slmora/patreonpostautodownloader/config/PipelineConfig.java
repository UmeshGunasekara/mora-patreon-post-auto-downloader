/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 2:16 PM
 */
package com.slmora.patreonpostautodownloader.config;

import com.slmora.common.io.properties.MoraAccessProperties;
import com.slmora.common.logging.MoraLogger;
import com.slmora.common.logging.MoraLoggerThreadInfo;
import com.slmora.patreonpostautodownloader.service.UrlExecutionService;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

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
    private final static MoraLogger LOGGER = MoraLogger.getLogger(PipelineConfig.class);

    // One volatile reference gives lock-free reads + safe visibility for all final fields in Snapshot.
    private static volatile Snapshot CURRENT = loadSnapshot();

    private PipelineConfig() {
        // Utility class
    }

    /**     * Optional explicit reload point (e.g., startup only).     * Readers remain lock-free; they either see old snapshot or new snapshot.     */
    public static void reload() {
        CURRENT = loadSnapshot();
    }

    // -------------------- Public API (same static-getter style) --------------------

    public static Path getUrlInputPath() {
        return CURRENT.urlInputPath();
    }

    public static Path getExcelOutputDirPath() {
        return CURRENT.excelOutputDirPath();
    }

    public static Path getImageOutputDirPath() {
        return CURRENT.imageOutputDirPath();
    }

    public static Path getDocxOutputDirPath() {
        return CURRENT.docxOutputDirPath();
    }

    public static Path getFailedOutputDirPath() {
        return CURRENT.failedOutputDirPath();
    }

    public static String getExcelPostSheetName() {
        return CURRENT.excelPostSheetName();
    }

    public static String getExcelPostFileName() {
        return CURRENT.excelPostFileName();
    }

    public static String getDocxPostFileNamePattern() {
        return CURRENT.docxPostFileNamePattern();
    }

    public static String getDocxPostFileName() {
        return CURRENT.docxPostFileName();
    }

    public static int getExcelQueueCapacity() {
        return CURRENT.excelQueueCapacity();
    }

    public static int getDocxQueueCapacity() {
        return CURRENT.docxQueueCapacity();
    }

    public static int getRetryQueueCapacity() {
        return CURRENT.retryQueueCapacity();
    }

    public static int getFailedQueueCapacity() {
        return CURRENT.failedQueueCapacity();
    }

    public static int getProcessImageDownloadThreads() {
        return CURRENT.processImageDownloadThreads();
    }

    public static int getProcessDocxThreads() {
        return CURRENT.processDocxThreads();
    }

    public static int getMaxRetry() {
        return CURRENT.maxRetry();
    }

    public static String getPatreonAccessCookie() {
        return CURRENT.patreonAccessCookie();
    }

    /**     * Safe for logging: masks secret fields.     */
    public static String getToString() {
        return CURRENT.toMaskedString();
    }

    // -------------------- Internal loading --------------------

    private static Snapshot loadSnapshot() {
        try {
            Properties p = new MoraAccessProperties().getAllPropertiesFromResource("app.properties");

            Snapshot s = new Snapshot(
                    requiredPath(p, "APP.URL_INPUT_PATH"),
                    requiredPath(p, "APP.EXCEL_OUTPUT_DIR_PATH"),
                    requiredPath(p, "APP.IMAGE_OUTPUT_DIR_PATH"),
                    requiredPath(p, "APP.DOCX_OUTPUT_DIR_PATH"),
                    requiredPath(p, "APP.FAILD_OUTPUT_DIR_PATH"),

                    p.getProperty("APP.EXCEL_POST_SHEET_NAME", "Posts"),
                    p.getProperty("APP.EXCEL_POST_FILE_NAME", "patreon_posts_output_temp.xlsx"),
                    p.getProperty("APP.DOCX_POST_FILE_NAME_PATTERN", "patreon_posts_output(.*)\\.xlsx"),
                    p.getProperty("APP.DOCX_POST_FILE_NAME", "patreon_posts_report_temp.docx"),

                    requiredInt(p, "APP.EXCEL_QUEUE_CAPACITY", 500),
                    requiredInt(p, "APP.DOCX_QUEUE_CAPACITY", 500),
                    requiredInt(p, "APP.RETRY_QUEUE_CAPACITY", 1000),
                    requiredInt(p, "APP.FAILED_QUEUE_CAPACITY", 5000),
                    requiredInt(p, "APP.PROCESS_IMAGE_DOWNLOAD_THREADS", 10),
                    requiredInt(p, "APP.PROCESS_DOCX_THREADS", 3),
                    requiredInt(p, "APP.MAX_RETRY", 3),

                    requiredString(p, "APP.PATREON_ACCESS_COOKIE=REMOVED_SECRET")
            );

            LOGGER.info(threadInfo(), "PipelineConfig loaded: {}", s.toMaskedString());
            return s;
        } catch (Exception e) {
            LOGGER.error(threadInfo(), "Failed to load PipelineConfig. Keeping previous snapshot if available.", e);

            // If called at class init and this fails, CURRENT is not initialized yet -> fail fast.
            if (CURRENT == null) {
                throw new IllegalStateException("PipelineConfig initialization failed", e);
            }
            return CURRENT;
        }
    }

    private static MoraLoggerThreadInfo threadInfo() {
        Thread t = Thread.currentThread();
        return new MoraLoggerThreadInfo(t.getName(), t.threadId(), t.getStackTrace());
    }

    private static String requiredString(Properties p, String key) {
        String v = p.getProperty(key);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("Missing required property: " + key);
        }
        return v;
    }

    private static String requiredString(Properties p, String key,  String defaultValue) {
        String v = p.getProperty(key, defaultValue);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("Missing required property: " + key);
        }
        return v;
    }

    private static Path requiredPath(Properties p, String key) {
        return Path.of(requiredString(p, key));
    }

    private static int requiredInt(Properties p, String key,  int defaultValue) {
        String raw = requiredString(p, key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid integer for property " + key + ": " + raw, ex);
        }
    }

    // Immutable config snapshot. Final fields + volatile reference publication = thread-safe lock-free reads.
    private record Snapshot(
            Path urlInputPath,
            Path excelOutputDirPath,
            Path imageOutputDirPath,
            Path docxOutputDirPath,
            Path failedOutputDirPath,

            String excelPostSheetName,
            String excelPostFileName,
            String docxPostFileNamePattern,
            String docxPostFileName,

            int excelQueueCapacity,
            int docxQueueCapacity,
            int retryQueueCapacity,
            int failedQueueCapacity,
            int processImageDownloadThreads,
            int processDocxThreads,
            int maxRetry,

            String patreonAccessCookie
    ) {
        private Snapshot {
            Objects.requireNonNull(urlInputPath, "urlInputPath");
            Objects.requireNonNull(excelOutputDirPath, "excelOutputDirPath");
            Objects.requireNonNull(imageOutputDirPath, "imageOutputDirPath");
            Objects.requireNonNull(docxOutputDirPath, "docxOutputDirPath");
            Objects.requireNonNull(failedOutputDirPath, "failedOutputDirPath");
            Objects.requireNonNull(excelPostSheetName, "excelPostSheetName");
            Objects.requireNonNull(excelPostFileName, "excelPostFileName");
            Objects.requireNonNull(docxPostFileNamePattern, "docxPostFileNamePattern");
            Objects.requireNonNull(docxPostFileName, "docxPostFileName");
            Objects.requireNonNull(patreonAccessCookie, "patreonAccessCookie");
        }

        String toMaskedString() {
            return "PipelineConfig{" +
                    "urlInputPath=" + urlInputPath +
                    ", excelOutputDirPath=" + excelOutputDirPath +
                    ", imageOutputDirPath=" + imageOutputDirPath +
                    ", docxOutputDirPath=" + docxOutputDirPath +
                    ", faildOutputDirPath=" + failedOutputDirPath +
                    ", excelPostSheetName='" + excelPostSheetName + '\'' +
                    ", excelPostFileName='" + excelPostFileName + '\'' +
                    ", docxPostFileNamePattern='" + docxPostFileNamePattern + '\'' +
                    ", docxPostFileName='" + docxPostFileName + '\'' +
                    ", excelQueueCapacity=" + excelQueueCapacity +
                    ", docxQueueCapacity=" + docxQueueCapacity +
                    ", retryQueueCapacity=" + retryQueueCapacity +
                    ", failedQueueCapacity=" + failedQueueCapacity +
                    ", processImageDownloadThreads=" + processImageDownloadThreads +
                    ", processDocxThreads=" + processDocxThreads +
                    ", maxRetry=" + maxRetry +
                    ", patreonAccessCookie='***'" +
                    '}';
        }
    }
}
