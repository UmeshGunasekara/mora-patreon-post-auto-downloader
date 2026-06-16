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

/**
 * The {@code PatreonPostDownloadPipelineControllerTest} test class is created
 * for verifying the runtime composition behavior implemented by
 * {@link PatreonPostDownloadPipelineController}.
 * <p>
 * It focuses on controller startup with mocked {@link PipelineConfig} values,
 * temporary output directories, queue/service/process construction, and the
 * directory creation side effects required before the pipeline stages run.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Verifies that configured Excel, image, DOCX, and failed-output directories are created.</li>
 *     <li>Uses a temporary seed URL file so the test does not depend on local workspace paths.</li>
 *     <li>Mocks static configuration access to isolate the controller from environment-backed properties.</li>
 *     <li>Exercises the controller's real {@link PatreonPostDownloadPipelineController#execute()} wiring path.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link PatreonPostDownloadPipelineController}<br>
 * 2 - {@link PipelineConfig}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link PatreonPostDownloadPipelineControllerTest#GivenValidPipelineConfig_WhenExecute_ThenPipelineIsConstructedAndStarted()}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>The test writes a blank URL seed file, so no live Patreon API call is required for this scenario.</li>
 *     <li>All file-system effects are contained under JUnit's {@link TempDir} directory.</li>
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
class PatreonPostDownloadPipelineControllerTest {

    /**
     * Temporary directory used to isolate configuration files and output
     * directories created by the controller during this test.
     */
    @TempDir
    Path tempDir;

    /**
     * <h3>Construct and start pipeline from valid configuration</h3>
     * Verifies that {@link PatreonPostDownloadPipelineController#execute()}
     * accepts a complete mocked configuration, starts the pipeline composition
     * path, and creates every configured output directory.
     * <p>
     * This test targets the controller wiring boundary without using real
     * application paths or Patreon credentials.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates a temporary seed URL file and output directory paths.</li>
     *     <li>Mocks all {@link PipelineConfig} values required by controller and downstream constructor setup.</li>
     *     <li>Invokes {@link PatreonPostDownloadPipelineController#execute()}.</li>
     *     <li>Asserts that Excel, image, DOCX, and failed-output directories exist after execution.</li>
     * </ul>
     *
     * @throws Exception when temporary file setup or controller execution fails
     * @since 1.0
     *
     * @see PatreonPostDownloadPipelineController#execute()
     */
    @Test
    void GivenValidPipelineConfig_WhenExecute_ThenPipelineIsConstructedAndStarted() throws Exception {
        Path urlInputPath = tempDir.resolve("seed.txt");
        Path excelOut = tempDir.resolve("excel");
        Path imageOut = tempDir.resolve("image");
        Path docxOut = tempDir.resolve("docx");
        Path failedOut = tempDir.resolve("failed");
        // A blank seed file allows the pipeline to initialize and exit without network access.
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


