package com.slmora.patreonpostautodownloader.service;

import com.slmora.patreonpostautodownloader.model.ExcelJob;
import com.slmora.patreonpostautodownloader.support.ExcelJobTestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The {@code CleanupServiceTest} test class is created for verifying cleanup
 * behavior implemented by {@link CleanupService}.
 * <p>
 * It focuses on explicit Excel file deletion, null Excel path handling, and the
 * success-cleanup hook used after DOCX generation in the Patreon post download
 * pipeline.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Verifies {@link CleanupService#deleteExcelFile(ExcelJob)} deletes an existing Excel file.</li>
 *     <li>Verifies a job with a null Excel file path exits deletion without failure.</li>
 *     <li>Verifies {@link CleanupService#cleanupAfterSuccess(ExcelJob)} completes for a valid job fixture.</li>
 *     <li>Uses JUnit temporary directories to isolate file-system side effects.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link CleanupService}<br>
 * 2 - {@link ExcelJob}<br>
 * 3 - {@link ExcelJobTestBuilder}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link CleanupServiceTest#GivenExcelFileExists_WhenDeleteExcelFile_ThenFileIsDeleted()}</li>
 *     <li>{@link CleanupServiceTest#GivenExcelFileIsNull_WhenDeleteExcelFile_ThenMethodExitsWithoutFailure()}</li>
 *     <li>{@link CleanupServiceTest#GivenValidJob_WhenCleanupAfterSuccess_ThenMethodCompletesWithoutException()}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>These tests do not run the DOCX producer; they validate cleanup service behavior directly.</li>
 *     <li>The success-cleanup test reflects the current implementation, which logs completion and keeps Excel files in place.</li>
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
class CleanupServiceTest {

    /**
     * Temporary directory used to create disposable Excel file fixtures.
     */
    @TempDir
    Path tempDir;

    /**
     * <h3>Delete existing Excel file</h3>
     * Verifies that {@link CleanupService#deleteExcelFile(ExcelJob)} removes
     * the Excel file referenced by the supplied job.
     * <p>
     * This test targets the real file-system deletion branch using an isolated
     * temporary file.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates a temporary {@code .xlsx} fixture file.</li>
     *     <li>Builds an {@link ExcelJob} that points to the fixture file.</li>
     *     <li>Invokes {@link CleanupService#deleteExcelFile(ExcelJob)}.</li>
     *     <li>Asserts that the file no longer exists.</li>
     * </ul>
     *
     * @throws Exception when temporary file creation or deletion fails
     * @since 1.0
     *
     * @see CleanupService#deleteExcelFile(ExcelJob)
     */
    @Test
    void GivenExcelFileExists_WhenDeleteExcelFile_ThenFileIsDeleted() throws Exception {
        // Arrange
        Path excelFile = tempDir.resolve("sample.xlsx");
        // Create a concrete file because deleteExcelFile uses Files.deleteIfExists on the job path.
        Files.writeString(excelFile, "content", StandardCharsets.UTF_8);
        ExcelJob job = ExcelJobTestBuilder.anExcelJob().withExcelFile(excelFile).build();
        CleanupService cleanupService = new CleanupService();

        // Act
        cleanupService.deleteExcelFile(job);

        // Assert
        assertThat(Files.exists(excelFile)).isFalse();
    }

    /**
     * <h3>Ignore null Excel path</h3>
     * Verifies that {@link CleanupService#deleteExcelFile(ExcelJob)} exits
     * without failure when the job does not contain an Excel file path.
     * <p>
     * This test targets the guard branch that prevents a null path from reaching
     * the file-system delete operation.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates an {@link ExcelJob} with a null Excel file path.</li>
     *     <li>Invokes the explicit Excel deletion method.</li>
     *     <li>Asserts the job still has no Excel file path.</li>
     * </ul>
     *
     * @throws Exception when the cleanup method reports an unexpected deletion failure
     * @since 1.0
     *
     * @see CleanupService#deleteExcelFile(ExcelJob)
     */
    @Test
    void GivenExcelFileIsNull_WhenDeleteExcelFile_ThenMethodExitsWithoutFailure() throws Exception {
        // Arrange
        ExcelJob job = new ExcelJob(77L, null);
        CleanupService cleanupService = new CleanupService();

        // Act
        cleanupService.deleteExcelFile(job);

        // Assert
        assertThat(job.getExcelFile()).isNull();
    }

    /**
     * <h3>Complete success cleanup hook</h3>
     * Verifies that {@link CleanupService#cleanupAfterSuccess(ExcelJob)}
     * completes for a valid job fixture.
     * <p>
     * This test targets the current success-cleanup hook, which logs completion
     * and does not delete the generated Excel file.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates a representative {@link ExcelJob} fixture.</li>
     *     <li>Invokes {@link CleanupService#cleanupAfterSuccess(ExcelJob)}.</li>
     *     <li>Asserts the fixture remains valid after cleanup completes.</li>
     * </ul>
     *
     * @since 1.0
     *
     * @see CleanupService#cleanupAfterSuccess(ExcelJob)
     */
    @Test
    void GivenValidJob_WhenCleanupAfterSuccess_ThenMethodCompletesWithoutException() {
        // Arrange
        ExcelJob job = ExcelJobTestBuilder.anExcelJob().build();
        CleanupService cleanupService = new CleanupService();

        // Act
        cleanupService.cleanupAfterSuccess(job);

        // Assert
        assertThat(job.getJobId()).isPositive();
    }
}

