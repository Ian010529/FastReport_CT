package com.ct.fastreport.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.ct.fastreport.dto.ReportJobMessage;
import com.ct.fastreport.service.ReportJobProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.List;

public class ReportJobLambdaHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private static final String APPROXIMATE_RECEIVE_COUNT = "ApproximateReceiveCount";

    private final ReportJobProcessor reportJobProcessor;
    private final ObjectMapper objectMapper;
    private final int maxReceiveCount;

    public ReportJobLambdaHandler() {
        ConfigurableApplicationContext context = SpringLambdaContext.get();
        this.reportJobProcessor = context.getBean(ReportJobProcessor.class);
        this.objectMapper = context.getBean(ObjectMapper.class);
        this.maxReceiveCount = context.getEnvironment()
                .getProperty("aws.sqs.max-receive-count", Integer.class, 5);
    }

    ReportJobLambdaHandler(ReportJobProcessor reportJobProcessor,
                           ObjectMapper objectMapper,
                           int maxReceiveCount) {
        this.reportJobProcessor = reportJobProcessor;
        this.objectMapper = objectMapper;
        this.maxReceiveCount = maxReceiveCount;
    }

    @Override
    public SQSBatchResponse handleRequest(SQSEvent event, Context context) {
        List<SQSBatchResponse.BatchItemFailure> failures = new ArrayList<>();
        if (event == null || event.getRecords() == null) {
            return new SQSBatchResponse(failures);
        }

        for (SQSEvent.SQSMessage record : event.getRecords()) {
            try {
                ReportJobMessage message = objectMapper.readValue(record.getBody(), ReportJobMessage.class);
                reportJobProcessor.process(message);
            } catch (Exception ex) {
                if (isFinalReceive(record)) {
                    if (!markFailed(record, context, ex)) {
                        failures.add(new SQSBatchResponse.BatchItemFailure(record.getMessageId()));
                    }
                } else {
                    failures.add(new SQSBatchResponse.BatchItemFailure(record.getMessageId()));
                }
            }
        }
        return new SQSBatchResponse(failures);
    }

    private boolean isFinalReceive(SQSEvent.SQSMessage record) {
        String rawCount = record.getAttributes() == null ? null : record.getAttributes().get(APPROXIMATE_RECEIVE_COUNT);
        if (rawCount == null || rawCount.isBlank()) {
            return false;
        }
        try {
            return Integer.parseInt(rawCount) >= maxReceiveCount;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private boolean markFailed(SQSEvent.SQSMessage record, Context context, Exception cause) {
        try {
            ReportJobMessage message = objectMapper.readValue(record.getBody(), ReportJobMessage.class);
            reportJobProcessor.markFailed(message.getReportId());
            if (context != null && context.getLogger() != null) {
                context.getLogger().log("Marked report " + message.getReportId()
                        + " failed after final SQS receive: " + cause.getMessage());
            }
            return true;
        } catch (Exception markFailedEx) {
            if (context != null && context.getLogger() != null) {
                context.getLogger().log("Failed to mark final SQS report failure: " + markFailedEx.getMessage());
            }
            return false;
        }
    }
}
