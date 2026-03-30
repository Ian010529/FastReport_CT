package com.ct.fastreport.messaging;

import com.ct.fastreport.config.RabbitConfig;
import com.ct.fastreport.dto.ReportJobMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReportJobPublisher {

    private static final int MAX_RETRIES = 3;

    private final RabbitTemplate rabbitTemplate;

    public ReportJobPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishNewReport(Long reportId) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                RabbitConfig.MAIN_ROUTING_KEY,
                new ReportJobMessage(reportId, 0)
        );
    }

    public boolean publishRetry(ReportJobMessage failedMessage) {
        int nextRetryCount = failedMessage.getRetryCount() + 1;
        if (nextRetryCount > MAX_RETRIES) {
            return false;
        }

        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                retryRoutingKey(nextRetryCount),
                new ReportJobMessage(failedMessage.getReportId(), nextRetryCount)
        );
        return true;
    }

    public int maxRetries() {
        return MAX_RETRIES;
    }

    private String retryRoutingKey(int retryCount) {
        return switch (retryCount) {
            case 1 -> RabbitConfig.RETRY_ROUTING_KEY_1;
            case 2 -> RabbitConfig.RETRY_ROUTING_KEY_2;
            case 3 -> RabbitConfig.RETRY_ROUTING_KEY_3;
            default -> throw new IllegalArgumentException("Unsupported retry count: " + retryCount);
        };
    }
}
