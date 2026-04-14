package com.ct.fastreport.aws.websocket;

import com.ct.fastreport.application.port.ReportResultNotifier;
import com.ct.fastreport.dto.ReportResultMessage;
import com.ct.fastreport.repository.ReportWebSocketSubscriptionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.apigatewaymanagementapi.ApiGatewayManagementApiClient;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.GoneException;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.PostToConnectionRequest;

@Component
@Profile("aws")
public class ApiGatewayWebSocketReportNotifier implements ReportResultNotifier {

    private static final Logger log = LoggerFactory.getLogger(ApiGatewayWebSocketReportNotifier.class);

    private final ApiGatewayManagementApiClient apiClient;
    private final ReportWebSocketSubscriptionRepository subscriptionRepository;
    private final ObjectMapper objectMapper;

    public ApiGatewayWebSocketReportNotifier(ApiGatewayManagementApiClient apiClient,
                                             ReportWebSocketSubscriptionRepository subscriptionRepository,
                                             ObjectMapper objectMapper) {
        this.apiClient = apiClient;
        this.subscriptionRepository = subscriptionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void notifyResult(ReportResultMessage message) {
        byte[] payload = toPayload(message);
        for (String connectionId : subscriptionRepository.findConnectionIds(message.getReportId())) {
            try {
                apiClient.postToConnection(PostToConnectionRequest.builder()
                        .connectionId(connectionId)
                        .data(SdkBytes.fromByteArray(payload))
                        .build());
            } catch (GoneException ex) {
                log.info("WebSocket connection {} is gone; deleting subscription", connectionId);
                subscriptionRepository.deleteConnection(connectionId);
            }
        }
        subscriptionRepository.deleteReportSubscriptions(message.getReportId());
    }

    private byte[] toPayload(ReportResultMessage message) {
        try {
            return objectMapper.writeValueAsBytes(message);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize report result message", ex);
        }
    }
}
