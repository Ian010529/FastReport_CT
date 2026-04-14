package com.ct.fastreport.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.ct.fastreport.repository.ReportWebSocketSubscriptionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class WebSocketConnectionLambdaHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private static final TypeReference<Map<String, Object>> BODY_TYPE = new TypeReference<>() {
    };

    private final ReportWebSocketSubscriptionRepository subscriptionRepository;
    private final ObjectMapper objectMapper;
    private final Duration subscriptionTtl;

    public WebSocketConnectionLambdaHandler() {
        ConfigurableApplicationContext context = SpringLambdaContext.get();
        this.subscriptionRepository = context.getBean(ReportWebSocketSubscriptionRepository.class);
        this.objectMapper = context.getBean(ObjectMapper.class);
        this.subscriptionTtl = Duration.ofHours(context.getEnvironment()
                .getProperty("aws.websocket.subscription-ttl-hours", Long.class, 2L));
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        Map<String, Object> requestContext = objectMap(event.get("requestContext"));
        String routeKey = stringValue(requestContext.get("routeKey"));
        String connectionId = stringValue(requestContext.get("connectionId"));

        if ("$connect".equals(routeKey)) {
            return response(200, "connected");
        }
        if ("$disconnect".equals(routeKey)) {
            if (connectionId != null) {
                subscriptionRepository.deleteConnection(connectionId);
            }
            return response(200, "disconnected");
        }
        if ("subscribeReport".equals(routeKey)) {
            return subscribe(event, connectionId);
        }
        return response(400, "unsupported route");
    }

    private Map<String, Object> subscribe(Map<String, Object> event, String connectionId) {
        if (connectionId == null || connectionId.isBlank()) {
            return response(400, "missing connectionId");
        }

        try {
            Map<String, Object> body = objectMapper.readValue(stringValue(event.get("body")), BODY_TYPE);
            Long reportId = longValue(body.get("reportId"));
            if (reportId == null) {
                return response(400, "missing reportId");
            }
            subscriptionRepository.subscribe(reportId, connectionId, Instant.now().plus(subscriptionTtl));
            subscriptionRepository.deleteExpired();
            return response(200, "subscribed");
        } catch (Exception ex) {
            return response(400, "invalid subscribeReport payload");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> objectMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text);
        }
        return null;
    }

    private Map<String, Object> response(int statusCode, String body) {
        return Map.of("statusCode", statusCode, "body", body);
    }
}
