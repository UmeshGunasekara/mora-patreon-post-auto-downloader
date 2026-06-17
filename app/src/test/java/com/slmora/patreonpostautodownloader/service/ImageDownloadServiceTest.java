package com.slmora.patreonpostautodownloader.service;

import com.slmora.patreonpostautodownloader.model.DownloadStatus;
import com.slmora.patreonpostautodownloader.model.ExcelJob;
import com.slmora.patreonpostautodownloader.model.ImageRecord;
import com.slmora.patreonpostautodownloader.support.ExcelJobTestBuilder;
import com.slmora.patreonpostautodownloader.support.ImageRecordTestBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The {@code ImageDownloadServiceTest} test class is created for verifying
 * image download and retry behavior implemented by {@link ImageDownloadService}.
 * <p>
 * It focuses on HTTP response handling, downloaded file persistence, image
 * record status mutation, unique output path generation, and retry filtering
 * for failed image records in the Patreon post download pipeline.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Verifies successful image responses are saved and marked {@link DownloadStatus#SUCCESS}.</li>
 *     <li>Verifies non-image content and HTTP error responses are marked {@link DownloadStatus#FAILED}.</li>
 *     <li>Verifies duplicate generated names produce unique image output paths.</li>
 *     <li>Verifies retry processing only downloads records currently marked failed.</li>
 *     <li>Uses a local {@link HttpServer} fixture so tests do not require external network access.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link ImageDownloadService}<br>
 * 2 - {@link ExcelJob}<br>
 * 3 - {@link ImageRecord}<br>
 * 4 - {@link DownloadStatus}<br>
 * 5 - {@link ExcelJobTestBuilder}<br>
 * 6 - {@link ImageRecordTestBuilder}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link ImageDownloadServiceTest#tearDown()}</li>
 *     <li>{@link ImageDownloadServiceTest#GivenImageResponse_WhenDownloadImages_ThenImageRecordIsMarkedSuccessAndFileIsSaved()}</li>
 *     <li>{@link ImageDownloadServiceTest#GivenNonImageContentType_WhenDownloadImages_ThenImageRecordIsMarkedFailed()}</li>
 *     <li>{@link ImageDownloadServiceTest#GivenHttpErrorStatus_WhenDownloadImages_ThenImageRecordIsMarkedFailed()}</li>
 *     <li>{@link ImageDownloadServiceTest#GivenDuplicateTargetNames_WhenDownloadImages_ThenServiceCreatesUniqueOutputPath()}</li>
 *     <li>{@link ImageDownloadServiceTest#GivenFailedAndSuccessfulRecords_WhenRetryFailedImages_ThenOnlyFailedOnesAreRetried()}</li>
 *     <li>{@link ImageDownloadServiceTest#startServer()}</li>
 *     <li>{@link ImageDownloadServiceTest#addHandler(String, int, String, byte[])}</li>
 *     <li>{@link ImageDownloadServiceTest#urlOf(String)}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>Downloaded files are written under JUnit's {@link TempDir} directory.</li>
 *     <li>Each test shuts down its {@link ImageDownloadService} executor after use.</li>
 *     <li>The local HTTP server is stopped after each test to release the assigned port.</li>
 * </ul>
 *
 * @author: SLMORA
 * @since 1.0
 *
 * <h4>Revision History</h4>
 * <blockquote><pre>
 * <br>Version      Date            Editor              Note
 * <br>-------------------------------------------------------
 * <br>1.0          6/6/2026       SLMORA              Initial Code
 * </pre></blockquote>
 */
class ImageDownloadServiceTest {

    /**
     * Temporary directory used as the image output directory for downloaded
     * file assertions.
     */
    @TempDir
    Path tempDir;

    /**
     * Local HTTP server used to provide deterministic image, non-image, and
     * error responses.
     */
    private HttpServer server;

    /**
     * <h3>Stop local HTTP server</h3>
     * Stops the per-test HTTP server when a test created one.
     * <p>
     * This cleanup keeps dynamically assigned local ports from remaining open
     * after each test scenario.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Checks whether a server instance was created.</li>
     *     <li>Stops the server immediately using a zero-second delay.</li>
     * </ul>
     *
     * @since 1.0
     */
    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    /**
     * <h3>Download image response successfully</h3>
     * Verifies that {@link ImageDownloadService#downloadImages(ExcelJob, Path)}
     * marks an image record successful and writes the response body to disk when
     * the server returns a valid image response.
     * <p>
     * This test targets the normal image download path including job-id suffix
     * naming.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Starts a local HTTP server returning {@code image/png} content.</li>
     *     <li>Creates a job with one image record pointing at that local URL.</li>
     *     <li>Downloads images into the temporary directory.</li>
     *     <li>Asserts success status, downloaded path, file existence, and job-id naming.</li>
     * </ul>
     *
     * @throws Exception when local server setup, download execution, or file assertions fail
     * @since 1.0
     *
     * @see ImageDownloadService#downloadImages(ExcelJob, Path)
     */
    @Test
    void GivenImageResponse_WhenDownloadImages_ThenImageRecordIsMarkedSuccessAndFileIsSaved() throws Exception {
        // Arrange
        server = startServer();
        addHandler("/img", 200, "image/png", "PNG-DATA".getBytes(StandardCharsets.UTF_8));

        String imageUrl = urlOf("/img");
        ExcelJob job = ExcelJobTestBuilder.anExcelJob().withJobId(501L).build();
        job.getImageRecords().add(ImageRecordTestBuilder.anImageRecord()
                .withImageUrl(imageUrl)
                .withImageName("cover")
                .build());

        ImageDownloadService service = new ImageDownloadService();

        // Act
        service.downloadImages(job, tempDir);

        // Assert
        ImageRecord record = job.getImageRecords().getFirst();
        assertThat(record.getDownloadStatus()).isEqualTo(DownloadStatus.SUCCESS);
        assertThat(record.getDownloadedImagePath()).isNotNull();
        assertThat(Files.exists(record.getDownloadedImagePath())).isTrue();
        assertThat(record.getDownloadedImagePath().getFileName().toString()).contains("cover_J501");
        service.shutdown();
    }

    /**
     * <h3>Reject non-image content type</h3>
     * Verifies that {@link ImageDownloadService#downloadImages(ExcelJob, Path)}
     * marks an image record failed when the response status is successful but
     * the content type is not an image.
     * <p>
     * This test targets the guard against saving HTML or other non-image
     * responses as downloaded image files.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Starts a local HTTP server returning {@code text/html} content.</li>
     *     <li>Downloads the configured image record.</li>
     *     <li>Asserts failed status and the non-image error message.</li>
     * </ul>
     *
     * @throws Exception when local server setup or download execution fails
     * @since 1.0
     *
     * @see ImageDownloadService#downloadImages(ExcelJob, Path)
     */
    @Test
    void GivenNonImageContentType_WhenDownloadImages_ThenImageRecordIsMarkedFailed() throws Exception {
        // Arrange
        server = startServer();
        addHandler("/html", 200, "text/html", "<html>not image</html>".getBytes(StandardCharsets.UTF_8));

        ExcelJob job = ExcelJobTestBuilder.anExcelJob().build();
        job.getImageRecords().add(ImageRecordTestBuilder.anImageRecord()
                .withImageUrl(urlOf("/html"))
                .withImageName("non-image")
                .build());

        ImageDownloadService service = new ImageDownloadService();

        // Act
        service.downloadImages(job, tempDir);

        // Assert
        ImageRecord record = job.getImageRecords().getFirst();
        assertThat(record.getDownloadStatus()).isEqualTo(DownloadStatus.FAILED);
        assertThat(record.getErrorMessage()).contains("Not an image");
        service.shutdown();
    }

    /**
     * <h3>Handle HTTP error status</h3>
     * Verifies that {@link ImageDownloadService#downloadImages(ExcelJob, Path)}
     * marks an image record failed when the remote server returns a non-2xx
     * status.
     * <p>
     * This test targets HTTP status validation before file writing occurs.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Starts a local HTTP server returning {@code 404}.</li>
     *     <li>Downloads the configured image record.</li>
     *     <li>Asserts failed status and an HTTP status error message.</li>
     * </ul>
     *
     * @throws Exception when local server setup or download execution fails
     * @since 1.0
     *
     * @see ImageDownloadService#downloadImages(ExcelJob, Path)
     */
    @Test
    void GivenHttpErrorStatus_WhenDownloadImages_ThenImageRecordIsMarkedFailed() throws Exception {
        // Arrange
        server = startServer();
        addHandler("/not-found", 404, "text/plain", "missing".getBytes(StandardCharsets.UTF_8));

        ExcelJob job = ExcelJobTestBuilder.anExcelJob().build();
        job.getImageRecords().add(ImageRecordTestBuilder.anImageRecord()
                .withImageUrl(urlOf("/not-found"))
                .withImageName("404-image")
                .build());

        ImageDownloadService service = new ImageDownloadService();

        // Act
        service.downloadImages(job, tempDir);

        // Assert
        ImageRecord record = job.getImageRecords().getFirst();
        assertThat(record.getDownloadStatus()).isEqualTo(DownloadStatus.FAILED);
        assertThat(record.getErrorMessage()).contains("HTTP 404");
        service.shutdown();
    }

    /**
     * <h3>Create unique output path for duplicate names</h3>
     * Verifies that {@link ImageDownloadService#downloadImages(ExcelJob, Path)}
     * saves both images when multiple records resolve to the same generated file
     * name.
     * <p>
     * This test targets the unique-path suffix behavior used to avoid
     * overwriting files in the image output directory.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Starts a local HTTP server returning a JPEG response.</li>
     *     <li>Adds two image records with the same image name and job id.</li>
     *     <li>Downloads both images into the same directory.</li>
     *     <li>Asserts both records succeed and one output name receives a duplicate suffix.</li>
     * </ul>
     *
     * @throws Exception when local server setup, download execution, or file assertions fail
     * @since 1.0
     *
     * @see ImageDownloadService#downloadImages(ExcelJob, Path)
     */
    @Test
    void GivenDuplicateTargetNames_WhenDownloadImages_ThenServiceCreatesUniqueOutputPath() throws Exception {
        // Arrange
        server = startServer();
        addHandler("/photo.jpg", 200, "image/jpeg", "JPEG-DATA".getBytes(StandardCharsets.UTF_8));

        String imageUrl = urlOf("/photo.jpg");
        ExcelJob job = ExcelJobTestBuilder.anExcelJob().withJobId(701L).build();
        job.getImageRecords().add(ImageRecordTestBuilder.anImageRecord().withImageUrl(imageUrl).withImageName("dup-name").build());
        job.getImageRecords().add(ImageRecordTestBuilder.anImageRecord().withRowNumber(2).withImageUrl(imageUrl).withImageName("dup-name").build());

        ImageDownloadService service = new ImageDownloadService();

        // Act
        service.downloadImages(job, tempDir);

        // Assert
        ImageRecord first = job.getImageRecords().get(0);
        ImageRecord second = job.getImageRecords().get(1);
        assertThat(first.getDownloadStatus()).isEqualTo(DownloadStatus.SUCCESS);
        assertThat(second.getDownloadStatus()).isEqualTo(DownloadStatus.SUCCESS);
        assertThat(first.getDownloadedImagePath()).isNotEqualTo(second.getDownloadedImagePath());
        String firstName = first.getDownloadedImagePath().getFileName().toString();
        String secondName = second.getDownloadedImagePath().getFileName().toString();
        assertThat(firstName.contains("_2") || secondName.contains("_2")).isTrue();
        service.shutdown();
    }

    /**
     * <h3>Retry only failed image records</h3>
     * Verifies that {@link ImageDownloadService#retryFailedImages(ExcelJob, Path)}
     * retries records marked {@link DownloadStatus#FAILED} and leaves already
     * successful records untouched.
     * <p>
     * This test targets the retry filter used by retry processing after an
     * image download batch partially fails.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Starts a local HTTP server returning a valid PNG response.</li>
     *     <li>Creates one failed image record and one already successful image record.</li>
     *     <li>Invokes failed-image retry.</li>
     *     <li>Asserts the failed record is downloaded and the successful record keeps its null downloaded path.</li>
     * </ul>
     *
     * @throws Exception when local server setup, retry execution, or file assertions fail
     * @since 1.0
     *
     * @see ImageDownloadService#retryFailedImages(ExcelJob, Path)
     */
    @Test
    void GivenFailedAndSuccessfulRecords_WhenRetryFailedImages_ThenOnlyFailedOnesAreRetried() throws Exception {
        // Arrange
        server = startServer();
        addHandler("/retry.png", 200, "image/png", "RETRY-DATA".getBytes(StandardCharsets.UTF_8));

        ExcelJob job = ExcelJobTestBuilder.anExcelJob().withJobId(801L).build();
        ImageRecord failedRecord = ImageRecordTestBuilder.anImageRecord()
                .withImageUrl(urlOf("/retry.png"))
                .withImageName("retry-me")
                .withDownloadStatus(DownloadStatus.FAILED)
                .build();
        ImageRecord alreadySuccess = ImageRecordTestBuilder.anImageRecord()
                .withRowNumber(2)
                .withImageUrl(urlOf("/retry.png"))
                .withImageName("already-success")
                .withDownloadStatus(DownloadStatus.SUCCESS)
                .build();

        job.getImageRecords().add(failedRecord);
        job.getImageRecords().add(alreadySuccess);

        ImageDownloadService service = new ImageDownloadService();

        // Act
        service.retryFailedImages(job, tempDir);

        // Assert
        assertThat(failedRecord.getDownloadStatus()).isEqualTo(DownloadStatus.SUCCESS);
        assertThat(failedRecord.getDownloadedImagePath()).isNotNull();
        assertThat(alreadySuccess.getDownloadStatus()).isEqualTo(DownloadStatus.SUCCESS);
        assertThat(alreadySuccess.getDownloadedImagePath()).isNull();
        service.shutdown();
    }

    /**
     * <h3>Start local HTTP server</h3>
     * Creates and starts a local HTTP server on an available loopback port.
     *
     * @return started HTTP server bound to {@code 127.0.0.1}
     * @throws IOException when the local server cannot be created
     * @since 1.0
     */
    private HttpServer startServer() throws IOException {
        HttpServer localServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        localServer.start();
        return localServer;
    }

    /**
     * <h3>Add fixed HTTP response handler</h3>
     * Registers a deterministic response for the supplied local server path.
     *
     * @param path request path to handle
     * @param status HTTP status code to return
     * @param contentType response content type header
     * @param body response body bytes
     *
     * @since 1.0
     */
    private void addHandler(String path, int status, String contentType, byte[] body) {
        server.createContext(path, new FixedResponseHandler(status, contentType, body));
    }

    /**
     * <h3>Build local server URL</h3>
     * Builds an HTTP URL for the current local server and supplied path.
     *
     * @param path request path hosted by the local server
     *
     * @return absolute local HTTP URL
     * @since 1.0
     */
    private String urlOf(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    /**
     * The {@code FixedResponseHandler} record is created for returning a fixed
     * HTTP response from the local test server.
     * <p>
     * Each test registers one or more handlers to simulate successful images,
     * non-image responses, and HTTP error statuses without external network
     * access.
     * </p>
     *
     * <h4>Key Features</h4>
     * <ul>
     *     <li>Sets the configured content type on every response.</li>
     *     <li>Sends the configured status code and body bytes.</li>
     *     <li>Closes the response stream after writing the body.</li>
     * </ul>
     *
     * <h4>Codes</h4>
     * 1 - {@link HttpHandler}<br>
     * 2 - {@link HttpExchange}<br>
     *
     * <h4>Methods</h4>
     * <ul>
     *     <li>{@link FixedResponseHandler#handle(HttpExchange)}</li>
     * </ul>
     *
     * <p>
     * <h4>Notes</h4>
     * <ul>
     *     <li>This record is a test fixture and is not used by production image downloading.</li>
     * </ul>
     *
     * @param status HTTP status code returned by the fixture
     * @param contentType content type header returned by the fixture
     * @param body response body bytes returned by the fixture
     *
     * @author: SLMORA
     * @since 1.0
     *
     * <h4>Revision History</h4>
     * <blockquote><pre>
     * <br>Version      Date            Editor              Note
     * <br>-------------------------------------------------------
     * <br>1.0          6/6/2026       SLMORA              Initial Code
     * </pre></blockquote>
     */
    private record FixedResponseHandler(int status, String contentType, byte[] body) implements HttpHandler {
        /**
         * <h3>Write fixed HTTP response</h3>
         * Writes the configured status, content type, and body to the exchange.
         *
         * @param exchange HTTP exchange supplied by the local test server
         *
         * @throws IOException when response headers or body cannot be written
         * @since 1.0
         */
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        }
    }
}


