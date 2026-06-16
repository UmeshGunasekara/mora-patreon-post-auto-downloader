package com.slmora.patreonpostautodownloader.app;

import com.slmora.patreonpostautodownloader.config.PipelineConfig;
import com.slmora.patreonpostautodownloader.controller.PatreonPostDownloadPipelineController;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.io.IOException;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * The {@code AppTest} test class is created for verifying the application
 * launcher behavior implemented by {@link App}.
 * <p>
 * It focuses on the entry-point contract: configuration logging access,
 * controller construction, pipeline execution delegation, and internal handling
 * of controller {@link IOException} failures.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Verifies that {@link App#main(String[])} constructs and executes the pipeline controller.</li>
 *     <li>Confirms {@link IOException} from controller execution is handled inside the launcher.</li>
 *     <li>Checks that null command-line arguments do not prevent pipeline execution.</li>
 *     <li>Uses Mockito static and construction mocks to avoid starting the real pipeline.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link App}<br>
 * 2 - {@link PipelineConfig}<br>
 * 3 - {@link PatreonPostDownloadPipelineController}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link AppTest#GivenValidPipelineController_WhenMainInvoked_ThenPipelineExecuteIsCalled()}</li>
 *     <li>{@link AppTest#GivenControllerThrowsIOException_WhenMainInvoked_ThenExceptionIsHandledInternally()}</li>
 *     <li>{@link AppTest#GivenNullArgs_WhenMainInvoked_ThenPipelineStillExecutes()}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>Tests mock {@link PipelineConfig#getToString()} so environment-backed configuration is not required.</li>
 *     <li>Tests mock {@link PatreonPostDownloadPipelineController} construction so no Excel, image, DOCX, or network work starts.</li>
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
class AppTest {

    /**
     * <h3>Execute pipeline from launcher</h3>
     * Verifies that {@link App#main(String[])} creates a
     * {@link PatreonPostDownloadPipelineController} and invokes
     * {@link PatreonPostDownloadPipelineController#execute()} once when startup
     * dependencies behave normally.
     * <p>
     * This test targets the successful launcher delegation path without running
     * the real producer-worker pipeline.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Mocks {@link PipelineConfig#getToString()} for deterministic startup logging.</li>
     *     <li>Mocks controller construction performed inside {@link App#main(String[])}.</li>
     *     <li>Invokes the launcher with an empty argument array.</li>
     *     <li>Asserts that the constructed controller executes exactly once.</li>
     * </ul>
     *
     * @throws IOException when the mocked controller execution contract reports an I/O failure
     * @since 1.0
     *
     * @see App#main(String[])
     */
    @Test
    void GivenValidPipelineController_WhenMainInvoked_ThenPipelineExecuteIsCalled() throws IOException {
        try (MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class);
             MockedConstruction<PatreonPostDownloadPipelineController> construction =
                     org.mockito.Mockito.mockConstruction(PatreonPostDownloadPipelineController.class)) {
            pipelineConfigMock.when(PipelineConfig::getToString).thenReturn("mocked-config");

            App.main(new String[0]);

            PatreonPostDownloadPipelineController controller = construction.constructed().getFirst();
            verify(controller, times(1)).execute();
        }
    }

    /**
     * <h3>Handle controller I/O failure</h3>
     * Verifies that {@link App#main(String[])} catches an {@link IOException}
     * thrown by the pipeline controller.
     * <p>
     * This test targets the launcher failure path where controller execution
     * fails after construction, confirming the exception is handled internally
     * and execution still reaches the mocked controller call.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Mocks configuration string resolution to avoid environment access.</li>
     *     <li>Constructs a mocked controller whose {@code execute()} method throws {@link IOException}.</li>
     *     <li>Invokes the launcher and expects no exception to escape the test method.</li>
     *     <li>Verifies that controller execution was attempted exactly once.</li>
     * </ul>
     *
     * @throws IOException when Mockito setup for the checked exception contract fails
     * @since 1.0
     *
     * @see App#main(String[])
     */
    @Test
    void GivenControllerThrowsIOException_WhenMainInvoked_ThenExceptionIsHandledInternally() throws IOException {
        try (MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class);
             MockedConstruction<PatreonPostDownloadPipelineController> construction =
                     org.mockito.Mockito.mockConstruction(PatreonPostDownloadPipelineController.class,
                             (mock, context) -> doThrow(new IOException("disk error")).when(mock).execute())) {
            pipelineConfigMock.when(PipelineConfig::getToString).thenReturn("mocked-config");

            App.main(new String[0]);

            PatreonPostDownloadPipelineController controller = construction.constructed().getFirst();
            verify(controller, times(1)).execute();
        }
    }

    /**
     * <h3>Execute pipeline with null arguments</h3>
     * Verifies that {@link App#main(String[])} does not depend on command-line
     * arguments and still executes the pipeline when {@code args} is null.
     * <p>
     * This test targets the launcher input boundary and confirms current
     * startup behavior is fully configuration-driven.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Mocks configuration string resolution for deterministic startup behavior.</li>
     *     <li>Mocks controller construction so no real pipeline is started.</li>
     *     <li>Invokes {@link App#main(String[])} with {@code null} arguments.</li>
     *     <li>Verifies that the constructed controller executes exactly once.</li>
     * </ul>
     *
     * @throws IOException when the mocked controller execution contract reports an I/O failure
     * @since 1.0
     *
     * @see App#main(String[])
     */
    @Test
    void GivenNullArgs_WhenMainInvoked_ThenPipelineStillExecutes() throws IOException {
        try (MockedStatic<PipelineConfig> pipelineConfigMock = mockStatic(PipelineConfig.class);
             MockedConstruction<PatreonPostDownloadPipelineController> construction =
                     org.mockito.Mockito.mockConstruction(PatreonPostDownloadPipelineController.class)) {
            pipelineConfigMock.when(PipelineConfig::getToString).thenReturn("mocked-config");

            App.main(null);

            PatreonPostDownloadPipelineController controller = construction.constructed().getFirst();
            verify(controller, times(1)).execute();
        }
    }
}
