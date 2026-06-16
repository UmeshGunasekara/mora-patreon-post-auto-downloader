/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 2:33 PM
 */
package com.slmora.patreonpostautodownloader.service;

import com.slmora.common.logging.MoraLogger;
import com.slmora.common.logging.MoraLoggerThreadInfo;
import com.slmora.patreonpostautodownloader.model.DownloadStatus;
import com.slmora.patreonpostautodownloader.model.ExcelJob;
import com.slmora.patreonpostautodownloader.model.ImageRecord;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

/**
 * The {@code ImageDownloadService} class is created for downloading Patreon
 * image URLs associated with an {@link ExcelJob}.
 * <p>
 * The image download worker and retry process use this service to fetch remote
 * image content, write files to the configured image output directory, and
 * update each {@link ImageRecord} with success or failure state.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Downloads all image records for a job using a virtual-thread executor.</li>
 *     <li>Retries only records currently marked as {@link DownloadStatus#FAILED}.</li>
 *     <li>Limits active HTTP downloads with a semaphore to avoid excessive parallel requests.</li>
 *     <li>Validates HTTP status and image content type before saving files.</li>
 *     <li>Creates unique output paths when generated image names collide.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link ExcelJob}<br>
 * 2 - {@link ImageRecord}<br>
 * 3 - {@link DownloadStatus}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link ImageDownloadService#downloadImages(ExcelJob, Path)}</li>
 *     <li>{@link ImageDownloadService#retryFailedImages(ExcelJob, Path)}</li>
 *     <li>{@link ImageDownloadService#shutdown()}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>The service mutates {@link ImageRecord} instances by setting downloaded path, status, and error message.</li>
 *     <li>A response with non-image content type is treated as a failed image download.</li>
 *     <li>Call {@link ImageDownloadService#shutdown()} when the service instance is no longer needed.</li>
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
public class ImageDownloadService
{
    /**
     * Class-scoped logger used for image download success, failure, and summary
     * diagnostics.
     */
    private final static MoraLogger LOGGER = MoraLogger.getLogger(ImageDownloadService.class);

    /**
     * HTTP connection timeout used by the shared image {@link HttpClient}.
     */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(20);

    /**
     * Per-request timeout applied to image download requests.
     */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    /**
     * Maximum number of concurrent in-flight image downloads for one service
     * instance.
     */
    private static final int MAX_CONCURRENCY = 16;

    /**
     * Virtual-thread executor used for lightweight concurrent image download
     * tasks.
     */
    private final ExecutorService virtualThreadPool =
            Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Shared HTTP client configured for image requests and redirect following.
     */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
