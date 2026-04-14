package com.ct.fastreport.messaging.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SqsReportJobPublisherTest {

    @Test
    void publishNewReportSendsExpectedJsonBody() {
        SqsClient sqsClient = mock(SqsClient.class);
        SqsReportJobPublisher publisher = new SqsReportJobPublisher(
                sqsClient,
                new ObjectMapper(),
                "https://sqs.example/report-jobs"
        );

        publisher.publishNewReport(42L);

        var captor = forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());
        assertThat(captor.getValue().queueUrl()).isEqualTo("https://sqs.example/report-jobs");
        assertThat(captor.getValue().messageBody()).contains("\"reportId\":42");
        assertThat(captor.getValue().messageBody()).contains("\"retryCount\":0");
    }
}
