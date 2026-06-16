package com.slmora.patreonpostautodownloader.model;

import com.slmora.patreonpostautodownloader.support.ExcelJobTestBuilder;
import com.slmora.patreonpostautodownloader.support.ImageRecordTestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The {@code ExcelJobTest} test class is created for verifying model behavior
 * implemented by {@link ExcelJob}.
 * <p>
 * It focuses on retry-count mutation and the compact job summary text used by
 * queue logging, retry handling, and failed-job diagnostics in the Patreon post
 * download pipeline.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Validates {@link ExcelJob#incrementRetryCount()} increments the mutable retry counter.</li>
 *     <li>Verifies {@link ExcelJob#toString()} includes key job metadata without expanding image records.</li>
 *     <li>Checks representative boundary job identifiers used in log summaries.</li>
 *     <li>Uses test builders to create focused model fixtures with only relevant fields populated.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link ExcelJob}<br>
 * 2 - {@link ExcelJobTestBuilder}<br>
 * 3 - {@link ImageRecordTestBuilder}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link ExcelJobTest#GivenNewExcelJob_WhenIncrementRetryCountCalled_ThenRetryCountIsIncreasedByOne()}</li>
 *     <li>{@link ExcelJobTest#GivenBoundaryJobIds_WhenJobCreated_ThenToStringContainsJobSummary(long)}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>These tests verify local model behavior and do not start any pipeline process or file I/O workflow.</li>
 *     <li>The summary test intentionally asserts only stable summary fragments rather than the full formatted string.</li>
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
class ExcelJobTest {

    /**
     * <h3>Increment retry count by one</h3>
     * Verifies that {@link ExcelJob#incrementRetryCount()} increases the
     * current retry count by exactly one.
     * <p>
     * This test targets the model helper used by retry routing before a job is
     * returned to retry processing.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates an {@link ExcelJob} fixture with an initial retry count of {@code 2}.</li>
     *     <li>Invokes {@link ExcelJob#incrementRetryCount()}.</li>
     *     <li>Asserts that the retry count becomes {@code 3}.</li>
     * </ul>
     *
     * @since 1.0
     *
     * @see ExcelJob#incrementRetryCount()
     */
    @Test
    void GivenNewExcelJob_WhenIncrementRetryCountCalled_ThenRetryCountIsIncreasedByOne() {
        // Arrange
        ExcelJob excelJob = ExcelJobTestBuilder.anExcelJob().withRetryCount(2).build();

        // Act
        excelJob.incrementRetryCount();

        // Assert
        assertThat(excelJob.getRetryCount()).isEqualTo(3);
    }

    /**
     * <h3>Include job summary fields in text output</h3>
     * Verifies that {@link ExcelJob#toString()} includes the job id, Excel file,
     * retry count field, and error message for representative boundary job ids.
     * <p>
     * This test targets the compact summary used in logs while ensuring image
     * records can exist on the job without being the asserted output focus.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Runs the same summary assertion for {@code 0}, {@code 1}, and {@link Long#MAX_VALUE} job ids.</li>
     *     <li>Creates a job fixture with an Excel path, one image record, and an error message.</li>
     *     <li>Invokes {@link ExcelJob#toString()}.</li>
     *     <li>Asserts that stable summary fields are present in the returned text.</li>
     * </ul>
     *
     * @param jobId boundary job identifier used in the model fixture
     *
     * @since 1.0
     *
     * @see ExcelJob#toString()
     */
    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, Long.MAX_VALUE})
    void GivenBoundaryJobIds_WhenJobCreated_ThenToStringContainsJobSummary(long jobId) {
        // Arrange
        Path excelPath = Path.of("C:/tmp/sample.xlsx");
        ExcelJob excelJob = ExcelJobTestBuilder.anExcelJob()
                .withJobId(jobId)
                .withExcelFile(excelPath)
                .withImageRecord(ImageRecordTestBuilder.anImageRecord().build())
                .withErrorMessage("none")
                .build();

        // Act
        String jobSummary = excelJob.toString();

        // Assert
        assertThat(jobSummary).contains("jobId=" + jobId);
        assertThat(jobSummary).contains("excelFile=" + excelPath);
        assertThat(jobSummary).contains("retryCount=");
        assertThat(jobSummary).contains("errorMessage='none'");
    }
}


