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
 * The {@code PipelineConfig} class is created for loading and exposing runtime
 * configuration used by the Patreon post download pipeline.
 * <p>
 * The class reads {@code app.properties} with values resolved from the local
 * {@code .env} file, stores the resolved values in an immutable {@link Snapshot},
 * and exposes those values through static getters used by pipeline controllers,
 * workers, services, and queue factories.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Loads required file-system paths, queue capacities, worker counts, retry limits, and Patreon authentication data.</li>
 *     <li>Publishes configuration through a volatile immutable snapshot for lock-free reads across pipeline threads.</li>
 *     <li>Supports explicit reload while keeping the previous snapshot when reload fails after successful initialization.</li>
 *     <li>Masks the Patreon access cookie when configuration is converted for logging.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link MoraAccessProperties}<br>
 * 2 - {@link UrlExecutionService}<br>
 * 3 - {@link Snapshot}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link PipelineConfig#reload()}</li>
 *     <li>{@link PipelineConfig#getUrlInputPath()}</li>
 *     <li>{@link PipelineConfig#getExcelOutputDirPath()}</li>
 *     <li>{@link PipelineConfig#getImageOutputDirPath()}</li>
 *     <li>{@link PipelineConfig#getDocxOutputDirPath()}</li>
 *     <li>{@link PipelineConfig#getFailedOutputDirPath()}</li>
 *     <li>{@link PipelineConfig#getPatreonAccessCookie()}</li>
 *     <li>{@link PipelineConfig#getToString()}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>The configured {@code APP.PATREON_ACCESS_COOKIE} is sensitive and must not be written to logs directly.</li>
 *     <li>The {@code .env} path is currently fixed to this project workspace and should be reviewed before moving the application.</li>
 *     <li>All getters return values from the currently published snapshot and do not reload properties automatically.</li>
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
    /**
     * Class-scoped logger used for configuration load, validation, reload, and
     * initialization failure messages.
     */
    private final static MoraLogger LOGGER = MoraLogger.getLogger(PipelineConfig.class);

    /**
     * Currently published configuration values.
     * <p>
     * A single volatile reference gives lock-free reads and safe visibility for
     * all final fields contained in {@link Snapshot}.
     * </p>
     */
    private static volatile Snapshot CURRENT = loadSnapshot();

    /**
     * <h3>Prevent configuration utility instantiation</h3>
     * Prevents creating instances because configuration is owned and exposed by
     * the static snapshot API.
     *
     * @since 1.0
     */
    private PipelineConfig() {
        // Utility class
    }

    /**
     * <h3>Reload pipeline configuration</h3>
     * Reloads {@code app.properties} and environment-backed values into a new
     * immutable configuration snapshot.
     * <p>
     * Readers remain lock-free while reload is running. They either observe the
     * previous snapshot or the newly loaded snapshot after the volatile reference
     * is updated.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Loads all configured paths, names, queue capacities, worker counts, retry limits, and Patreon cookie values.</li>
     *     <li>Publishes the new snapshot only after all required values are resolved and validated.</li>
     *     <li>Keeps the existing snapshot when reload fails after successful class initialization.</li>
     * </ul>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * PipelineConfig.reload();
     * }</pre>
     *
     * @implNote The volatile assignment is the publication point for a complete
     * immutable snapshot.
     * @apiNote Call this method only when the application intentionally wants to
     * refresh configuration. Normal getters do not reload automatically.
     * @since 1.0
     */
    public static void reload() {
        CURRENT = loadSnapshot();
    }

    /**
     * <h3>Resolve URL input path</h3>
     * Returns the local file path used by the Excel producer to read seed
     * Patreon API URLs.
     *
     * @return configured {@code APP.URL_INPUT_PATH} as a {@link Path}
     * @since 1.0
     */
    public static Path getUrlInputPath() {
        return CURRENT.urlInputPath();
    }

    /**
     * <h3>Resolve Excel output directory</h3>
     * Returns the directory where generated batch Excel files are written.
     *
     * @return configured {@code APP.EXCEL_OUTPUT_DIR_PATH} as a {@link Path}
     * @since 1.0
     */
    public static Path getExcelOutputDirPath() {
        return CURRENT.excelOutputDirPath();
    }

    /**
     * <h3>Resolve image output directory</h3>
     * Returns the directory where downloaded Patreon images are stored.
     *
     * @return configured {@code APP.IMAGE_OUTPUT_DIR_PATH} as a {@link Path}
     * @since 1.0
     */
    public static Path getImageOutputDirPath() {
        return CURRENT.imageOutputDirPath();
    }

    /**
     * <h3>Resolve DOCX output directory</h3>
     * Returns the directory where generated DOCX reports are written.
     *
     * @return configured {@code APP.DOCX_OUTPUT_DIR_PATH} as a {@link Path}
     * @since 1.0
     */
    public static Path getDocxOutputDirPath() {
        return CURRENT.docxOutputDirPath();
    }

    /**
     * <h3>Resolve failed output directory</h3>
     * Returns the directory used for failed-job and job-status log output.
     *
     * @return configured {@code APP.FAILED_OUTPUT_DIR_PATH} as a {@link Path}
     * @since 1.0
     */
    public static Path getFailedOutputDirPath() {
        return CURRENT.failedOutputDirPath();
    }

    /**
     * <h3>Resolve Excel post sheet name</h3>
     * Returns the sheet name expected when writing and reading post rows.
     *
     * @return configured {@code APP.EXCEL_POST_SHEET_NAME}, or {@code Posts} when the property is absent
     * @since 1.0
     */
    public static String getExcelPostSheetName() {
        return CURRENT.excelPostSheetName();
    }

    /**
     * <h3>Resolve Excel post file name</h3>
     * Returns the temporary Excel file name used while the producer is writing
     * post batches.
     *
     * @return configured {@code APP.EXCEL_POST_FILE_NAME}, or {@code patreon_posts_output_temp.xlsx} when absent
     * @since 1.0
     */
    public static String getExcelPostFileName() {
        return CURRENT.excelPostFileName();
    }

    /**
     * <h3>Resolve DOCX source file pattern</h3>
     * Returns the regular expression used by document generation to derive final
     * DOCX names from completed Excel batch files.
     *
     * @return configured {@code APP.DOCX_POST_FILE_NAME_PATTERN}, or the default Patreon Excel pattern when absent
     * @since 1.0
     */
    public static String getDocxPostFileNamePattern() {
        return CURRENT.docxPostFileNamePattern();
    }

    /**
     * <h3>Resolve DOCX post file name</h3>
     * Returns the template DOCX file name used by the report generation step.
     *
     * @return configured {@code APP.DOCX_POST_FILE_NAME}, or {@code patreon_posts_report_temp.docx} when absent
     * @since 1.0
     */
    public static String getDocxPostFileName() {
        return CURRENT.docxPostFileName();
    }

    /**
     * <h3>Resolve Excel queue capacity</h3>
     * Returns the bounded capacity for the queue that carries completed Excel jobs.
     *
     * @return configured {@code APP.EXCEL_QUEUE_CAPACITY}, or {@code 500} when absent
     * @since 1.0
     */
    public static int getExcelQueueCapacity() {
        return CURRENT.excelQueueCapacity();
    }

    /**
     * <h3>Resolve DOCX queue capacity</h3>
     * Returns the bounded capacity for the queue that carries jobs ready for DOCX
     * generation.
     *
     * @return configured {@code APP.DOCX_QUEUE_CAPACITY}, or {@code 500} when absent
     * @since 1.0
     */
    public static int getDocxQueueCapacity() {
        return CURRENT.docxQueueCapacity();
    }

    /**
     * <h3>Resolve retry queue capacity</h3>
     * Returns the bounded capacity for image download retry jobs.
     *
     * @return configured {@code APP.RETRY_QUEUE_CAPACITY}, or {@code 1000} when absent
     * @since 1.0
     */
    public static int getRetryQueueCapacity() {
        return CURRENT.retryQueueCapacity();
    }

    /**
     * <h3>Resolve failed queue capacity</h3>
     * Returns the bounded capacity for jobs that have permanently failed or need
     * failure persistence.
     *
     * @return configured {@code APP.FAILED_QUEUE_CAPACITY}, or {@code 5000} when absent
     * @since 1.0
     */
    public static int getFailedQueueCapacity() {
        return CURRENT.failedQueueCapacity();
    }

    /**
     * <h3>Resolve image download worker count</h3>
     * Returns the number of process-level image download workers configured for
     * the pipeline.
     *
     * @return configured {@code APP.PROCESS_IMAGE_DOWNLOAD_THREADS}, or {@code 10} when absent
     * @since 1.0
     */
    public static int getProcessImageDownloadThreads() {
        return CURRENT.processImageDownloadThreads();
    }

    /**
     * <h3>Resolve DOCX worker count</h3>
     * Returns the number of DOCX producer workers configured for report generation.
     *
     * @return configured {@code APP.PROCESS_DOCX_THREADS}, or {@code 3} when absent
     * @since 1.0
     */
    public static int getProcessDocxThreads() {
        return CURRENT.processDocxThreads();
    }

    /**
     * <h3>Resolve Excel worker count</h3>
     * Returns the number of Excel producer workers configured for URL pagination
     * and workbook generation.
     *
     * @return configured {@code APP.PROCESS_EXCEL_THREADS}, or {@code 3} when absent
     * @since 1.0
     */
    public static int getProcessExcelThreads() {
        return CURRENT.processExcelThreads();
    }

    /**
     * <h3>Resolve maximum retry count</h3>
     * Returns the maximum number of retry attempts used for failed image download
     * jobs.
     *
     * @return configured {@code APP.MAX_RETRY}, or {@code 3} when absent
     * @since 1.0
     */
    public static int getMaxRetry() {
        return CURRENT.maxRetry();
    }

    /**
     * <h3>Resolve Patreon access cookie</h3>
     * Returns the configured Patreon access cookie used by
     * {@link UrlExecutionService} when calling Patreon API URLs.
     *
     * @return required {@code APP.PATREON_ACCESS_COOKIE} value
     * @apiNote Treat the returned value as sensitive. Use {@link PipelineConfig#getToString()} for logging.
     * @since 1.0
     */
    public static String getPatreonAccessCookie() {
        return CURRENT.patreonAccessCookie();
    }

    /**
     * <h3>Create masked configuration text</h3>
     * Returns a log-safe representation of the currently published configuration.
     * <p>
     * Sensitive values, including the Patreon access cookie, are masked before
     * the string is returned.
     * </p>
     *
     * @return masked text representation of the current {@link Snapshot}
     * @since 1.0
     */
    public static String getToString() {
        return CURRENT.toMaskedString();
    }

    /**
     * <h3>Load configuration snapshot</h3>
     * Loads all pipeline configuration values from {@code app.properties} and
     * the configured local {@code .env} file.
     * <p>
     * The method validates required values, applies defaults for optional
     * settings, logs the masked result, and returns the immutable snapshot to be
     * published by the caller.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Reads property values through {@link MoraAccessProperties}.</li>
     *     <li>Converts path and integer values into their runtime types.</li>
     *     <li>Fails fast during class initialization when no previous snapshot exists.</li>
     *     <li>Returns the previous snapshot when a later reload fails.</li>
     * </ul>
     *
     * @return newly loaded {@link Snapshot}, or the previous snapshot when reload fails after initialization
     * @throws IllegalStateException when initial configuration loading fails before a snapshot exists
     * @since 1.0
     */
    private static Snapshot loadSnapshot() {
        try {
            Properties p = new MoraAccessProperties().getAllPropertiesFromResource("app.properties",Path.of("D:\\SLMORAWorkSpace\\IntelliJProjects\\slmora-project\\mora-patreon-post-auto-downloader\\.env"));

            Snapshot s = new Snapshot(
                    requiredPath(p, "APP.URL_INPUT_PATH"),
                    requiredPath(p, "APP.EXCEL_OUTPUT_DIR_PATH"),
                    requiredPath(p, "APP.IMAGE_OUTPUT_DIR_PATH"),
                    requiredPath(p, "APP.DOCX_OUTPUT_DIR_PATH"),
                    requiredPath(p, "APP.FAILED_OUTPUT_DIR_PATH"),

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
                    requiredInt(p, "APP.PROCESS_EXCEL_THREADS", 3),
                    requiredInt(p, "APP.MAX_RETRY", 3),

                    requiredString(p, "APP.PATREON_ACCESS_COOKIE")
            );

            LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                    Thread.currentThread().threadId(),
                    Thread.currentThread().getStackTrace()),
                    "PipelineConfig loaded: {}", s.toMaskedString());
            return s;
        } catch (Exception e) {
            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()),
                    "Failed to load PipelineConfig. Keeping previous snapshot if available.", e);

            // During class initialization there is no previous snapshot, so startup must fail immediately.
            if (CURRENT == null) {
                LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()),
                        "PipelineConfig initialization failed");
                throw new IllegalStateException("PipelineConfig initialization failed", e);
            }
            return CURRENT;
        }
    }

    /**
     * <h3>Read required string property</h3>
     * Reads a required property value and rejects missing or blank text.
     *
     * @param p source properties loaded from the application configuration
     * @param key property key to resolve
     *
     * @return non-blank property value
     * @throws IllegalArgumentException when the property is missing or blank
     * @since 1.0
     */
    private static String requiredString(Properties p, String key) {
        String v = p.getProperty(key);
        if (v == null || v.isBlank()) {
            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()),
                    "Missing required property: {}", key);
            throw new IllegalArgumentException("Missing required property: " + key);
        }
        return v;
    }

    /**
     * <h3>Read string property with default</h3>
     * Reads a property value using the supplied default and rejects the final
     * value when it is blank.
     *
     * @param p source properties loaded from the application configuration
     * @param key property key to resolve
     * @param defaultValue fallback value used when the property is absent
     *
     * @return non-blank property value or non-blank default value
     * @throws IllegalArgumentException when the resolved value is blank
     * @since 1.0
     */
    private static String requiredString(Properties p, String key,  String defaultValue) {
        String v = p.getProperty(key, defaultValue);
        if (v == null || v.isBlank()) {
            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()),
                    "Missing required property: {}", key);
            throw new IllegalArgumentException("Missing required property: " + key);
        }
        return v;
    }

    /**
     * <h3>Read required path property</h3>
     * Reads a required string property and converts it into a {@link Path}.
     *
     * @param p source properties loaded from the application configuration
     * @param key property key to resolve
     *
     * @return configured path value
     * @throws IllegalArgumentException when the property is missing, blank, or not accepted by {@link Path#of(String, String...)}
     * @since 1.0
     */
    private static Path requiredPath(Properties p, String key) {
        return Path.of(requiredString(p, key));
    }

    /**
     * <h3>Read integer property with default</h3>
     * Reads an integer configuration value using a default when the property is
     * absent.
     *
     * @param p source properties loaded from the application configuration
     * @param key property key to resolve
     * @param defaultValue fallback integer used when the property is absent
     *
     * @return parsed integer value
     * @throws IllegalArgumentException when the resolved value is blank or cannot be parsed as an integer
     * @since 1.0
     */
    private static int requiredInt(Properties p, String key,  int defaultValue) {
        String raw = requiredString(p, key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()),
                    "Invalid integer for property {}: Raw {}" ,key ,raw, ex);
            throw new IllegalArgumentException("Invalid integer for property " + key + ": " + raw, ex);
        }
    }

    /**
     * The {@code Snapshot} record is created for carrying one immutable set of
     * resolved pipeline configuration values.
     * <p>
     * Instances are built only after the loader has resolved required properties
     * and applied defaults. The outer class publishes a complete instance through
     * a volatile reference so pipeline threads can read configuration without
     * locks.
     * </p>
     *
     * <h4>Key Features</h4>
     * <ul>
     *     <li>Stores file-system paths used by Excel, image, DOCX, URL, and failure-output processing.</li>
     *     <li>Stores queue capacities, worker counts, retry limits, and file naming values used by the pipeline.</li>
     *     <li>Stores the Patreon access cookie while masking it from log-safe string output.</li>
     * </ul>
     *
     * <h4>Codes</h4>
     * 1 - {@link PipelineConfig#loadSnapshot()}<br>
     * 2 - {@link PipelineConfig#getToString()}<br>
     *
     * <h4>Methods</h4>
     * <ul>
     *     <li>{@link Snapshot#toMaskedString()}</li>
     * </ul>
     *
     * <p>
     * <h4>Notes</h4>
     * <ul>
     *     <li>The compact constructor validates object references but does not validate integer ranges.</li>
     *     <li>The cookie field is intentionally omitted from the unmasked text returned for logging.</li>
     * </ul>
     *
     * @param urlInputPath path to the file containing seed Patreon API URLs
     * @param excelOutputDirPath directory where Excel output files are written
     * @param imageOutputDirPath directory where downloaded images are written
     * @param docxOutputDirPath directory where generated DOCX files are written
     * @param failedOutputDirPath directory where failed job details are written
     * @param excelPostSheetName sheet name used for post rows in Excel files
     * @param excelPostFileName temporary Excel output file name
     * @param docxPostFileNamePattern regular expression used to map Excel file names to DOCX names
     * @param docxPostFileName template DOCX report file name
     * @param excelQueueCapacity bounded capacity of the Excel-ready queue
     * @param docxQueueCapacity bounded capacity of the DOCX-ready queue
     * @param retryQueueCapacity bounded capacity of the retry queue
     * @param failedQueueCapacity bounded capacity of the failed-job queue
     * @param processImageDownloadThreads number of image download process workers
     * @param processDocxThreads number of DOCX process workers
     * @param processExcelThreads number of Excel process workers
     * @param maxRetry maximum retry count for failed image jobs
     * @param patreonAccessCookie Patreon access cookie used for API requests
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
            int processExcelThreads,
            int maxRetry,

            String patreonAccessCookie
    ) {
        /**
         * <h3>Validate snapshot references</h3>
         * Ensures required object values are present before the snapshot is
         * published to pipeline readers.
         *
         * @throws NullPointerException when any required object reference is {@code null}
         * @since 1.0
         */
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

        /**
         * <h3>Create masked snapshot text</h3>
         * Creates a readable configuration summary that is safe for application
         * logs.
         *
         * @return snapshot summary with {@code patreonAccessCookie} replaced by {@code ***}
         * @since 1.0
         */
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
                    ", processExcelThreads=" + processExcelThreads +
                    ", maxRetry=" + maxRetry +
                    ", patreonAccessCookie='***'" +
                    '}';
        }
    }
}