//            .followRedirects(HttpClient.Redirect.NORMAL)
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    /**
     * <h3>Download all job images</h3>
     * Downloads every image record currently attached to the supplied job.
     * <p>
     * Each image record is submitted to the virtual-thread pool. The method
     * waits for all submitted downloads to complete, logs per-image outcomes,
     * and leaves final success or failure details on the mutable
     * {@link ImageRecord} instances.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates one download task per image record in the job.</li>
     *     <li>Uses a {@link Semaphore} to limit active downloads to {@link ImageDownloadService#MAX_CONCURRENCY}.</li>
     *     <li>Calls {@link ImageDownloadService#downloadSingleImage(ExcelJob, ImageRecord, Path)} for each record.</li>
     *     <li>Waits for all futures and logs success and failure totals.</li>
     * </ul>
     *
     * @param job Excel job containing image records to download
     * @param imageOutputDir directory where image files should be written
     *
     * @throws ExecutionException when a submitted download task fails outside the handled download path
     * @throws InterruptedException when waiting for download task completion is interrupted
     *
     * @apiNote This method mutates image records in the supplied job.
     * @since 1.0
     */
    public void downloadImages(ExcelJob job, Path imageOutputDir) throws ExecutionException, InterruptedException
    {
        List<Future<Result>> futures = new ArrayList<>();

        // Virtual threads are lightweight, but the semaphore keeps remote image requests politely bounded.
        Semaphore semaphore = new Semaphore(MAX_CONCURRENCY);

        for (ImageRecord record : job.getImageRecords()) {
            futures.add(virtualThreadPool.submit(() -> {
                semaphore.acquire();
                try {
                    Optional<Path> saved = downloadSingleImage(job, record, imageOutputDir);
                    if(saved.isPresent()) {
                        return new Result(record, true, saved.toString(), null);
                    }else {
                        return new Result(record, false, null, record.getErrorMessage());
                    }

                } catch (Exception e) {
                    LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                    Thread.currentThread().threadId(),
                                    Thread.currentThread().getStackTrace()), e);
                    return new Result(record, false, null, e.getMessage());
                }finally {
                    // Always release the permit so one failed download cannot starve the remaining batch.
                    semaphore.release();
                }
            }));
        }

        int ok = 0, fail = 0;
        for (Future<Result> future : futures) {
            Result result = future.get();
            if (result.success()) {
                ok++;
                LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()),
                        "[OK]   " + result.imageRecord().getImageName() + " -> " + result.path());
            } else {
                fail++;
                LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()),
                        "[FAIL] " + result.imageRecord().getImageName() + " (" + result.imageRecord().getImageUrl() + ") : " + result.error());
            }
        }
        LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                        Thread.currentThread().threadId(),
                        Thread.currentThread().getStackTrace()),
                "Done. Success=" + ok + " Fail=" + fail);
    }

    /**
     * <h3>Retry failed job images</h3>
     * Retries only image records currently marked as {@link DownloadStatus#FAILED}.
     * <p>
     * Successful records are left untouched. Failed records are resubmitted to
     * the same single-image download path used by the initial download stage.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Filters the job image list to failed records only.</li>
     *     <li>Downloads failed records concurrently with the same semaphore limit as normal downloads.</li>
     *     <li>Waits for retry futures and logs retry success and failure totals.</li>
     * </ul>
     *
     * @param job Excel job containing image records to retry
     * @param imageOutputDir directory where retry output files should be written
     *
     * @throws ExecutionException when a submitted retry task fails outside the handled download path
     * @throws InterruptedException when waiting for retry task completion is interrupted
     *
     * @apiNote Already successful image records are not downloaded again by this method.
     * @since 1.0
     */
    public void retryFailedImages(ExcelJob job, Path imageOutputDir) throws ExecutionException, InterruptedException
    {

        List<Future<Result>> futures = new ArrayList<>();

        // Reuse the same concurrency cap for retries so retry storms do not overload remote hosts.
        Semaphore semaphore = new Semaphore(MAX_CONCURRENCY);

        for (ImageRecord record : job.getImageRecords()) {
            if (record.getDownloadStatus() == DownloadStatus.FAILED) {
                futures.add(virtualThreadPool.submit(() ->{
                    semaphore.acquire();
                    try {
                        Optional<Path> saved = downloadSingleImage(job, record, imageOutputDir);
                        if(saved.isPresent()) {
                            return new Result(record, true, saved.toString(), null);
                        }else {
                            return new Result(record, false, null, record.getErrorMessage());
                        }

                    } catch (Exception e) {
                        LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                        Thread.currentThread().threadId(),
                                        Thread.currentThread().getStackTrace()), e);
                        return new Result(record, false, null, e.getMessage());
                    }finally {
                        // Always release the permit so later retry records can continue.
                        semaphore.release();
                    }
                }));
            }
        }

        int ok = 0, fail = 0;
        for (Future<Result> future : futures) {
            Result result = future.get();
            if (result.success()) {
                ok++;
                LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()),
                        "[OK]   " + result.imageRecord().getImageName() + " -> " + result.path());
            } else {
                fail++;
                LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()),
                        "[FAIL] " + result.imageRecord().getImageName() + " (" + result.imageRecord().getImageUrl() + ") : " + result.error());
            }
        }
        LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                        Thread.currentThread().threadId(),
                        Thread.currentThread().getStackTrace()),
                "Done. Success=" + ok + " Fail=" + fail);
    }

    /**
     * <h3>Download single image</h3>
     * Downloads one image URL, writes it to disk, and updates the corresponding
     * image record.
     * <p>
     * The output file name includes the job id, a sanitized image name, an
     * extension inferred from URL or content type, and a uniqueness suffix when
     * a file with the same name already exists.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Builds an HTTP GET request using browser-like image headers.</li>
     *     <li>Rejects non-2xx HTTP responses and non-image content types.</li>
     *     <li>Copies the response body to a unique output path.</li>
     *     <li>Sets {@link DownloadStatus#SUCCESS}, output path, and clears error message on success.</li>
     *     <li>Sets {@link DownloadStatus#FAILED} and records the error message on failure.</li>
     * </ul>
     *
     * @param job job that owns the image record
     * @param record image record to download and mutate
     * @param imageOutputDir directory where the image should be written
     *
     * @return saved image path when the download succeeds, or {@link Optional#empty()} when it fails
     * @since 1.0
     */
    private Optional<Path> downloadSingleImage(
            ExcelJob job,
            ImageRecord record,
            Path imageOutputDir
    ) {
        try {
            String url = record.getImageUrl();
            String imageName = record.getImageName();
            // Include the job id so files from different Excel batches do not collide.
            imageName = imageName+"_J"+job.getJobId();

            URI uri = URI.create(url.trim());

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(REQUEST_TIMEOUT)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                    .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .GET()
                    .build();

//            HttpResponse<byte[]> response = httpClient.send(
//                    request,
//                    HttpResponse.BodyHandlers.ofByteArray()
//            );

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

//            if (response.statusCode() < 200 || response.statusCode() >= 300) {
//                throw new IOException("HTTP status: " + response.statusCode());
//            }

//            if (response.statusCode() != 200) {
//                throw new RuntimeException("HTTP " + response.statusCode());
//            }

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("HTTP " + response.statusCode());
            }

            String contentType = response.headers().firstValue("Content-Type").orElse("");
            if (!contentType.startsWith("image")) {
                // Some servers return HTML error pages with 200 status; avoid saving those as image files.
                throw new RuntimeException("Not an image. Content-Type=" + (contentType.isBlank() ? "unknown" : contentType));
            }

            String ext = guessExtension(url, contentType);
            String safeBase = sanitizeFileName(imageName);
            Path lastImageFile = uniquePath(imageOutputDir.resolve(safeBase + ext));

            try (InputStream body = response.body()) {
                Files.copy(body, lastImageFile, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()),
                        "Image has  been downloaded in {}", lastImageFile.toAbsolutePath());
            }

