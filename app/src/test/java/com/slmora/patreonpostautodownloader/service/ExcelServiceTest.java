package com.slmora.patreonpostautodownloader.service;

import com.slmora.patreonpostautodownloader.model.ImageRecord;
import com.slmora.patreonpostautodownloader.model.PostRecord;
import com.slmora.patreonpostautodownloader.support.PostRecordTestBuilder;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExcelServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void GivenNewExcelFile_WhenCreateExcelFromRecords_ThenFileIsCreatedWithPostRows() throws Exception {
        // Arrange
        ExcelService excelService = new ExcelService();
        Path excelFile = tempDir.resolve("posts.xlsx");
        List<PostRecord> posts = List.of(
                PostRecordTestBuilder.aPostRecord().withId("11").build(),
                PostRecordTestBuilder.aPostRecord().withId("12").build()
        );

        // Act
        excelService.createExcelFromRecords(posts, excelFile, "Posts");

        // Assert
        List<ImageRecord> images = excelService.readImageRecords(excelFile, "Posts");
        assertThat(excelFile).exists();
        assertThat(images).hasSize(2);
    }

    @Test
    void GivenExistingExcelFile_WhenCreateExcelFromRecordsAgain_ThenRowsAreAppended() throws Exception {
        // Arrange
        ExcelService excelService = new ExcelService();
        Path excelFile = tempDir.resolve("append.xlsx");

        // Act
        excelService.createExcelFromRecords(
                List.of(PostRecordTestBuilder.aPostRecord().withId("21").build()),
                excelFile,
                "Posts"
        );
        excelService.createExcelFromRecords(
                List.of(PostRecordTestBuilder.aPostRecord().withId("22").build()),
                excelFile,
                "Posts"
        );

        // Assert
        List<ImageRecord> images = excelService.readImageRecords(excelFile, "Posts");
        assertThat(images).hasSize(2);
    }

    @Test
    void GivenPipeSeparatedImageUrlsAndInvalidPublishedAt_WhenReadImageRecords_ThenMultipleRecordsAreCreated() throws Exception {
        // Arrange
        ExcelService excelService = new ExcelService();
        Path excelFile = tempDir.resolve("images.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Posts");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("id");
            header.createCell(1).setCellValue("published_at");
            header.createCell(2).setCellValue("title");
            header.createCell(7).setCellValue("large_url");

            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("p-101");
            row.createCell(1).setCellValue("2026-06-16T11:22:33+00:00");
            row.createCell(2).setCellValue("Hello World!");
            row.createCell(7).setCellValue("https://a.jpg| https://b.jpg");

            try (FileOutputStream out = new FileOutputStream(excelFile.toFile())) {
                workbook.write(out);
            }
        }

        // Act
        List<ImageRecord> images = excelService.readImageRecords(excelFile, "Posts");

        // Assert
        assertThat(images).hasSize(2);
        assertThat(images.getFirst().getImageUrl()).isEqualTo("https://a.jpg");
        assertThat(images.getLast().getImageUrl()).isEqualTo("https://b.jpg");
        assertThat(images.getFirst().getImageName()).contains("D2026-06-16-T11-22-33-p-101-HELLO-WORLD-01");
    }

    @Test
    void GivenMissingHeaderRow_WhenReadImageRecords_ThenIllegalStateExceptionIsThrown() throws Exception {
        // Arrange
        ExcelService excelService = new ExcelService();
        Path excelFile = tempDir.resolve("missing-header.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            workbook.createSheet("Posts");
            try (FileOutputStream out = new FileOutputStream(excelFile.toFile())) {
                workbook.write(out);
            }
        }

        // Act + Assert
        assertThatThrownBy(() -> excelService.readImageRecords(excelFile, "Posts"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Excel header row is missing");
    }

    @Test
    void GivenMissingExcelFile_WhenReadImageRecords_ThenRuntimeExceptionIsThrown() {
        // Arrange
        ExcelService excelService = new ExcelService();
        Path missingFile = tempDir.resolve("not-found.xlsx");

        // Act + Assert
        assertThatThrownBy(() -> excelService.readImageRecords(missingFile, "Posts"))
                .isInstanceOf(RuntimeException.class);
    }
}

