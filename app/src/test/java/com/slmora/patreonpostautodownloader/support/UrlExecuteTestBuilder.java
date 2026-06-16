package com.slmora.patreonpostautodownloader.support;

import com.slmora.patreonpostautodownloader.model.PostRecord;
import com.slmora.patreonpostautodownloader.model.URLExecute;

import java.util.ArrayList;
import java.util.List;

public final class UrlExecuteTestBuilder {
    private final List<PostRecord> posts = new ArrayList<>();
    private String nextUrl;

    private UrlExecuteTestBuilder() {
    }

    public static UrlExecuteTestBuilder aUrlExecute() {
        return new UrlExecuteTestBuilder();
    }

    public UrlExecuteTestBuilder withPost(PostRecord postRecord) {
        this.posts.add(postRecord);
        return this;
    }

    public UrlExecuteTestBuilder withPosts(List<PostRecord> postRecords) {
        this.posts.clear();
        this.posts.addAll(postRecords);
        return this;
    }

    public UrlExecuteTestBuilder withNextUrl(String nextUrl) {
        this.nextUrl = nextUrl;
        return this;
    }

    public URLExecute build() {
        URLExecute urlExecute = new URLExecute();
        urlExecute.setPosts(posts);
        urlExecute.setNextUrl(nextUrl);
        return urlExecute;
    }
}

