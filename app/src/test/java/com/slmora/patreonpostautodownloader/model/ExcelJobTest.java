package com.slmora.patreonpostautodownloader.model;

import com.slmora.patreonpostautodownloader.support.ExcelJobTestBuilder;
import com.slmora.patreonpostautodownloader.support.ImageRecordTestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelJobTest {

    @Test
    void GivenNewExcelJob_WhenIncrementRetryCountCalled_ThenRetryCountIsIncreasedByOne() {
        // Arrange
        ExcelJob excelJob = ExcelJobTestBuilder.anExcelJob().withRetryCount(2).build();

        // Act
        excelJob.incrementRetryCount();

        // Assert
        assertThat(excelJob.getRetryCount()).isEqualTo(3);
    }

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