//            Files.write(imagePath, response.body());

            record.setDownloadedImagePath(lastImageFile);
            record.setDownloadStatus(DownloadStatus.SUCCESS);
            record.setErrorMessage(null);

            return Optional.of(lastImageFile);

        } catch (Exception e) {
            record.setDownloadStatus(DownloadStatus.FAILED);
            record.setErrorMessage(e.getMessage());
            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()), e);
        }
        return Optional.empty();
    }

    /**
     * <h3>Guess image extension</h3>
     * Resolves an output file extension from the source URL path or response
     * content type.
     *
     * @param url source image URL
     * @param contentType HTTP content type returned by the server
     *
     * @return resolved extension including the leading dot
     * @since 1.0
     */
    private String guessExtension(String url, String contentType) {
        // Prefer the URL extension because it often preserves the creator's original image format.
        String clean = url.split("\\?")[0];
        int dot = clean.lastIndexOf('.');
        if (dot > clean.lastIndexOf('/') && dot != -1) {
            String ext = clean.substring(dot);
            if (ext.length() <= 6) return ext; // .png, .jpeg, .webp etc.
        }

        // Fall back to content type when URLs are CDN paths without useful extensions.
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".img";
        };
    }

    /**
     * <h3>Sanitize image file name</h3>
     * Removes path-sensitive characters and normalizes whitespace in a generated
     * image name.
     *
     * @param name proposed image file base name
     *
     * @return file-system-safe base name, or {@code image} when the sanitized result is blank
     * @since 1.0
     */
    private String sanitizeFileName(String name) {
        // Replace Windows-reserved and cross-platform path separator characters before writing files.
        String s = name.trim();
        s = s.replaceAll("[\\\\/:*?\"<>|]", "_");
        s = s.replaceAll("\\s+", " ");
        if (s.isBlank()) s = "image";
        return s;
    }

    /**
     * <h3>Resolve unique output path</h3>
     * Returns the requested path when available, otherwise adds a numeric suffix
     * until an unused file name is found.
     *
     * @param base desired output path
     *
     * @return unique path that does not currently exist
     * @throws Exception when too many duplicate file names already exist
     * @since 1.0
     */
    private Path uniquePath(Path base) throws Exception {
        if (!Files.exists(base)) return base;

        String fileName = base.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String stem = (dot > 0) ? fileName.substring(0, dot) : fileName;
        String ext = (dot > 0) ? fileName.substring(dot) : "";

        Path dir = base.getParent();
        for (int i = 2; i < 10_000; i++) {
            Path p = dir.resolve(stem + "_" + i + ext);
            if (!Files.exists(p)) return p;
        }
        throw new RuntimeException("Too many duplicate filenames for: " + base);
    }

    /**
     * <h3>Shutdown image download executor</h3>
     * Stops accepting new virtual-thread download tasks.
     *
     * @apiNote Call this when the service instance is no longer needed, such as
     * at the end of tests or pipeline shutdown.
     * @since 1.0
     */
    public void shutdown() {
        virtualThreadPool.shutdown();
    }

    /**
     * The {@code Result} record is created for carrying one asynchronous image
     * download outcome back to the batch aggregator.
     * <p>
     * It keeps the image record, success flag, saved path text, and failure
     * message together while the caller collects completed futures.
     * </p>
     *
     * <h4>Key Features</h4>
     * <ul>
     *     <li>Associates a completed download attempt with its {@link ImageRecord}.</li>
     *     <li>Stores either the saved path or an error message for logging.</li>
     * </ul>
     *
     * <h4>Codes</h4>
     * 1 - {@link ImageRecord}<br>
     *
     * <h4>Methods</h4>
     * <ul>
     *     <li>Record component accessors generated by Java</li>
     * </ul>
     *
     * <p>
     * <h4>Notes</h4>
     * <ul>
     *     <li>This record is internal to image download aggregation.</li>
     * </ul>
     *
     * @param imageRecord image record associated with the attempt
     * @param success whether the attempt saved an image successfully
     * @param path saved image path text when successful
     * @param error error message when unsuccessful
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
    private record Result(
            ImageRecord imageRecord, boolean success, String path, String error) {}
}
