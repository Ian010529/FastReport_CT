package com.ct.fastreport.messaging.rabbit;

import com.ct.fastreport.config.RabbitConfig;
import com.ct.fastreport.dto.ReportJobMessage;
import com.ct.fastreport.service.ReportJobProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class RabbitReportJobListener {

    private static final Logger log = LoggerFactory.getLogger(RabbitReportJobListener.class);

    private final ReportJobProcessor reportJobProcessor;
    private final RabbitReportJobPublisher reportJobPublisher;

    public RabbitReportJobListener(ReportJobProcessor reportJobProcessor,
                                   RabbitReportJobPublisher reportJobPublisher) {
        this.reportJobProcessor = reportJobProcessor;
        this.reportJobPublisher = reportJobPublisher;
    }

    @RabbitListener(queues = RabbitConfig.MAIN_QUEUE)
    public void handleReportJob(ReportJobMessage message) {
        Long reportId = message.getReportId();
        int retryCount = message.getRetryCount();
        int attemptNumber = retryCount + 1;
        int totalAttempts = reportJobPublisher.maxRetries() + 1;

        log.info("Rabbit worker picked report id={} attempt={}/{}", reportId, attemptNumber, totalAttempts);

        try {
            reportJobProcessor.process(message);
            log.info("Rabbit worker completed report id={} attempt={}/{}", reportId, attemptNumber, totalAttempts);
        } catch (Exception ex) {
            log.error("Rabbit worker failed report id={} attempt={}/{}", reportId, attemptNumber, totalAttempts, ex);

            boolean scheduled = reportJobPublisher.publishRetry(message);
            if (scheduled) {
                log.warn("Scheduled Rabbit retry for report id={} nextRetryCount={}", reportId, retryCount + 1);
                return;
            }

            reportJobProcessor.markFailed(reportId);
            log.error("Report id={} marked as failed after {} retries", reportId, reportJobPublisher.maxRetries());
        }
    }
}
