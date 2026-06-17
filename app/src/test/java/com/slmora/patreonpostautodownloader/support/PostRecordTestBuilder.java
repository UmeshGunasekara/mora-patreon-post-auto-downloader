package com.slmora.patreonpostautodownloader.support;

import com.slmora.patreonpostautodownloader.model.PostRecord;

/**
 * The {@code PostRecordTestBuilder} class is created for building deterministic
 * {@link PostRecord} fixtures used by Excel writer and Excel producer tests.
 * <p>
 * It centralizes representative Patreon post values so tests can create
 * realistic records while overriding only the fields that matter for workbook
 * generation, pagination, or image URL extraction scenarios.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Provides a fluent factory for creating {@link PostRecord} test fixtures.</li>
 *     <li>Uses stable default values for post identity, timestamp, content, Patreon URL, and image URLs.</li>
 *     <li>Allows tests to customize common assertion fields such as ID, publication time, title, and large image URL.</li>
 *     <li>Builds real {@link PostRecord} instances so Excel mapping tests exercise DTO state directly.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link PostRecord}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link PostRecordTestBuilder#aPostRecord()}</li>
 *     <li>{@link PostRecordTestBuilder#withId(String)}</li>
 *     <li>{@link PostRecordTestBuilder#withPublishedAt(String)}</li>
 *     <li>{@link PostRecordTestBuilder#withTitle(String)}</li>
 *     <li>{@link PostRecordTestBuilder#withLargeUrl(String)}</li>
 *     <li>{@link PostRecordTestBuilder#build()}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>This builder is intended for test support only and is not used by the runtime pipeline.</li>
 *     <li>Default URL values are synthetic and do not require live Patreon or CDN access.</li>
 *     <li>Defaults cover the fields written by {@code ExcelService}, including post metadata and image URL columns.</li>
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
public final class PostRecordTestBuilder {
    /**
     * Default Patreon post identifier used when a test does not need a custom ID.
     */
    private String id = "post-1";

    /**
     * Default publication timestamp used by workbook mapping fixtures.
     */
    private String publishedAt = "2026-06-01T10:15:30+00:00";

    /**
     * Default post title used in generated workbook assertions.
     */
    private String title = "Sample Title";

    /**
     * Default teaser text assigned to the built post record.
     */
    private String cleanedTeaserText = "teaser";

    /**
     * Default content JSON string assigned to the built post record.
     */
    private String contentJsonString = "{}";

    /**
     * Default comment count assigned to the built post record.
     */
    private int commentCount = 0;

    /**
     * Default public Patreon URL assigned to the built post record.
     */
    private String patreonUrl = "https://www.patreon.com/posts/1";

    /**
     * Default large image URL used by image URL extraction and workbook tests.
     */
    private String largeUrl = "https://cdn.example.com/large-1.jpg";

    /**
     * Default thumbnail image URL assigned to the built post record.
     */
    private String thumbUrl = "https://cdn.example.com/thumb-1.jpg";

    /**
     * <h3>Create hidden builder instance</h3>
     * Prevents direct construction so tests start from the named
     * {@link PostRecordTestBuilder#aPostRecord()} factory method.
     * <p>
     * The factory keeps fixture creation readable and ensures every test starts
     * from the same representative Patreon post state.
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
    private PostRecordTestBuilder() {
    }

    /**
     * <h3>Start post record fixture builder</h3>
     * Creates a new builder initialized with deterministic default values for a
     * {@link PostRecord} test fixture.
     * <p>
     * Callers can chain {@code with...} methods to override fields used by a
     * specific Excel workbook or producer scenario.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates an independent builder instance for each post-record fixture.</li>
     *     <li>Initializes representative Patreon metadata, content, and image URL fields.</li>
     *     <li>Leaves all values synthetic and local to the test fixture.</li>
     * </ul>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * PostRecord post = PostRecordTestBuilder.aPostRecord()
     *         .withId("42")
     *         .withTitle("Pipeline post")
     *         .build();
     * }</pre>
     *
     * @return a new builder configured with default post-record fixture values
     *
     * @since 1.0
     */
    public static PostRecordTestBuilder aPostRecord() {
        return new PostRecordTestBuilder();
    }

    /**
     * <h3>Set fixture post identifier</h3>
     * Overrides the Patreon post identifier assigned to the built
     * {@link PostRecord}.
     * <p>
     * Excel and producer tests use this value when verifying row identity,
     * pagination output, or workbook column content.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Stores the supplied post identifier on this builder.</li>
     *     <li>Returns the same builder instance for fluent chaining.</li>
     * </ul>
     *
     * @param id Patreon post identifier to assign to the built fixture
     *
     * @return this builder instance for fluent fixture customization
     *
     * @since 1.0
     */
    public PostRecordTestBuilder withId(String id) {
        this.id = id;
        return this;
    }

    /**
     * <h3>Set fixture publication timestamp</h3>
     * Overrides the publication timestamp assigned to the built
     * {@link PostRecord}.
     * <p>
     * Tests use this value when verifying date grouping, workbook columns, or
     * generated batch metadata derived from post dates.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Stores the supplied publication timestamp on this builder.</li>
     *     <li>Returns the same builder instance for fluent chaining.</li>
     * </ul>
     *
     * @param publishedAt publication timestamp to assign to the built fixture
     *
     * @return this builder instance for fluent fixture customization
     *
     * @since 1.0
     */
    public PostRecordTestBuilder withPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
        return this;
    }

    /**
     * <h3>Set fixture title</h3>
     * Overrides the post title assigned to the built {@link PostRecord}.
     * <p>
     * Tests use this value when verifying title mapping into Excel rows or
     * generated downstream report input.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Stores the supplied title on this builder.</li>
     *     <li>Returns the same builder instance for fluent chaining.</li>
     * </ul>
     *
     * @param title post title to assign to the built fixture
     *
     * @return this builder instance for fluent fixture customization
     *
     * @since 1.0
     */
    public PostRecordTestBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * <h3>Set fixture large image URL</h3>
     * Overrides the large image URL assigned to the built {@link PostRecord}.
     * <p>
     * Tests use this value when verifying Excel image URL columns and later
     * image-record extraction from generated workbooks.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Stores the supplied large image URL on this builder.</li>
     *     <li>Returns the same builder instance for fluent chaining.</li>
     * </ul>
     *
     * @param largeUrl large image URL to assign to the built fixture
     *
     * @return this builder instance for fluent fixture customization
     *
     * @since 1.0
     */
    public PostRecordTestBuilder withLargeUrl(String largeUrl) {
        this.largeUrl = largeUrl;
        return this;
    }

    /**
     * <h3>Build post record fixture</h3>
     * Creates a real {@link PostRecord} and applies all values collected by this
     * builder.
     * <p>
     * The built record is populated through Lombok-generated setters, matching
     * how parsed Patreon data is represented before Excel generation.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates a new mutable post record DTO.</li>
     *     <li>Sets identity, timestamp, title, teaser, content, comment count, Patreon URL, and image URLs.</li>
     *     <li>Returns a populated model instance for direct use in Excel and producer tests.</li>
     * </ul>
     *
     * @return a new {@link PostRecord} fixture populated from this builder's current state
     *
     * @since 1.0
     */
    public PostRecord build() {
        PostRecord postRecord = new PostRecord();
        postRecord.setId(id);
        postRecord.setPublishedAt(publishedAt);
        postRecord.setTitle(title);
        postRecord.setCleanedTeaserText(cleanedTeaserText);
        postRecord.setContentJsonString(contentJsonString);
        postRecord.setCommentCount(commentCount);
        postRecord.setPatreonUrl(patreonUrl);
        postRecord.setLargeUrl(largeUrl);
        postRecord.setThumbUrl(thumbUrl);
        return postRecord;
    }
}

