package com.ct.fastreport.messaging;

import com.ct.fastreport.config.RabbitConfig;
import com.ct.fastreport.dto.ReportResultMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReportResultPublisher {

    private final RabbitTemplate rabbitTemplate;

    public ReportResultPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishResult(ReportResultMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                RabbitConfig.RESULT_ROUTING_KEY,
                message
        );
    }
}
