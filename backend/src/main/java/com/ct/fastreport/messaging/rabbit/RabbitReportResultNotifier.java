package com.ct.fastreport.messaging.rabbit;

import com.ct.fastreport.application.port.ReportResultNotifier;
import com.ct.fastreport.config.RabbitConfig;
import com.ct.fastreport.dto.ReportResultMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class RabbitReportResultNotifier implements ReportResultNotifier {

    private final RabbitTemplate rabbitTemplate;

    public RabbitReportResultNotifier(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void notifyResult(ReportResultMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                RabbitConfig.RESULT_ROUTING_KEY,
                message
        );
    }
}
