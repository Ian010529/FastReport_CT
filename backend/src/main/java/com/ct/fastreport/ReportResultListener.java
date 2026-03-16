package com.ct.fastreport;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ReportResultListener {

    private final ReportSseService reportSseService;

    public ReportResultListener(ReportSseService reportSseService) {
        this.reportSseService = reportSseService;
    }

    @RabbitListener(queues = RabbitConfig.RESULT_QUEUE)
    public void handleResult(ReportResultMessage message) {
        reportSseService.publishResult(message);
    }
}
