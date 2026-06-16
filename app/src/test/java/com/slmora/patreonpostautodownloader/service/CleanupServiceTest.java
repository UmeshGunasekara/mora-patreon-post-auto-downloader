package com.slmora.patreonpostautodownloader.service;

import com.slmora.patreonpostautodownloader.model.ExcelJob;
import com.slmora.patreonpostautodownloader.support.ExcelJobTestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CleanupServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void GivenExcelFileExists_WhenDeleteExcelFile_ThenFileIsDeleted() throws Exception {
        // Arrange
        Path excelFile = tempDir.resolve("sample.xlsx");
        Files.writeString(excelFile, "content", StandardCharsets.UTF_8);
        ExcelJob job = ExcelJobTestBuilder.anExcelJob().withExcelFile(excelFile).build();
        CleanupService cleanupService = new CleanupService();

        // Act
        cleanupService.deleteExcelFile(job);

        // Assert
        assertThat(Files.exists(excelFile)).isFalse();
    }

    @Test
    void GivenExcelFileIsNull_WhenDeleteExcelFile_ThenMethodExitsWithoutFailure() throws Exception {
        // Arrange
        ExcelJob job = new ExcelJob(77L, null);
        CleanupService cleanupService = new CleanupService();

        // Act
        cleanupService.deleteExcelFile(job);

        // Assert
        assertThat(job.getExcelFile()).isNull();
    }

    @Test
    void GivenValidJob_WhenCleanupAfterSuccess_ThenMethodCompletesWithoutException() {
        // Arrange
        ExcelJob job = ExcelJobTestBuilder.anExcelJob().build();
        CleanupService cleanupService = new CleanupService();

        // Act
        cleanupService.cleanupAfterSuccess(job);

        // Assert
        assertThat(job.getJobId()).isPositive();
    }
}

