package com.ct.fastreport.messaging.sqs;

import com.ct.fastreport.application.port.ReportJobPublisher;
import com.ct.fastreport.dto.ReportJobMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Component
@Profile("aws")
public class SqsReportJobPublisher implements ReportJobPublisher {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String queueUrl;

    public SqsReportJobPublisher(SqsClient sqsClient,
                                 ObjectMapper objectMapper,
                                 @Value("${aws.sqs.report-jobs-queue-url:${REPORT_JOBS_QUEUE_URL:}}") String queueUrl) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.queueUrl = queueUrl;
    }

    @Override
    public void publishNewReport(Long reportId) {
        if (queueUrl == null || queueUrl.isBlank()) {
            throw new IllegalStateException("REPORT_JOBS_QUEUE_URL is required when SPRING_PROFILES_ACTIVE=aws");
        }
        try {
            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(objectMapper.writeValueAsString(new ReportJobMessage(reportId, 0)))
                    .build());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize report job message", ex);
        }
    }
}
