package com.slmora.patreonpostautodownloader.support;

import com.slmora.patreonpostautodownloader.model.ExcelJob;
import com.slmora.patreonpostautodownloader.model.ImageRecord;
import com.slmora.patreonpostautodownloader.model.JobStatus;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ExcelJobTestBuilder {
    private long jobId = 101L;
    private Path excelFile = Path.of("C:/tmp/patreon_posts_output_20260101_20260130_J101.xlsx");
    private Path docxFile = Path.of("C:/tmp/patreon_posts_report_20260101_20260130_J101.docx");
    private JobStatus status = JobStatus.EXCEL_CREATED;
    private int retryCount;
    private String errorMessage;
    private final List<ImageRecord> imageRecords = new ArrayList<>();

    private ExcelJobTestBuilder() {
    }

    public static ExcelJobTestBuilder anExcelJob() {
        return new ExcelJobTestBuilder();
    }

    public ExcelJobTestBuilder withJobId(long jobId) {
        this.jobId = jobId;
        return this;
    }

    public ExcelJobTestBuilder withExcelFile(Path excelFile) {
        this.excelFile = excelFile;
        return this;
    }

    public ExcelJobTestBuilder withDocxFile(Path docxFile) {
        this.docxFile = docxFile;
        return this;
    }

    public ExcelJobTestBuilder withStatus(JobStatus status) {
        this.status = status;
        return this;
    }

    public ExcelJobTestBuilder withRetryCount(int retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    public ExcelJobTestBuilder withErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public ExcelJobTestBuilder withImageRecord(ImageRecord imageRecord) {
        this.imageRecords.add(imageRecord);
        return this;
    }

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

