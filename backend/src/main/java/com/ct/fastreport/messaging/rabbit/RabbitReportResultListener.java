package com.ct.fastreport.messaging.rabbit;

import com.ct.fastreport.config.RabbitConfig;
import com.ct.fastreport.dto.ReportResultMessage;
import com.ct.fastreport.service.ReportSseService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class RabbitReportResultListener {

    private final ReportSseService reportSseService;

    public RabbitReportResultListener(ReportSseService reportSseService) {
        this.reportSseService = reportSseService;
    }

    @RabbitListener(queues = RabbitConfig.RESULT_QUEUE)
    public void handleResult(ReportResultMessage message) {
        reportSseService.publishResult(message);
    }
}
