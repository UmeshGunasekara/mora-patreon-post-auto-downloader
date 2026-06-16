package com.slmora.patreonpostautodownloader.controller;

import com.slmora.patreonpostautodownloader.config.PipelineConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class PatreonPostDownloadPipelineControllerTest {

    @TempDir
    Path tempDir;

    @Test
    void GivenValidPipelineConfig_WhenExecute_ThenPipelineIsConstructedAndStarted() throws Exception {
        Path urlInputPath = tempDir.resolve("seed.txt");
        Path excelOut = tempDir.resolve("excel");
        Path imageOut = tempDir.resolve("image");
        Path docxOut = tempDir.resolve("docx");
        Path failedOut = tempDir.resolve("failed");
        Files.writeString(urlInputPath, "\n", StandardCharsets.UTF_8);

        try (MockedStatic<PipelineConfig> configMock = mockStatic(PipelineConfig.class)) {

            configMock.when(PipelineConfig::getToString).thenReturn("cfg");
            configMock.when(PipelineConfig::getUrlInputPath).thenReturn(urlInputPath);
            configMock.when(PipelineConfig::getExcelOutputDirPath).thenReturn(excelOut);
            configMock.when(PipelineConfig::getImageOutputDirPath).thenReturn(imageOut);
            configMock.when(PipelineConfig::getDocxOutputDirPath).thenReturn(docxOut);
            configMock.when(PipelineConfig::getFailedOutputDirPath).thenReturn(failedOut);
            configMock.when(PipelineConfig::getExcelQueueCapacity).thenReturn(10);
            configMock.when(PipelineConfig::getDocxQueueCapacity).thenReturn(10);
            configMock.when(PipelineConfig::getRetryQueueCapacity).thenReturn(10);
            configMock.when(PipelineConfig::getFailedQueueCapacity).thenReturn(10);
            configMock.when(PipelineConfig::getProcessExcelThreads).thenReturn(1);
            configMock.when(PipelineConfig::getProcessImageDownloadThreads).thenReturn(1);
            configMock.when(PipelineConfig::getProcessDocxThreads).thenReturn(1);
            configMock.when(PipelineConfig::getMaxRetry).thenReturn(1);
            configMock.when(PipelineConfig::getPatreonAccessCookie).thenReturn("session=abc");
            configMock.when(PipelineConfig::getExcelPostSheetName).thenReturn("Posts");
            configMock.when(PipelineConfig::getExcelPostFileName).thenReturn("patreon_posts_output_temp.xlsx");
            configMock.when(PipelineConfig::getDocxPostFileNamePattern).thenReturn("patreon_posts_output(.*)\\.xlsx");
            configMock.when(PipelineConfig::getDocxPostFileName).thenReturn("patreon_posts_report_temp.docx");

            PatreonPostDownloadPipelineController controller = new PatreonPostDownloadPipelineController();

            controller.execute();

            assertThat(Files.exists(excelOut)).isTrue();
            assertThat(Files.exists(imageOut)).isTrue();
            assertThat(Files.exists(docxOut)).isTrue();
            assertThat(Files.exists(failedOut)).isTrue();
        }
    }
}


