package com.ct.fastreport.aws.websocket;

import com.ct.fastreport.dto.ReportResultMessage;
import com.ct.fastreport.repository.ReportWebSocketSubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.apigatewaymanagementapi.ApiGatewayManagementApiClient;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.GoneException;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.PostToConnectionRequest;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiGatewayWebSocketReportNotifierTest {

    @Test
    void deletesGoneConnectionsAndCleansReportSubscriptions() {
        ApiGatewayManagementApiClient apiClient = mock(ApiGatewayManagementApiClient.class);
        ReportWebSocketSubscriptionRepository repository = mock(ReportWebSocketSubscriptionRepository.class);
        when(repository.findConnectionIds(42L)).thenReturn(List.of("gone-connection"));
        doThrow(GoneException.builder().message("gone").build())
                .when(apiClient)
                .postToConnection(any(PostToConnectionRequest.class));

        new ApiGatewayWebSocketReportNotifier(apiClient, repository, new ObjectMapper())
                .notifyResult(new ReportResultMessage(42L, "completed", "content"));

        verify(repository).deleteConnection("gone-connection");
        verify(repository).deleteReportSubscriptions(42L);
    }
}
