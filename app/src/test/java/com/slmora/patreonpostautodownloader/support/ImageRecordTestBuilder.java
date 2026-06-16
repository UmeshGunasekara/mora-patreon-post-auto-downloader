package com.slmora.patreonpostautodownloader.support;

import com.slmora.patreonpostautodownloader.model.DownloadStatus;
import com.slmora.patreonpostautodownloader.model.ImageRecord;

import java.nio.file.Path;

public final class ImageRecordTestBuilder {
    private int rowNumber = 1;
    private String imageUrl = "https://cdn.example.com/image-1.jpg";
    private String imageName = "image-1";
    private Path downloadedImagePath;
    private DownloadStatus downloadStatus = DownloadStatus.PENDING;
    private String errorMessage;

    private ImageRecordTestBuilder() {
    }

    public static ImageRecordTestBuilder anImageRecord() {
        return new ImageRecordTestBuilder();
    }

    public ImageRecordTestBuilder withRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
        return this;
    }

    public ImageRecordTestBuilder withImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        return this;
    }

    public ImageRecordTestBuilder withImageName(String imageName) {
        this.imageName = imageName;
        return this;
    }

    public ImageRecordTestBuilder withDownloadedImagePath(Path downloadedImagePath) {
        this.downloadedImagePath = downloadedImagePath;
        return this;
    }

    public ImageRecordTestBuilder withDownloadStatus(DownloadStatus downloadStatus) {
        this.downloadStatus = downloadStatus;
        return this;
    }

    public ImageRecordTestBuilder withErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public ImageRecord build() {
        ImageRecord imageRecord = new ImageRecord(rowNumber, imageUrl, imageName);
        imageRecord.setDownloadedImagePath(downloadedImagePath);
        imageRecord.setDownloadStatus(downloadStatus);
        imageRecord.setErrorMessage(errorMessage);
        return imageRecord;
    }
}

