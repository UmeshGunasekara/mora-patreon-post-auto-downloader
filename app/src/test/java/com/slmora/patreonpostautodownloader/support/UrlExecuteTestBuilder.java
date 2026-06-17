package com.slmora.patreonpostautodownloader.support;

import com.slmora.patreonpostautodownloader.model.PostRecord;
import com.slmora.patreonpostautodownloader.model.URLExecute;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code UrlExecuteTestBuilder} class is created for building deterministic
 * {@link URLExecute} fixtures used by Excel producer tests.
 * <p>
 * It models the parsed result of one Patreon API execution, including the
 * posts extracted from the response page and the optional pagination URL used
 * to continue producer processing.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Provides a fluent factory for creating {@link URLExecute} test fixtures.</li>
 *     <li>Allows tests to add one or more parsed {@link PostRecord} values.</li>
 *     <li>Allows tests to model single-page and multi-page Patreon pagination through {@code nextUrl}.</li>
 *     <li>Builds real {@link URLExecute} instances so producer tests exercise DTO state directly.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link URLExecute}<br>
 * 2 - {@link PostRecord}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link UrlExecuteTestBuilder#aUrlExecute()}</li>
 *     <li>{@link UrlExecuteTestBuilder#withPost(PostRecord)}</li>
 *     <li>{@link UrlExecuteTestBuilder#withPosts(List)}</li>
 *     <li>{@link UrlExecuteTestBuilder#withNextUrl(String)}</li>
 *     <li>{@link UrlExecuteTestBuilder#build()}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>This builder is intended for test support only and is not used by the runtime pipeline.</li>
 *     <li>The default fixture contains no posts and no next URL until a test explicitly supplies them.</li>
 *     <li>Pagination URLs are synthetic fixture values unless a test deliberately routes them through a mocked service.</li>
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
public final class UrlExecuteTestBuilder {
    /**
     * Parsed post records to assign to the built URL execution result.
     */
    private final List<PostRecord> posts = new ArrayList<>();

    /**
     * Optional next page URL to assign to the built URL execution result.
     */
    private String nextUrl;

    /**
     * <h3>Create hidden builder instance</h3>
     * Prevents direct construction so tests start from the named
     * {@link UrlExecuteTestBuilder#aUrlExecute()} factory method.
     * <p>
     * The factory keeps fixture creation readable and ensures every test starts
     * from an empty URL execution result.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Keeps construction private to enforce the fluent fixture entry point.</li>
     *     <li>Performs no network, file-system, or pipeline side effects.</li>
     * </ul>
     *
     * @since 1.0
     */
    private UrlExecuteTestBuilder() {
    }

    /**
     * <h3>Start URL execution fixture builder</h3>
     * Creates a new builder initialized with no parsed posts and no next page
     * URL.
     * <p>
     * Callers can chain post and pagination methods to model the exact response
     * shape needed by an Excel producer test.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates an independent builder instance for each URL execution fixture.</li>
     *     <li>Initializes an empty post list.</li>
     *     <li>Leaves {@code nextUrl} unset unless explicitly provided.</li>
     * </ul>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * URLExecute result = UrlExecuteTestBuilder.aUrlExecute()
     *         .withPost(PostRecordTestBuilder.aPostRecord().build())
     *         .withNextUrl("https://www.patreon.com/api/posts?page=2")
     *         .build();
     * }</pre>
     *
     * @return a new builder configured with an empty URL execution fixture state
     *
     * @since 1.0
     */
    public static UrlExecuteTestBuilder aUrlExecute() {
        return new UrlExecuteTestBuilder();
    }

    /**
     * <h3>Add parsed post fixture</h3>
     * Adds one parsed {@link PostRecord} to the built {@link URLExecute}
     * fixture.
     * <p>
     * Producer tests use this method when modeling a response page with one or
     * more Patreon posts appended to the Excel workbook.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Appends the supplied post record to this builder's internal list.</li>
     *     <li>Preserves insertion order for producer and workbook assertions.</li>
     *     <li>Returns the same builder instance for fluent chaining.</li>
     * </ul>
     *
     * @param postRecord parsed post record to add to the built fixture
     *
     * @return this builder instance for fluent fixture customization
     *
     * @since 1.0
     */
    public UrlExecuteTestBuilder withPost(PostRecord postRecord) {
        this.posts.add(postRecord);
        return this;
    }

    /**
     * <h3>Replace parsed post fixtures</h3>
     * Replaces all currently configured parsed posts with the supplied list.
     * <p>
     * Tests use this method when a scenario already has a prepared list that
     * should become the complete response-page content.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Clears any post records already added to this builder.</li>
     *     <li>Adds all supplied records in their existing order.</li>
     *     <li>Returns the same builder instance for fluent chaining.</li>
     * </ul>
     *
     * @param postRecords complete parsed post list to assign to the built fixture
     *
     * @return this builder instance for fluent fixture customization
     *
     * @since 1.0
     */
    public UrlExecuteTestBuilder withPosts(List<PostRecord> postRecords) {
        this.posts.clear();
        this.posts.addAll(postRecords);
        return this;
    }

    /**
     * <h3>Set fixture next page URL</h3>
     * Overrides the pagination URL assigned to the built {@link URLExecute}.
     * <p>
     * Excel producer tests use this value to verify whether processing stops
     * after one response page or continues to another mocked URL execution.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Stores the supplied next URL on this builder.</li>
     *     <li>Returns the same builder instance for fluent chaining.</li>
     * </ul>
     *
     * @param nextUrl next pagination URL to assign to the built fixture
     *
     * @return this builder instance for fluent fixture customization
     *
     * @since 1.0
     */
    public UrlExecuteTestBuilder withNextUrl(String nextUrl) {
        this.nextUrl = nextUrl;
        return this;
    }

    /**
     * <h3>Build URL execution fixture</h3>
     * Creates a real {@link URLExecute} and applies all values collected by this
     * builder.
     * <p>
     * The built result receives the configured post list and next URL, matching
     * the shape returned by URL execution parsing before the Excel producer
     * consumes it.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates a new mutable URL execution result DTO.</li>
     *     <li>Assigns the configured parsed posts through {@link URLExecute#setPosts(List)}.</li>
     *     <li>Sets the configured next page URL for pagination scenarios.</li>
     * </ul>
     *
     * @return a new {@link URLExecute} fixture populated from this builder's current state
     *
     * @implNote The current post list reference is passed to {@link URLExecute}
     * to match the mutable DTO style used by the runtime model.
     * @since 1.0
     */
    public URLExecute build() {
        URLExecute urlExecute = new URLExecute();
        urlExecute.setPosts(posts);
        urlExecute.setNextUrl(nextUrl);
        return urlExecute;
    }
}

