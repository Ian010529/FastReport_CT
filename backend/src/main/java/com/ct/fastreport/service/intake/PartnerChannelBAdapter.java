package com.ct.fastreport.service.intake;

import com.ct.fastreport.exception.ValidationError;
import com.ct.fastreport.model.InternalReportRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PartnerChannelBAdapter extends BaseIntakeAdapter<String, PartnerChannelBAdapter.ParsedPayload> {

    private final ObjectMapper objectMapper;

    public PartnerChannelBAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String sourceSystem() {
        return "partner-channel-b";
    }

    @Override
    public ParsedPayload parse(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            throw new ValidationError("PARTNER_B_EMPTY_PAYLOAD", "PartnerChannelB payload must not be blank.");
        }

        try {
            JsonNode root = objectMapper.readTree(rawInput);
            return new ParsedPayload(rawInput, root);
        } catch (Exception ex) {
            throw new ValidationError(
                    "PARTNER_B_INVALID_JSON",
                    "PartnerChannelB payload is not valid JSON.",
                    Map.of("sourceSystem", sourceSystem())
            );
        }
    }

    @Override
    public InternalReportRequest transform(ParsedPayload parsedPayload) {
        JsonNode root = parsedPayload.root();

        List<InternalReportRequest.SpendingPoint> spendingHistory = new ArrayList<>();
        for (JsonNode spendingNode : arrayNode(root, "billing_history")) {
            spendingHistory.add(new InternalReportRequest.SpendingPoint(
                    text(spendingNode, "month"),
                    decimal(spendingNode, "amount")
            ));
        }

        List<InternalReportRequest.ComplaintRecord> complaintHistory = new ArrayList<>();
        for (JsonNode complaintNode : arrayNode(root, "tickets")) {
            complaintHistory.add(new InternalReportRequest.ComplaintRecord(
                    text(complaintNode, "opened_at"),
                    text(complaintNode, "summary")
            ));
        }

        Map<String, String> networkMetrics = new LinkedHashMap<>();
        JsonNode qualityNode = root.path("quality");
        putIfPresent(networkMetrics, "signalRating", text(qualityNode, "signal_rating"));
        putIfPresent(networkMetrics, "outageCount30d", text(qualityNode, "outage_count_30d"));
        putIfPresent(networkMetrics, "latencyTier", text(qualityNode, "latency_tier"));

        return new InternalReportRequest(
                sourceSystem(),
                text(root, "partner_request_id"),
                new InternalReportRequest.Customer(
                        text(root, "subscriber_id"),
                        text(root, "subscriber_name"),
                        text(root, "government_id")
                ),
                new InternalReportRequest.Manager(
                        text(root, "owner_staff_code"),
                        text(root, "owner_staff_name")
                ),
                new InternalReportRequest.Service(
                        text(root, "product_code")
                ),
                text(root, "active_plan"),
                stringList(root, "value_added_services"),
                spendingHistory,
                complaintHistory,
                new InternalReportRequest.NetworkQualitySnapshot(
                        text(qualityNode, "overview"),
                        networkMetrics
                ),
                text(root, "manual_review_reason"),
                buildSourceMetadata(parsedPayload)
        );
    }

    @Override
    public void validate(InternalReportRequest request) {
        List<String> missingFields = new ArrayList<>();

        if (request.customer() == null || isBlank(request.customer().customerId())) {
            missingFields.add("subscriber_id -> customer.customerId");
        }
        if (request.customer() == null || isBlank(request.customer().customerName())) {
            missingFields.add("subscriber_name -> customer.customerName");
        }
        if (request.customer() == null || isBlank(request.customer().nationalId())) {
            missingFields.add("government_id -> customer.nationalId");
        }
        if (request.manager() == null || isBlank(request.manager().managerId())) {
            missingFields.add("owner_staff_code -> manager.managerId");
        }
        if (request.manager() == null || isBlank(request.manager().managerName())) {
            missingFields.add("owner_staff_name -> manager.managerName");
        }
        if (request.service() == null || isBlank(request.service().serviceCode())) {
            missingFields.add("product_code -> service.serviceCode");
        }
        if (isBlank(request.currentPlan())) {
            missingFields.add("active_plan -> currentPlan");
        }
        if (request.networkQuality() == null || isBlank(request.networkQuality().summary())) {
            missingFields.add("quality.overview -> networkQuality.summary");
        }

        if (!missingFields.isEmpty()) {
            throw new ValidationError(
                    "PARTNER_B_REQUIRED_FIELDS_MISSING",
                    "PartnerChannelB payload is missing fields required for InternalReportRequest.",
                    Map.of(
                            "sourceSystem", sourceSystem(),
                            "missingFields", missingFields,
                            "sourceRecordId", request.sourceRecordId()
                    )
            );
        }
    }

    private Map<String, Object> buildSourceMetadata(ParsedPayload parsedPayload) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("sourceSystem", sourceSystem());
        metadata.put("rawPayload", parsedPayload.rawPayload());
        metadata.put("contentType", "application/json");
        metadata.put("fieldMappings", Map.ofEntries(
                Map.entry("sourceRecordId", "partner_request_id"),
                Map.entry("customer.customerId", "subscriber_id"),
                Map.entry("customer.customerName", "subscriber_name"),
                Map.entry("customer.nationalId", "government_id"),
                Map.entry("manager.managerId", "owner_staff_code"),
                Map.entry("manager.managerName", "owner_staff_name"),
                Map.entry("service.serviceCode", "product_code"),
                Map.entry("currentPlan", "active_plan"),
                Map.entry("additionalServices", "value_added_services[]"),
                Map.entry("spendingHistory[].period", "billing_history[].month"),
                Map.entry("spendingHistory[].amount", "billing_history[].amount"),
                Map.entry("complaintHistory[].occurredOn", "tickets[].opened_at"),
                Map.entry("complaintHistory[].description", "tickets[].summary"),
                Map.entry("networkQuality.summary", "quality.overview"),
                Map.entry("networkQuality.metrics.signalRating", "quality.signal_rating"),
                Map.entry("networkQuality.metrics.outageCount30d", "quality.outage_count_30d"),
                Map.entry("networkQuality.metrics.latencyTier", "quality.latency_tier"),
                Map.entry("overrideReason", "manual_review_reason")
        ));
        metadata.put("sourceTrace", Map.of(
                "adapterClass", getClass().getSimpleName(),
                "parsedFormat", "json"
        ));
        return metadata;
    }

    private Iterable<JsonNode> arrayNode(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isArray() ? value : List.of();
    }

    private List<String> stringList(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (!value.isArray()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (JsonNode item : value) {
            if (!item.isNull()) {
                result.add(item.asText());
            }
        }
        return List.copyOf(result);
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }

    private BigDecimal decimal(JsonNode node, String fieldName) {
        String value = text(node, fieldName);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new ValidationError(
                    "PARTNER_B_INVALID_AMOUNT",
                    "PartnerChannelB billing amount must be a valid decimal number.",
                    Map.of("field", fieldName, "value", value)
            );
        }
    }

    private void putIfPresent(Map<String, String> metrics, String key, String value) {
        if (value != null) {
            metrics.put(key, value);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record ParsedPayload(
            String rawPayload,
            JsonNode root
    ) {
    }
}
