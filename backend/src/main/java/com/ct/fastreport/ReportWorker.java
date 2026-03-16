package com.ct.fastreport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ReportWorker {

    private static final Logger log = LoggerFactory.getLogger(ReportWorker.class);

    private final ReportGenerationService reportGenerationService;
    private final ReportJobPublisher reportJobPublisher;

    public ReportWorker(ReportGenerationService reportGenerationService, ReportJobPublisher reportJobPublisher) {
        this.reportGenerationService = reportGenerationService;
        this.reportJobPublisher = reportJobPublisher;
    }

    @RabbitListener(queues = RabbitConfig.MAIN_QUEUE)
    public void handleReportJob(ReportJobMessage message) {
        Long reportId = message.getReportId();
        int retryCount = message.getRetryCount();
        int attemptNumber = retryCount + 1;
        int totalAttempts = reportJobPublisher.maxRetries() + 1;

        log.info("Worker picked report id={} attempt={}/{}", reportId, attemptNumber, totalAttempts);

        try {
            reportGenerationService.generateCarePlan(reportId);
            log.info("Worker completed report id={} attempt={}/{}", reportId, attemptNumber, totalAttempts);
        } catch (Exception ex) {
            log.error("Worker failed report id={} attempt={}/{}", reportId, attemptNumber, totalAttempts, ex);

            boolean scheduled = reportJobPublisher.publishRetry(message);
            if (scheduled) {
                log.warn("Scheduled retry for report id={} nextRetryCount={}", reportId, retryCount + 1);
                return;
            }

            reportGenerationService.markFailed(reportId);
            log.error("Report id={} marked as failed after {} retries", reportId, reportJobPublisher.maxRetries());
        }
    }
}
