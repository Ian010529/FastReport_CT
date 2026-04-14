package com.ct.fastreport.aws.lambda;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.ct.fastreport.dto.ReportJobMessage;
import com.ct.fastreport.service.ReportJobProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReportJobLambdaHandlerTest {

    @Test
    void returnsBatchFailureWhenRecordProcessingFailsBeforeFinalReceive() throws Exception {
        ReportJobProcessor processor = mock(ReportJobProcessor.class);
        ObjectMapper objectMapper = new ObjectMapper();
        doThrow(new RuntimeException("llm failed"))
                .when(processor)
                .process(org.mockito.ArgumentMatchers.any(ReportJobMessage.class));

        ReportJobLambdaHandler handler = new ReportJobLambdaHandler(processor, objectMapper, 5);
        var response = handler.handleRequest(event("message-1", 42L, "2"), null);

        assertThat(response.getBatchItemFailures()).hasSize(1);
        assertThat(response.getBatchItemFailures().get(0).getItemIdentifier()).isEqualTo("message-1");
    }

    @Test
    void marksFailedAndAcknowledgesMessageOnFinalReceive() throws Exception {
        ReportJobProcessor processor = mock(ReportJobProcessor.class);
        ObjectMapper objectMapper = new ObjectMapper();
        doThrow(new RuntimeException("llm failed"))
                .when(processor)
                .process(org.mockito.ArgumentMatchers.any(ReportJobMessage.class));

        ReportJobLambdaHandler handler = new ReportJobLambdaHandler(processor, objectMapper, 5);
        var response = handler.handleRequest(event("message-1", 42L, "5"), null);

        assertThat(response.getBatchItemFailures()).isEmpty();
        verify(processor).markFailed(42L);
    }

    private SQSEvent event(String messageId, Long reportId, String receiveCount) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setMessageId(messageId);
        message.setBody(objectMapper.writeValueAsString(new ReportJobMessage(reportId, 0)));
        message.setAttributes(Map.of("ApproximateReceiveCount", receiveCount));

        SQSEvent event = new SQSEvent();
        event.setRecords(List.of(message));
        return event;
    }
}
