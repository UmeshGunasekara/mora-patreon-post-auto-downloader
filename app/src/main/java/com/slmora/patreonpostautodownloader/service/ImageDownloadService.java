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
 * The {@code ImageDownloadService} Class created for
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
public class ImageDownloadService
{
    private final static MoraLogger LOGGER = MoraLogger.getLogger(ImageDownloadService.class);

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);
    private static final int MAX_CONCURRENCY = 16; // virtual threads are cheap; still avoid hammering servers

    private final ExecutorService virtualThreadPool =
            Executors.newVirtualThreadPerTaskExecutor();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
//            .followRedirects(HttpClient.Redirect.NORMAL)
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    public void downloadImages(ExcelJob job, Path imageOutputDir) throws ExecutionException, InterruptedException
    {
        List<Future<Result>> futures = new ArrayList<>();

        // Concurrency control (avoid too many parallel downloads)
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
                    LOGGER.error(threadInfo(), e);
                    return new Result(record, false, null, e.getMessage());
                }finally {
                    semaphore.release();
                }
            }));
        }

        int ok = 0, fail = 0;
        for (Future<Result> future : futures) {
            Result result = future.get();
            if (result.success()) {
                ok++;
                LOGGER.info(threadInfo(),"[OK]   " + result.imageRecord().getImageName() + " -> " + result.path());
            } else {
                fail++;
                LOGGER.error(threadInfo(),"[FAIL] " + result.imageRecord().getImageName() + " (" + result.imageRecord().getImageUrl() + ") : " + result.error());
            }
        }
        LOGGER.info(threadInfo(),"Done. Success=" + ok + " Fail=" + fail);
    }

    public void retryFailedImages(ExcelJob job, Path imageOutputDir) throws ExecutionException, InterruptedException
    {

        List<Future<Result>> futures = new ArrayList<>();

        // Concurrency control (avoid too many parallel downloads)
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
                        LOGGER.error(threadInfo(), e);
                        return new Result(record, false, null, e.getMessage());
                    }finally {
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
                LOGGER.info(threadInfo(),"[OK]   " + result.imageRecord().getImageName() + " -> " + result.path());
            } else {
                fail++;
                LOGGER.error(threadInfo(),"[FAIL] " + result.imageRecord().getImageName() + " (" + result.imageRecord().getImageUrl() + ") : " + result.error());
            }
        }
        LOGGER.info(threadInfo(),"Done. Success=" + ok + " Fail=" + fail);
    }

    private Optional<Path> downloadSingleImage(
            ExcelJob job,
            ImageRecord record,
            Path imageOutputDir
    ) {
        try {
            String url = record.getImageUrl();
            String imageName = record.getImageName();
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
                // Some servers may return html error pages; this blocks saving junk.
                throw new RuntimeException("Not an image. Content-Type=" + (contentType.isBlank() ? "unknown" : contentType));
            }

            String ext = guessExtension(url, contentType);
            String safeBase = sanitizeFileName(imageName);
            Path lastImageFile = uniquePath(imageOutputDir.resolve(safeBase + ext));

            try (InputStream body = response.body()) {
                Files.copy(body, lastImageFile, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info(threadInfo(),"Image has  been downloaded in {}", lastImageFile.toAbsolutePath());
            }

//            Files.write(imagePath, response.body());

            record.setDownloadedImagePath(lastImageFile);
            record.setDownloadStatus(DownloadStatus.SUCCESS);
            record.setErrorMessage(null);

            return Optional.of(lastImageFile);

        } catch (Exception e) {
            record.setDownloadStatus(DownloadStatus.FAILED);
            record.setErrorMessage(e.getMessage());
            LOGGER.error(threadInfo(),e);
        }
        return Optional.empty();
    }

    private String guessExtension(String url, String contentType) {
        // 1) try extension from URL path
        String clean = url.split("\\?")[0];
        int dot = clean.lastIndexOf('.');
        if (dot > clean.lastIndexOf('/') && dot != -1) {
            String ext = clean.substring(dot);
            if (ext.length() <= 6) return ext; // .png, .jpeg, .webp etc.
        }

        // 2) fallback from content-type
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".img";
        };
    }

    private String sanitizeFileName(String name) {
        // Windows-safe + cross-platform
        String s = name.trim();
        s = s.replaceAll("[\\\\/:*?\"<>|]", "_");
        s = s.replaceAll("\\s+", " ");
        if (s.isBlank()) s = "image";
        return s;
    }

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

    public void shutdown() {
        virtualThreadPool.shutdown();
    }

    private record Result(
            ImageRecord imageRecord, boolean success, String path, String error) {}

    private static MoraLoggerThreadInfo threadInfo() {
        Thread t = Thread.currentThread();
        return new MoraLoggerThreadInfo(t.getName(), t.threadId(), t.getStackTrace());
    }
}
