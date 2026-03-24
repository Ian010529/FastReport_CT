package com.ct.fastreport.messaging;

import com.ct.fastreport.config.RabbitConfig;
import com.ct.fastreport.dto.ReportJobMessage;
import com.ct.fastreport.dto.ReportResultMessage;
import com.ct.fastreport.service.ReportGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ReportWorker {

    private static final Logger log = LoggerFactory.getLogger(ReportWorker.class);

    private final ReportGenerationService reportGenerationService;
    private final ReportJobPublisher reportJobPublisher;
    private final ReportResultPublisher reportResultPublisher;

    public ReportWorker(ReportGenerationService reportGenerationService,
                        ReportJobPublisher reportJobPublisher,
                        ReportResultPublisher reportResultPublisher) {
        this.reportGenerationService = reportGenerationService;
        this.reportJobPublisher = reportJobPublisher;
        this.reportResultPublisher = reportResultPublisher;
    }

    @RabbitListener(queues = RabbitConfig.MAIN_QUEUE)
    public void handleReportJob(ReportJobMessage message) {
        Long reportId = message.getReportId();
        int retryCount = message.getRetryCount();
        int attemptNumber = retryCount + 1;
        int totalAttempts = reportJobPublisher.maxRetries() + 1;

        log.info("Worker picked report id={} attempt={}/{}", reportId, attemptNumber, totalAttempts);

        try {
            String reportContent = reportGenerationService.generateCarePlan(reportId);
            reportResultPublisher.publishResult(new ReportResultMessage(reportId, "completed", reportContent));
            log.info("Worker completed report id={} attempt={}/{}", reportId, attemptNumber, totalAttempts);
        } catch (Exception ex) {
            log.error("Worker failed report id={} attempt={}/{}", reportId, attemptNumber, totalAttempts, ex);

            boolean scheduled = reportJobPublisher.publishRetry(message);
            if (scheduled) {
                log.warn("Scheduled retry for report id={} nextRetryCount={}", reportId, retryCount + 1);
                return;
            }

            reportGenerationService.markFailed(reportId);
            reportResultPublisher.publishResult(new ReportResultMessage(reportId, "failed", null));
            log.error("Report id={} marked as failed after {} retries", reportId, reportJobPublisher.maxRetries());
        }
    }
}
