package com.ct.fastreport.service;

import com.ct.fastreport.application.port.ReportResultNotifier;
import com.ct.fastreport.dto.ReportJobMessage;
import com.ct.fastreport.dto.ReportResultMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ReportJobProcessor {

    private static final Logger log = LoggerFactory.getLogger(ReportJobProcessor.class);

    private final ReportGenerationService reportGenerationService;
    private final ReportResultNotifier reportResultNotifier;

    public ReportJobProcessor(ReportGenerationService reportGenerationService,
                              ReportResultNotifier reportResultNotifier) {
        this.reportGenerationService = reportGenerationService;
        this.reportResultNotifier = reportResultNotifier;
    }

    public void process(ReportJobMessage message) throws Exception {
        Long reportId = message.getReportId();
        log.info("Processing report job id={} retryCount={}", reportId, message.getRetryCount());
        String reportContent = reportGenerationService.generateCarePlan(reportId);
        reportResultNotifier.notifyResult(new ReportResultMessage(reportId, "completed", reportContent));
        log.info("Completed report job id={}", reportId);
    }

    public void markFailed(Long reportId) {
        reportGenerationService.markFailed(reportId);
        reportResultNotifier.notifyResult(new ReportResultMessage(reportId, "failed", null));
    }
}
