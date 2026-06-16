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

class AppTest {

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
