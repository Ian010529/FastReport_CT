package com.ct.fastreport;

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
