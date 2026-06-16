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

class ImageDownloadServiceTest {

    @TempDir
    Path tempDir;

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

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

    private HttpServer startServer() throws IOException {
        HttpServer localServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        localServer.start();
        return localServer;
    }

    private void addHandler(String path, int status, String contentType, byte[] body) {
        server.createContext(path, new FixedResponseHandler(status, contentType, body));
    }

    private String urlOf(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    private record FixedResponseHandler(int status, String contentType, byte[] body) implements HttpHandler {
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


