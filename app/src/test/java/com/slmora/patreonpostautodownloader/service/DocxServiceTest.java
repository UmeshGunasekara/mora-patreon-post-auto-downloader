package com.slmora.patreonpostautodownloader.service;

import com.slmora.patreonpostautodownloader.model.ExcelJob;
import com.slmora.patreonpostautodownloader.support.ExcelJobTestBuilder;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocxServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void GivenValidExcelAndMatchingPattern_WhenCreateDocx_ThenDocxFileIsCreatedAndAssignedToJob() throws Exception {
        // Arrange
        Path excelFile = tempDir.resolve("patreon_posts_output_20260101_20260130_J1.xlsx");
        Path docxOutputDir = tempDir.resolve("docx");
        Files.createDirectories(docxOutputDir);
        createExcelFile(excelFile, true, "", "");

        ExcelJob job = ExcelJobTestBuilder.anExcelJob().withExcelFile(excelFile).build();
        DocxService docxService = new DocxService();

        // Act
        docxService.createDocx(
                job,
                docxOutputDir,
                "patreon_posts_output(.*)\\.xlsx",
                "patreon_posts_report_temp.docx",
                "Posts"
        );

        // Assert
        assertThat(job.getDocxFile()).isNotNull();
        assertThat(job.getDocxFile().getFileName().toString())
                .startsWith("patreon_posts_report")
                .contains("20260101_20260130_J1")
                .endsWith(".docx");
        assertThat(Files.exists(job.getDocxFile())).isTrue();
    }

    @Test
    void GivenNonMatchingPattern_WhenCreateDocx_ThenFallbackTmpDocxNameIsUsed() throws Exception {
        // Arrange
        Path excelFile = tempDir.resolve("different-name.xlsx");
        Path docxOutputDir = tempDir.resolve("docx-fallback");
        Files.createDirectories(docxOutputDir);
        createExcelFile(excelFile, true, "", "");

        ExcelJob job = ExcelJobTestBuilder.anExcelJob().withExcelFile(excelFile).build();
        DocxService docxService = new DocxService();

        // Act
        docxService.createDocx(
                job,
                docxOutputDir,
                "patreon_posts_output(.*)\\.xlsx",
                "patreon_posts_report_temp.docx",
                "Posts"
        );

        // Assert
        assertThat(job.getDocxFile().getFileName().toString()).isEqualTo("_tmp");
        assertThat(Files.exists(job.getDocxFile())).isTrue();
    }

    @Test
    void GivenExcelWithoutHeaderRow_WhenCreateDocx_ThenIllegalStateExceptionIsThrown() throws Exception {
        // Arrange
        Path excelFile = tempDir.resolve("no-header.xlsx");
        Path docxOutputDir = tempDir.resolve("docx-no-header");
        Files.createDirectories(docxOutputDir);
        createExcelFile(excelFile, false, "", "");

        ExcelJob job = ExcelJobTestBuilder.anExcelJob().withExcelFile(excelFile).build();
        DocxService docxService = new DocxService();

        // Act + Assert
        assertThatThrownBy(() -> docxService.createDocx(
                job,
                docxOutputDir,
                "patreon_posts_output(.*)\\.xlsx",
                "patreon_posts_report_temp.docx",
                "Posts"
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Excel header row is missing");
    }

    @Test
    void GivenImageUrlDownloadFails_WhenCreateDocx_ThenDocxStillGeneratedWithFallbackImageText() throws Exception {
        // Arrange
        Path excelFile = tempDir.resolve("patreon_posts_output_20260201_20260228_J2.xlsx");
        Path docxOutputDir = tempDir.resolve("docx-image-error");
        Files.createDirectories(docxOutputDir);
        createExcelFile(excelFile, true, "", "http://127.0.0.1:1/does-not-exist.png");

        ExcelJob job = ExcelJobTestBuilder.anExcelJob().withExcelFile(excelFile).build();
        DocxService docxService = new DocxService();

        // Act
        docxService.createDocx(
                job,
                docxOutputDir,
                "patreon_posts_output(.*)\\.xlsx",
                "patreon_posts_report_temp.docx",
                "Posts"
        );

        // Assert
        assertThat(job.getDocxFile()).isNotNull();
        assertThat(Files.exists(job.getDocxFile())).isTrue();
    }

    @Test
    void GivenMissingOptionalColumnsAndMixedContentJson_WhenCreateDocx_ThenDocxContainsExtractedAndFallbackText() throws Exception {
        // Arrange
        Path excelFile = tempDir.resolve("patreon_posts_output_20260301_20260331_J3.xlsx");
        Path docxOutputDir = tempDir.resolve("docx-text-branches");
        Files.createDirectories(docxOutputDir);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Posts");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("id");
            header.createCell(1).setCellValue("published_at");
            header.createCell(2).setCellValue("title");
            header.createCell(4).setCellValue("content_json_string");
            // intentionally omit patreon_url, large_url, thumb_url headers

            var row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("p-11");
            row1.createCell(1).setCellValue("bad-date-no-T");
            row1.createCell(2).setCellValue("Title 1");
            row1.createCell(4).setCellValue("not-json");

            var row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("p-12");
            row2.createCell(1).setCellValue("2026-06-16T10:15");
            row2.createCell(2).setCellValue("Title 2");
            row2.createCell(4).setCellValue("{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"first\"}]},{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"second\"}]}]}");

            try (FileOutputStream out = new FileOutputStream(excelFile.toFile())) {
                workbook.write(out);
            }
        }

        ExcelJob job = ExcelJobTestBuilder.anExcelJob().withExcelFile(excelFile).build();
        DocxService docxService = new DocxService();

        // Act
        docxService.createDocx(
                job,
                docxOutputDir,
                "patreon_posts_output(.*)\\.xlsx",
                "patreon_posts_report_temp.docx",
                "Posts"
        );

        // Assert
        assertThat(job.getDocxFile()).isNotNull();
        assertThat(Files.exists(job.getDocxFile())).isTrue();
        String xml = readWordDocumentXml(job.getDocxFile());
        assertThat(xml).contains("not-json");
        assertThat(xml).contains("first");
        assertThat(xml).contains("second");
    }

    private void createExcelFile(Path excelFile, boolean withHeader, String thumbUrl, String largeUrl) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Posts");

            if (withHeader) {
                var header = sheet.createRow(0);
                header.createCell(0).setCellValue("id");
                header.createCell(1).setCellValue("published_at");
                header.createCell(2).setCellValue("title");
                header.createCell(4).setCellValue("content_json_string");
                header.createCell(6).setCellValue("patreon_url");
                header.createCell(7).setCellValue("large_url");
                header.createCell(8).setCellValue("thumb_url");

                var row = sheet.createRow(1);
                row.createCell(0).setCellValue("p-1");
                row.createCell(1).setCellValue("2026-06-16T10:15:30+00:00");
                row.createCell(2).setCellValue("Docx Title");
                row.createCell(4).setCellValue("{\"type\":\"paragraph\",\"text\":\"hello\"}");
                row.createCell(6).setCellValue("https://www.patreon.com/posts/1");
                row.createCell(7).setCellValue(largeUrl);
                row.createCell(8).setCellValue(thumbUrl);
            }

            try (FileOutputStream out = new FileOutputStream(excelFile.toFile())) {
                workbook.write(out);
            }
        }
    }

    private String readWordDocumentXml(Path docxPath) throws IOException {
        try (ZipFile zip = new ZipFile(docxPath.toFile())) {
            var entry = zip.getEntry("word/document.xml");
            try (var in = zip.getInputStream(entry)) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }
}



