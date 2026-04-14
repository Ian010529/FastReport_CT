package com.ct.fastreport.service;

import com.ct.fastreport.application.port.ReportResultNotifier;
import com.ct.fastreport.dto.ReportJobMessage;
import com.ct.fastreport.dto.ReportResultMessage;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportJobProcessorTest {

    @Test
    void processNotifiesCompletedResult() throws Exception {
        ReportGenerationService generationService = mock(ReportGenerationService.class);
        ReportResultNotifier notifier = mock(ReportResultNotifier.class);
        when(generationService.generateCarePlan(42L)).thenReturn("generated report");

        new ReportJobProcessor(generationService, notifier).process(new ReportJobMessage(42L, 0));

        verify(notifier).notifyResult(argThat(message ->
                message.getReportId().equals(42L)
                        && "completed".equals(message.getStatus())
                        && "generated report".equals(message.getReportContent())));
    }

    @Test
    void markFailedUpdatesReportAndNotifiesFailedResult() {
        ReportGenerationService generationService = mock(ReportGenerationService.class);
        ReportResultNotifier notifier = mock(ReportResultNotifier.class);

        new ReportJobProcessor(generationService, notifier).markFailed(42L);

        verify(generationService).markFailed(42L);
        verify(notifier).notifyResult(argThat((ReportResultMessage message) ->
                message.getReportId().equals(42L)
                        && "failed".equals(message.getStatus())
                        && message.getReportContent() == null));
    }
}
