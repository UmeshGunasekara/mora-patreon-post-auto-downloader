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

/**
 * The {@code ExcelServiceTest} test class is created for verifying Excel
 * workbook read and write behavior implemented by {@link ExcelService}.
 * <p>
 * It focuses on creating new Patreon post workbooks, appending post rows to an
 * existing workbook, extracting image download records from the {@code large_url}
 * column, and validating error handling for malformed or missing workbooks.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Verifies new workbook creation from {@link PostRecord} fixtures.</li>
 *     <li>Verifies repeated writes append rows to an existing workbook.</li>
 *     <li>Verifies pipe-separated image URLs create multiple {@link ImageRecord} values.</li>
 *     <li>Verifies missing header rows and missing files report failures.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link ExcelService}<br>
 * 2 - {@link PostRecord}<br>
 * 3 - {@link ImageRecord}<br>
 * 4 - {@link PostRecordTestBuilder}<br>
 * 5 - {@link XSSFWorkbook}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link ExcelServiceTest#GivenNewExcelFile_WhenCreateExcelFromRecords_ThenFileIsCreatedWithPostRows()}</li>
 *     <li>{@link ExcelServiceTest#GivenExistingExcelFile_WhenCreateExcelFromRecordsAgain_ThenRowsAreAppended()}</li>
 *     <li>{@link ExcelServiceTest#GivenPipeSeparatedImageUrlsAndInvalidPublishedAt_WhenReadImageRecords_ThenMultipleRecordsAreCreated()}</li>
 *     <li>{@link ExcelServiceTest#GivenMissingHeaderRow_WhenReadImageRecords_ThenIllegalStateExceptionIsThrown()}</li>
 *     <li>{@link ExcelServiceTest#GivenMissingExcelFile_WhenReadImageRecords_ThenRuntimeExceptionIsThrown()}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>All workbook files are created under JUnit's {@link TempDir} directory.</li>
 *     <li>Tests verify the stable column contract consumed later by image download and DOCX generation stages.</li>
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
class ExcelServiceTest {

    /**
     * Temporary directory used for generated workbook fixtures.
     */
    @TempDir
    Path tempDir;

    /**
     * <h3>Create workbook with post rows</h3>
     * Verifies that {@link ExcelService#createExcelFromRecords(List, Path, String)}
     * creates a new Excel workbook and writes post rows that can later be read
     * as image records.
     * <p>
     * This test targets the new-workbook branch that creates the canonical
     * header row and first set of post rows.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates two representative {@link PostRecord} fixtures.</li>
     *     <li>Writes those records into a new workbook.</li>
     *     <li>Reads image records back from the workbook.</li>
     *     <li>Asserts the workbook exists and two rows are available for image processing.</li>
     * </ul>
     *
     * @throws Exception when workbook creation or image record reading fails
     * @since 1.0
     *
     * @see ExcelService#createExcelFromRecords(List, Path, String)
     */
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

    /**
     * <h3>Append rows to existing workbook</h3>
     * Verifies that calling {@link ExcelService#createExcelFromRecords(List, Path, String)}
     * more than once for the same workbook appends rows instead of replacing the
     * previous content.
     * <p>
     * This test targets the existing-workbook branch used by paginated Patreon
     * responses within the same Excel batch.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Writes one post record into a new workbook.</li>
     *     <li>Writes a second post record into the same workbook path.</li>
     *     <li>Reads image records from the workbook.</li>
     *     <li>Asserts both rows remain available after the second write.</li>
     * </ul>
     *
     * @throws Exception when workbook writing or reading fails
     * @since 1.0
     *
     * @see ExcelService#createExcelFromRecords(List, Path, String)
     */
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

    /**
     * <h3>Create image records from pipe-separated URLs</h3>
     * Verifies that {@link ExcelService#readImageRecords(Path, String)} splits
     * pipe-separated image URLs into separate image records and builds
     * deterministic image names from row metadata.
     * <p>
     * This test targets large URL parsing, timestamp splitting, title
     * normalization, and per-image index formatting.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates a workbook with required image extraction headers.</li>
     *     <li>Writes one row containing two {@code large_url} values separated by {@code |}.</li>
     *     <li>Reads image records through {@link ExcelService#readImageRecords(Path, String)}.</li>
     *     <li>Asserts URL trimming and generated image-name content.</li>
     * </ul>
     *
     * @throws Exception when workbook fixture creation or image record reading fails
     * @since 1.0
     *
     * @see ExcelService#readImageRecords(Path, String)
     */
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

    /**
     * <h3>Reject workbook without header row</h3>
     * Verifies that {@link ExcelService#readImageRecords(Path, String)} throws
     * {@link IllegalStateException} when the configured sheet has no header row.
     * <p>
     * This test targets the validation required before column-name lookup can
     * occur.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates a workbook with the target sheet but no header row.</li>
     *     <li>Invokes image record reading.</li>
     *     <li>Asserts the exception type and missing-header message.</li>
     * </ul>
     *
     * @throws Exception when workbook fixture creation fails
     * @since 1.0
     *
     * @see ExcelService#readImageRecords(Path, String)
     */
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

    /**
     * <h3>Report missing workbook read failure</h3>
     * Verifies that {@link ExcelService#readImageRecords(Path, String)} wraps
     * file-system read failures in a {@link RuntimeException} when the workbook
     * does not exist.
     * <p>
     * This test targets the service's error handling around workbook input I/O.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Resolves a workbook path that does not exist.</li>
     *     <li>Invokes image record reading.</li>
     *     <li>Asserts that a runtime exception is thrown.</li>
     * </ul>
     *
     * @since 1.0
     *
     * @see ExcelService#readImageRecords(Path, String)
     */
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

