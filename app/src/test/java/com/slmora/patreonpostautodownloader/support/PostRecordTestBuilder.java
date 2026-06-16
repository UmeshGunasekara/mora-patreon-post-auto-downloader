package com.slmora.patreonpostautodownloader.support;

import com.slmora.patreonpostautodownloader.model.PostRecord;

public final class PostRecordTestBuilder {
    private String id = "post-1";
    private String publishedAt = "2026-06-01T10:15:30+00:00";
    private String title = "Sample Title";
    private String cleanedTeaserText = "teaser";
    private String contentJsonString = "{}";
    private int commentCount = 0;
    private String patreonUrl = "https://www.patreon.com/posts/1";
    private String largeUrl = "https://cdn.example.com/large-1.jpg";
    private String thumbUrl = "https://cdn.example.com/thumb-1.jpg";

    private PostRecordTestBuilder() {
    }

    public static PostRecordTestBuilder aPostRecord() {
        return new PostRecordTestBuilder();
    }

    public PostRecordTestBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public PostRecordTestBuilder withPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
        return this;
    }

    public PostRecordTestBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    public PostRecordTestBuilder withLargeUrl(String largeUrl) {
        this.largeUrl = largeUrl;
        return this;
    }

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

