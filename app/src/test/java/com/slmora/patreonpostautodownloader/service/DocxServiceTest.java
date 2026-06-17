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

/**
 * The {@code DocxServiceTest} test class is created for verifying DOCX report
 * generation behavior implemented by {@link DocxService}.
 * <p>
 * It focuses on generating DOCX files from Excel workbooks, resolving output
 * file names from finalized Excel batch names, validating required workbook
 * structure, handling image download failures, and extracting readable text from
 * Patreon content JSON.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Verifies successful DOCX creation assigns the generated path to an {@link ExcelJob}.</li>
 *     <li>Verifies filename fallback behavior when the source Excel name does not match the configured pattern.</li>
 *     <li>Verifies missing Excel header rows fail fast with a clear exception.</li>
 *     <li>Verifies DOCX generation continues when an image URL cannot be downloaded.</li>
 *     <li>Verifies optional columns and mixed content JSON are handled without stopping report generation.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link DocxService}<br>
 * 2 - {@link ExcelJob}<br>
 * 3 - {@link ExcelJobTestBuilder}<br>
 * 4 - {@link XSSFWorkbook}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link DocxServiceTest#GivenValidExcelAndMatchingPattern_WhenCreateDocx_ThenDocxFileIsCreatedAndAssignedToJob()}</li>
 *     <li>{@link DocxServiceTest#GivenNonMatchingPattern_WhenCreateDocx_ThenFallbackTmpDocxNameIsUsed()}</li>
 *     <li>{@link DocxServiceTest#GivenExcelWithoutHeaderRow_WhenCreateDocx_ThenIllegalStateExceptionIsThrown()}</li>
 *     <li>{@link DocxServiceTest#GivenImageUrlDownloadFails_WhenCreateDocx_ThenDocxStillGeneratedWithFallbackImageText()}</li>
 *     <li>{@link DocxServiceTest#GivenMissingOptionalColumnsAndMixedContentJson_WhenCreateDocx_ThenDocxContainsExtractedAndFallbackText()}</li>
 *     <li>{@link DocxServiceTest#createExcelFile(Path, boolean, String, String)}</li>
 *     <li>{@link DocxServiceTest#readWordDocumentXml(Path)}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>All workbooks and DOCX files are written under JUnit's {@link TempDir} directory.</li>
 *     <li>The tests inspect {@code word/document.xml} directly when text-content assertions are required.</li>
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
class DocxServiceTest {

    /**
     * Temporary directory used for generated Excel workbook and DOCX report
     * fixtures.
     */
    @TempDir
    Path tempDir;

    /**
     * <h3>Create DOCX with derived batch file name</h3>
     * Verifies that {@link DocxService#createDocx(ExcelJob, Path, String, String, String)}
     * creates a DOCX report and stores the generated path on the job when the
     * Excel file name matches the configured pattern.
     * <p>
     * This test targets the normal file naming path used by finalized Excel
     * batches.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates a workbook whose file name contains a date range and job id.</li>
     *     <li>Invokes DOCX generation with a matching Excel filename pattern.</li>
     *     <li>Asserts the job receives a DOCX path containing the extracted batch suffix.</li>
     *     <li>Asserts the DOCX file exists on disk.</li>
     * </ul>
     *
     * @throws Exception when workbook creation or DOCX generation fails
     * @since 1.0
     *
     * @see DocxService#createDocx(ExcelJob, Path, String, String, String)
     */
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

    /**
     * <h3>Use fallback DOCX name for non-matching Excel file</h3>
     * Verifies that {@link DocxService#createDocx(ExcelJob, Path, String, String, String)}
     * uses the fallback {@code _tmp} file name when the Excel file name does not
     * match the configured extraction pattern.
     * <p>
     * This test targets the defensive filename fallback path.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates a valid workbook with a file name outside the expected batch pattern.</li>
     *     <li>Invokes DOCX generation with the standard pattern.</li>
     *     <li>Asserts the generated file name is {@code _tmp}.</li>
     *     <li>Asserts the fallback DOCX file exists.</li>
     * </ul>
     *
     * @throws Exception when workbook creation or DOCX generation fails
     * @since 1.0
     *
     * @see DocxService#createDocx(ExcelJob, Path, String, String, String)
     */
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

    /**
     * <h3>Reject workbook without header row</h3>
     * Verifies that {@link DocxService#createDocx(ExcelJob, Path, String, String, String)}
     * throws {@link IllegalStateException} when the configured sheet does not
     * contain a header row.
     * <p>
     * This test targets workbook structure validation before row mapping begins.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates an Excel workbook without a header row.</li>
     *     <li>Invokes DOCX generation.</li>
     *     <li>Asserts the exception type and message identify the missing header row.</li>
     * </ul>
     *
     * @throws Exception when workbook fixture setup fails
     * @since 1.0
     *
     * @see DocxService#createDocx(ExcelJob, Path, String, String, String)
     */
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

    /**
     * <h3>Generate DOCX when image download fails</h3>
     * Verifies that a failing remote image URL does not prevent
     * {@link DocxService#createDocx(ExcelJob, Path, String, String, String)}
     * from producing the DOCX report.
     * <p>
     * This test targets the image embedding fallback where the service writes
     * fallback image text instead of failing the whole document.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates a workbook with an unreachable image URL.</li>
     *     <li>Invokes DOCX generation.</li>
     *     <li>Asserts the job receives a DOCX path and the file exists.</li>
     * </ul>
     *
     * @throws Exception when workbook creation or DOCX generation fails unexpectedly
     * @since 1.0
     *
     * @see DocxService#createDocx(ExcelJob, Path, String, String, String)
     */
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

    /**
     * <h3>Handle optional columns and mixed content JSON</h3>
     * Verifies that DOCX generation tolerates missing optional URL/image
     * columns and writes both fallback raw text and extracted nested JSON text.
     * <p>
     * This test targets the workbook column lookup and content JSON text
     * extraction branches.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates a workbook that omits {@code patreon_url}, {@code large_url}, and {@code thumb_url} headers.</li>
     *     <li>Adds one row with malformed content JSON and one row with nested paragraph JSON.</li>
     *     <li>Generates a DOCX report from the workbook.</li>
     *     <li>Reads {@code word/document.xml} and asserts raw fallback and extracted text are present.</li>
     * </ul>
     *
     * @throws Exception when workbook creation, DOCX generation, or DOCX XML reading fails
     * @since 1.0
     *
     * @see DocxService#createDocx(ExcelJob, Path, String, String, String)
     */
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

    /**
     * <h3>Create Excel workbook fixture</h3>
     * Creates a minimal Excel workbook used by DOCX generation tests.
     *
     * @param excelFile target workbook path
     * @param withHeader whether the workbook should include the expected header row and one data row
     * @param thumbUrl thumbnail image URL value to write into the row
     * @param largeUrl large image URL value to write into the row
     *
     * @throws IOException when the workbook cannot be written
     * @since 1.0
     */
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

    /**
     * <h3>Read generated Word document XML</h3>
     * Opens a generated DOCX package and returns the main document XML content
     * for text assertions.
     *
     * @param docxPath generated DOCX file path
     *
     * @return UTF-8 text from {@code word/document.xml}
     * @throws IOException when the DOCX zip package or document entry cannot be read
     * @since 1.0
     */
    private String readWordDocumentXml(Path docxPath) throws IOException {
        try (ZipFile zip = new ZipFile(docxPath.toFile())) {
            var entry = zip.getEntry("word/document.xml");
            try (var in = zip.getInputStream(entry)) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }
}



