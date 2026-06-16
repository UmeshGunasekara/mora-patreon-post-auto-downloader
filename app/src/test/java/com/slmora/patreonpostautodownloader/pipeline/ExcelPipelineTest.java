package com.slmora.patreonpostautodownloader.pipeline;

import com.slmora.patreonpostautodownloader.process.FailedJobMonitor;
import com.slmora.patreonpostautodownloader.process.ProcessDocxProducer;
import com.slmora.patreonpostautodownloader.process.ProcessExcelProducer;
import com.slmora.patreonpostautodownloader.process.ProcessImageDownloadWorker;
import com.slmora.patreonpostautodownloader.process.ProcessRetry;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ExcelPipelineTest {

    @Test
    void GivenAllStagesConfigured_WhenStartCalled_ThenEveryStageIsSubmittedForExecution() {
        // Arrange
        ProcessExcelProducer excelProducer = mock(ProcessExcelProducer.class);
        ProcessImageDownloadWorker imageDownloadWorker = mock(ProcessImageDownloadWorker.class);
        ProcessRetry retryProcess = mock(ProcessRetry.class);
        ProcessDocxProducer docxProducer = mock(ProcessDocxProducer.class);
        FailedJobMonitor failedJobMonitor = mock(FailedJobMonitor.class);

        ExcelPipeline pipeline = new ExcelPipeline(
                excelProducer,
                imageDownloadWorker,
                retryProcess,
                docxProducer,
                failedJobMonitor
        );

        // Act
        pipeline.start();

        // Assert
        verify(excelProducer, timeout(2000).times(1)).start();
        verify(imageDownloadWorker, timeout(2000).times(1)).start();
        verify(retryProcess, timeout(2000).times(1)).start();
        verify(docxProducer, timeout(2000).times(1)).start();
        verify(failedJobMonitor, timeout(2000).times(1)).start();
    }
}

