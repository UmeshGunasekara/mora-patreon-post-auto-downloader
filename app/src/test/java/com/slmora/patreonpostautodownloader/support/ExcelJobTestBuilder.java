package com.slmora.patreonpostautodownloader.support;

import com.slmora.patreonpostautodownloader.model.ExcelJob;
import com.slmora.patreonpostautodownloader.model.ImageRecord;
import com.slmora.patreonpostautodownloader.model.JobStatus;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@code ExcelJobTestBuilder} class is created for building deterministic
 * {@link ExcelJob} fixtures used by pipeline, process, model, and service tests.
 * <p>
 * It centralizes the common default values for an Excel pipeline job while
 * allowing individual tests to override only the fields that matter for the
 * scenario under verification.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Provides a fluent factory for creating {@link ExcelJob} test fixtures.</li>
 *     <li>Uses stable default paths, job status, and job identifiers for repeatable assertions.</li>
 *     <li>Allows tests to customize retry state, error messages, DOCX path, Excel path, and image records.</li>
 *     <li>Builds real {@link ExcelJob} instances instead of mocks for state-focused test coverage.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link ExcelJob}<br>
 * 2 - {@link ImageRecord}<br>
 * 3 - {@link JobStatus}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link ExcelJobTestBuilder#anExcelJob()}</li>
 *     <li>{@link ExcelJobTestBuilder#withJobId(long)}</li>
 *     <li>{@link ExcelJobTestBuilder#withExcelFile(Path)}</li>
 *     <li>{@link ExcelJobTestBuilder#withDocxFile(Path)}</li>
 *     <li>{@link ExcelJobTestBuilder#withStatus(JobStatus)}</li>
 *     <li>{@link ExcelJobTestBuilder#withRetryCount(int)}</li>
 *     <li>{@link ExcelJobTestBuilder#withErrorMessage(String)}</li>
 *     <li>{@link ExcelJobTestBuilder#withImageRecord(ImageRecord)}</li>
 *     <li>{@link ExcelJobTestBuilder#build()}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>This builder is intended for test code only and is not part of the application runtime pipeline.</li>
 *     <li>Defaults are synthetic fixture values and do not require real files to exist unless a test explicitly performs file I/O.</li>
 *     <li>The builder stores image records in insertion order so tests can assert downstream record ordering when needed.</li>
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
public final class ExcelJobTestBuilder {
    /**
     * Default job identifier used when a test does not need a specific ID.
     */
    private long jobId = 101L;

    /**
     * Default Excel workbook path matching the pipeline's generated-file naming style.
     */
    private Path excelFile = Path.of("C:/tmp/patreon_posts_output_20260101_20260130_J101.xlsx");

    /**
     * Default DOCX output path matching the report naming style used by document tests.
     */
    private Path docxFile = Path.of("C:/tmp/patreon_posts_report_20260101_20260130_J101.docx");

    /**
     * Default status representing a job that has completed Excel generation.
     */
    private JobStatus status = JobStatus.EXCEL_CREATED;

    /**
     * Retry count to assign to the built job.
     */
    private int retryCount;

    /**
     * Error message to assign to the built job.
     */
    private String errorMessage;

    /**
     * Image records to copy into the built job in insertion order.
     */
    private final List<ImageRecord> imageRecords = new ArrayList<>();

    /**
     * <h3>Create hidden builder instance</h3>
     * Prevents direct construction so tests start from the named
     * {@link ExcelJobTestBuilder#anExcelJob()} factory method.
     * <p>
     * The factory keeps fixture creation readable at call sites and matches the
     * fluent test-builder style used by this support class.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Keeps construction private to enforce a single entry point for default fixture setup.</li>
     *     <li>Performs no file, queue, or pipeline side effects.</li>
     * </ul>
     *
     * @since 1.0
     */
    private ExcelJobTestBuilder() {
    }

    /**
     * <h3>Start Excel job fixture builder</h3>
     * Creates a new builder initialized with deterministic default values for an
     * {@link ExcelJob} test fixture.
     * <p>
     * Callers can chain {@code with...} methods to override only the values
     * relevant to the current test scenario.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates an independent builder instance for each test fixture.</li>
     *     <li>Initializes defaults for job ID, Excel path, DOCX path, status, retry count, and error message.</li>
     *     <li>Leaves image records empty until explicitly added by the test.</li>
     * </ul>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * ExcelJob job = ExcelJobTestBuilder.anExcelJob()
     *         .withRetryCount(2)
     *         .build();
     * }</pre>
     *
     * @return a new builder configured with default Excel job fixture values
     *
     * @since 1.0
     */
    public static ExcelJobTestBuilder anExcelJob() {
        return new ExcelJobTestBuilder();
    }

    /**
     * <h3>Set fixture job identifier</h3>
     * Overrides the default job identifier assigned to the built {@link ExcelJob}.
     * <p>
     * Tests use this value when verifying log output, persistence content, or
     * generated path behavior that includes a job ID.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Stores the supplied identifier on this builder.</li>
     *     <li>Returns the same builder instance for fluent chaining.</li>
     * </ul>
     *
     * @param jobId job identifier to assign to the built fixture
     *
     * @return this builder instance for fluent fixture customization
     *
     * @since 1.0
     */
    public ExcelJobTestBuilder withJobId(long jobId) {
        this.jobId = jobId;
        return this;
    }

    /**
     * <h3>Set fixture Excel file</h3>
     * Overrides the Excel workbook path assigned to the built {@link ExcelJob}.
     * <p>
     * Tests use this path for scenarios that read, write, delete, or derive
     * output names from a job's Excel file.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Stores the supplied Excel path on this builder.</li>
     *     <li>Returns the same builder instance for fluent chaining.</li>
     * </ul>
     *
     * @param excelFile Excel workbook path to assign to the built fixture
     *
     * @return this builder instance for fluent fixture customization
     *
     * @since 1.0
     */
    public ExcelJobTestBuilder withExcelFile(Path excelFile) {
        this.excelFile = excelFile;
        return this;
    }

    /**
     * <h3>Set fixture DOCX file</h3>
     * Overrides the DOCX report path assigned to the built {@link ExcelJob}.
     * <p>
     * Tests use this value when verifying document generation, cleanup, or
     * persistence behavior that depends on the report path.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Stores the supplied DOCX path on this builder.</li>
     *     <li>Returns the same builder instance for fluent chaining.</li>
     * </ul>
     *
     * @param docxFile DOCX report path to assign to the built fixture
     *
     * @return this builder instance for fluent fixture customization
     *
     * @since 1.0
     */
    public ExcelJobTestBuilder withDocxFile(Path docxFile) {
        this.docxFile = docxFile;
        return this;
    }

    /**
     * <h3>Set fixture job status</h3>
     * Overrides the lifecycle status assigned to the built {@link ExcelJob}.
     * <p>
     * Tests use this value to exercise process, retry, cleanup, and persistence
     * branches that depend on job state.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Stores the supplied status on this builder.</li>
     *     <li>Returns the same builder instance for fluent chaining.</li>
     * </ul>
     *
     * @param status job lifecycle status to assign to the built fixture
     *
     * @return this builder instance for fluent fixture customization
     *
     * @since 1.0
     */
    public ExcelJobTestBuilder withStatus(JobStatus status) {
        this.status = status;
        return this;
    }

    /**
     * <h3>Set fixture retry count</h3>
     * Overrides the retry count assigned to the built {@link ExcelJob}.
     * <p>
     * Retry-focused tests use this value to place the job below, at, or above
     * the configured retry limit.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Stores the supplied retry count on this builder.</li>
     *     <li>Returns the same builder instance for fluent chaining.</li>
     * </ul>
     *
     * @param retryCount retry count to assign to the built fixture
     *
     * @return this builder instance for fluent fixture customization
     *
     * @since 1.0
     */
    public ExcelJobTestBuilder withRetryCount(int retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    /**
     * <h3>Set fixture error message</h3>
     * Overrides the error message assigned to the built {@link ExcelJob}.
     * <p>
     * Failure-path tests use this value when verifying logging, persistence, or
     * queue handoff behavior for failed jobs.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Stores the supplied error message on this builder.</li>
     *     <li>Returns the same builder instance for fluent chaining.</li>
     * </ul>
     *
     * @param errorMessage error message to assign to the built fixture
     *
     * @return this builder instance for fluent fixture customization
     *
     * @since 1.0
     */
    public ExcelJobTestBuilder withErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    /**
     * <h3>Add fixture image record</h3>
     * Adds one {@link ImageRecord} to the image records copied into the built
     * {@link ExcelJob}.
     * <p>
     * Tests use this method when they need realistic job image content for
     * image download, DOCX generation, cleanup, or persistence scenarios.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Appends the supplied image record to this builder's internal list.</li>
     *     <li>Preserves insertion order for the final job image-record list.</li>
     *     <li>Returns the same builder instance for fluent chaining.</li>
     * </ul>
     *
     * @param imageRecord image record to add to the built fixture
     *
     * @return this builder instance for fluent fixture customization
     *
     * @since 1.0
     */
    public ExcelJobTestBuilder withImageRecord(ImageRecord imageRecord) {
        this.imageRecords.add(imageRecord);
        return this;
    }

    /**
     * <h3>Build Excel job fixture</h3>
     * Creates a real {@link ExcelJob} and applies all values collected by this
     * builder.
     * <p>
     * The built job receives constructor values first, then mutable processing
     * fields are populated to match the requested test scenario.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Constructs the job with the configured job ID and Excel workbook path.</li>
     *     <li>Sets DOCX path, status, retry count, and error message on the job.</li>
     *     <li>Copies configured image records into the job's synchronized image-record list.</li>
     * </ul>
     *
     * @return a new {@link ExcelJob} fixture populated from this builder's current state
     *
     * @implNote Image records are copied into the built job rather than exposing
     * this builder's mutable list.
     * @since 1.0
     */
    public ExcelJob build() {
        ExcelJob excelJob = new ExcelJob(jobId, excelFile);
        excelJob.setDocxFile(docxFile);
        excelJob.setStatus(status);
        excelJob.setRetryCount(retryCount);
        excelJob.setErrorMessage(errorMessage);
        excelJob.getImageRecords().addAll(imageRecords);
        return excelJob;
    }
}

