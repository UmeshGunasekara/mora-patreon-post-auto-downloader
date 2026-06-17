package com.slmora.patreonpostautodownloader.support;

import com.slmora.patreonpostautodownloader.model.DownloadStatus;
import com.slmora.patreonpostautodownloader.model.ImageRecord;

import java.nio.file.Path;

/**
 * The {@code ImageRecordTestBuilder} class is created for building
 * deterministic {@link ImageRecord} fixtures used by model, process, and
 * service tests.
 * <p>
 * It centralizes common image-record defaults while allowing tests to override
 * only the source row, URL, generated name, download path, status, or error
 * details needed for a specific pipeline scenario.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Provides a fluent factory for creating {@link ImageRecord} test fixtures.</li>
 *     <li>Uses stable default image metadata for repeatable image-processing assertions.</li>
 *     <li>Allows tests to model pending, successful, and failed download states.</li>
 *     <li>Builds real {@link ImageRecord} instances so tests cover mutable DTO state directly.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link ImageRecord}<br>
 * 2 - {@link DownloadStatus}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link ImageRecordTestBuilder#anImageRecord()}</li>
 *     <li>{@link ImageRecordTestBuilder#withRowNumber(int)}</li>
 *     <li>{@link ImageRecordTestBuilder#withImageUrl(String)}</li>
 *     <li>{@link ImageRecordTestBuilder#withImageName(String)}</li>
 *     <li>{@link ImageRecordTestBuilder#withDownloadedImagePath(Path)}</li>
 *     <li>{@link ImageRecordTestBuilder#withDownloadStatus(DownloadStatus)}</li>
 *     <li>{@link ImageRecordTestBuilder#withErrorMessage(String)}</li>
 *     <li>{@link ImageRecordTestBuilder#build()}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>This builder is intended for test support only and is not used by the runtime pipeline.</li>
 *     <li>The default URL is synthetic and does not require network access unless a test explicitly invokes download behavior.</li>
 *     <li>The default status is {@link DownloadStatus#PENDING}, matching newly discovered image records.</li>
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
public final class ImageRecordTestBuilder {
    /**
     * Default Excel row number associated with the synthetic image record.
     */
    private int rowNumber = 1;

    /**
     * Default source image URL used when a test does not need a custom URL.
     */
    private String imageUrl = "https://cdn.example.com/image-1.jpg";

    /**
     * Default generated image name used by image download test fixtures.
     */
    private String imageName = "image-1";

    /**
     * Downloaded image path assigned to the built record.
     */
    private Path downloadedImagePath;

    /**
     * Download status assigned to the built record.
     */
    private DownloadStatus downloadStatus = DownloadStatus.PENDING;

    /**
     * Error message assigned to the built record.
     */
    private String errorMessage;

    /**
     * <h3>Create hidden builder instance</h3>
     * Prevents direct construction so tests start from the named
     * {@link ImageRecordTestBuilder#anImageRecord()} factory method.
     * <p>
     * The factory keeps fixture creation readable and ensures each test starts
     * from the same default image-record state.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Keeps construction private to enforce the fluent fixture entry point.</li>
     *     <li>Performs no file-system, network, or pipeline side effects.</li>
     * </ul>
     *
     * @since 1.0
     */
    private ImageRecordTestBuilder() {
    }

    /**
     * <h3>Start image record fixture builder</h3>
     * Creates a new builder initialized with deterministic default values for an
     * {@link ImageRecord} test fixture.
     * <p>
     * Callers can chain {@code with...} methods to model the exact download
     * state required by a test.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates an independent builder instance for each image-record fixture.</li>
     *     <li>Initializes source row, image URL, generated image name, and pending status.</li>
     *     <li>Leaves downloaded path and error message unset unless explicitly provided.</li>
     * </ul>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * ImageRecord record = ImageRecordTestBuilder.anImageRecord()
     *         .withDownloadStatus(DownloadStatus.SUCCESS)
     *         .build();
     * }</pre>
     *
     * @return a new builder configured with default image-record fixture values
     *
     * @since 1.0
     */
    public static ImageRecordTestBuilder anImageRecord() {
        return new ImageRecordTestBuilder();
    }

    /**
     * <h3>Set fixture row number</h3>
     * Overrides the Excel row number assigned to the built {@link ImageRecord}.
     * <p>
     * Tests use this value when verifying row-specific diagnostics, persistence,
     * or duplicate-image handling.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Stores the supplied row number on this builder.</li>
     *     <li>Returns the same builder instance for fluent chaining.</li>
     * </ul>
     *
     * @param rowNumber Excel row number to assign to the built fixture
     *
     * @return this builder instance for fluent fixture customization
     *
     * @since 1.0
     */
    public ImageRecordTestBuilder withRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
        return this;
    }

    /**
     * <h3>Set fixture image URL</h3>
     * Overrides the source image URL assigned to the built {@link ImageRecord}.
     * <p>
     * Image download tests use this value to simulate successful responses,
     * failures, duplicate URLs, or URL-specific file naming behavior.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Stores the supplied image URL on this builder.</li>
     *     <li>Returns the same builder instance for fluent chaining.</li>
     * </ul>
     *
     * @param imageUrl source image URL to assign to the built fixture
     *
     * @return this builder instance for fluent fixture customization
     *
     * @since 1.0
     */
    public ImageRecordTestBuilder withImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        return this;
    }

    /**
     * <h3>Set fixture image name</h3>
     * Overrides the generated image name assigned to the built
     * {@link ImageRecord}.
     * <p>
     * Tests use this value when verifying output file naming, duplicate names,
     * and persisted image metadata.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Stores the supplied image name on this builder.</li>
     *     <li>Returns the same builder instance for fluent chaining.</li>
     * </ul>
     *
     * @param imageName generated image name to assign to the built fixture
     *
     * @return this builder instance for fluent fixture customization
     *
     * @since 1.0
     */
    public ImageRecordTestBuilder withImageName(String imageName) {
        this.imageName = imageName;
        return this;
    }

    /**
     * <h3>Set fixture downloaded image path</h3>
     * Overrides the downloaded image path assigned to the built
     * {@link ImageRecord}.
     * <p>
     * Tests use this value when modeling records that already downloaded an
     * image or when verifying cleanup and document generation behavior.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Stores the supplied downloaded image path on this builder.</li>
     *     <li>Returns the same builder instance for fluent chaining.</li>
     * </ul>
     *
     * @param downloadedImagePath downloaded image path to assign to the built fixture
     *
     * @return this builder instance for fluent fixture customization
     *
     * @since 1.0
     */
    public ImageRecordTestBuilder withDownloadedImagePath(Path downloadedImagePath) {
        this.downloadedImagePath = downloadedImagePath;
        return this;
    }

    /**
     * <h3>Set fixture download status</h3>
     * Overrides the download status assigned to the built {@link ImageRecord}.
     * <p>
     * Process and service tests use this value to exercise success, failure,
     * retry, and skip branches that depend on image state.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Stores the supplied download status on this builder.</li>
     *     <li>Returns the same builder instance for fluent chaining.</li>
     * </ul>
     *
     * @param downloadStatus download status to assign to the built fixture
     *
     * @return this builder instance for fluent fixture customization
     *
     * @since 1.0
     */
    public ImageRecordTestBuilder withDownloadStatus(DownloadStatus downloadStatus) {
        this.downloadStatus = downloadStatus;
        return this;
    }

    /**
     * <h3>Set fixture error message</h3>
     * Overrides the error message assigned to the built {@link ImageRecord}.
     * <p>
     * Failure-path tests use this value when verifying retry decisions, failed
     * job reporting, or persisted image failure details.
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
    public ImageRecordTestBuilder withErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    /**
     * <h3>Build image record fixture</h3>
     * Creates a real {@link ImageRecord} and applies all values collected by
     * this builder.
     * <p>
     * The built record receives immutable source identity values through the
     * constructor, then mutable download fields are set to match the test
     * scenario.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Constructs the record with the configured row number, image URL, and image name.</li>
     *     <li>Sets downloaded image path, download status, and error message on the record.</li>
     *     <li>Returns a new model instance for direct use in service and process tests.</li>
     * </ul>
     *
     * @return a new {@link ImageRecord} fixture populated from this builder's current state
     *
     * @since 1.0
     */
    public ImageRecord build() {
        ImageRecord imageRecord = new ImageRecord(rowNumber, imageUrl, imageName);
        imageRecord.setDownloadedImagePath(downloadedImagePath);
        imageRecord.setDownloadStatus(downloadStatus);
        imageRecord.setErrorMessage(errorMessage);
        return imageRecord;
    }
}

