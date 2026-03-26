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
public class AlliancePortalV1Adapter extends BaseIntakeAdapter<String, AlliancePortalV1Adapter.ParsedPayload> {

    private final ObjectMapper objectMapper;

    public AlliancePortalV1Adapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String sourceSystem() {
        return "alliance-portal-v1";
    }

    @Override
    public ParsedPayload parse(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            throw new ValidationError("ALLIANCE_V1_EMPTY_PAYLOAD", "AlliancePortalV1 payload must not be blank.");
        }

        try {
            JsonNode root = objectMapper.readTree(rawInput);
            return new ParsedPayload(rawInput, root);
        } catch (Exception ex) {
            throw new ValidationError(
                    "ALLIANCE_V1_INVALID_JSON",
                    "AlliancePortalV1 payload is not valid JSON.",
                    Map.of("sourceSystem", sourceSystem())
            );
        }
    }

    @Override
    public InternalReportRequest transform(ParsedPayload parsedPayload) {
        JsonNode root = parsedPayload.root();
        JsonNode metaNode = root.path("meta");
        JsonNode accountNode = root.path("account");
        JsonNode ownerNode = accountNode.path("owner");
        JsonNode portfolioNode = root.path("portfolio");
        JsonNode diagnosticsNode = root.path("diagnostics");
        JsonNode flagsNode = root.path("flags");

        List<String> addOns = new ArrayList<>();
        for (JsonNode item : arrayNode(portfolioNode, "addons")) {
            String sku = text(item, "sku");
            if (sku != null) {
                addOns.add(sku);
            }
        }

        List<InternalReportRequest.SpendingPoint> spendingHistory = new ArrayList<>();
        for (JsonNode item : arrayNode(portfolioNode, "arpuTrend")) {
            spendingHistory.add(new InternalReportRequest.SpendingPoint(
                    text(item, "cycle"),
                    decimal(item, "fee")
            ));
        }

        List<InternalReportRequest.ComplaintRecord> complaintHistory = new ArrayList<>();
        for (JsonNode item : arrayNode(diagnosticsNode, "careCases")) {
            complaintHistory.add(new InternalReportRequest.ComplaintRecord(
                    text(item, "openedOn"),
                    joinNonBlank(" | ",
                            text(item, "category"),
                            text(item, "memo"))
            ));
        }

        Map<String, String> networkMetrics = new LinkedHashMap<>();
        putIfPresent(networkMetrics, "downloadMbpsBand", text(diagnosticsNode, "downloadMbpsBand"));
        putIfPresent(networkMetrics, "stabilityIndex", text(diagnosticsNode, "stabilityIndex"));
        putIfPresent(networkMetrics, "coverageTier", text(diagnosticsNode, "coverageTier"));

        return new InternalReportRequest(
                sourceSystem(),
                text(metaNode, "traceNo"),
                new InternalReportRequest.Customer(
                        text(accountNode, "acctNo"),
                        text(accountNode, "displayName"),
                        text(accountNode, "legalCertNo")
                ),
                new InternalReportRequest.Manager(
                        text(ownerNode, "staffNo"),
                        text(ownerNode, "fullName")
                ),
                new InternalReportRequest.Service(
                        text(portfolioNode, "mainOffer")
                ),
                text(portfolioNode, "bundleLabel"),
                List.copyOf(addOns),
                spendingHistory,
                complaintHistory,
                new InternalReportRequest.NetworkQualitySnapshot(
                        text(diagnosticsNode, "networkNarrative"),
                        networkMetrics
                ),
                text(flagsNode, "manualOverrideNote"),
                buildSourceMetadata(parsedPayload)
        );
    }

    @Override
    public void validate(InternalReportRequest request) {
        List<String> missingFields = new ArrayList<>();

        if (request.customer() == null || isBlank(request.customer().customerId())) {
            missingFields.add("account.acctNo -> customer.customerId");
        }
        if (request.customer() == null || isBlank(request.customer().customerName())) {
            missingFields.add("account.displayName -> customer.customerName");
        }
        if (request.customer() == null || isBlank(request.customer().nationalId())) {
            missingFields.add("account.legalCertNo -> customer.nationalId");
        }
        if (request.manager() == null || isBlank(request.manager().managerId())) {
            missingFields.add("account.owner.staffNo -> manager.managerId");
        }
        if (request.manager() == null || isBlank(request.manager().managerName())) {
            missingFields.add("account.owner.fullName -> manager.managerName");
        }
        if (request.service() == null || isBlank(request.service().serviceCode())) {
            missingFields.add("portfolio.mainOffer -> service.serviceCode");
        }
        if (isBlank(request.currentPlan())) {
            missingFields.add("portfolio.bundleLabel -> currentPlan");
        }
        if (request.networkQuality() == null || isBlank(request.networkQuality().summary())) {
            missingFields.add("diagnostics.networkNarrative -> networkQuality.summary");
        }

        if (!missingFields.isEmpty()) {
            throw new ValidationError(
                    "ALLIANCE_V1_REQUIRED_FIELDS_MISSING",
                    "AlliancePortalV1 payload is missing fields required for InternalReportRequest.",
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
                Map.entry("sourceRecordId", "meta.traceNo"),
                Map.entry("customer.customerId", "account.acctNo"),
                Map.entry("customer.customerName", "account.displayName"),
                Map.entry("customer.nationalId", "account.legalCertNo"),
                Map.entry("manager.managerId", "account.owner.staffNo"),
                Map.entry("manager.managerName", "account.owner.fullName"),
                Map.entry("service.serviceCode", "portfolio.mainOffer"),
                Map.entry("currentPlan", "portfolio.bundleLabel"),
                Map.entry("additionalServices", "portfolio.addons[].sku"),
                Map.entry("spendingHistory[].period", "portfolio.arpuTrend[].cycle"),
                Map.entry("spendingHistory[].amount", "portfolio.arpuTrend[].fee"),
                Map.entry("complaintHistory[].occurredOn", "diagnostics.careCases[].openedOn"),
                Map.entry("complaintHistory[].description", "diagnostics.careCases[].category + memo"),
                Map.entry("networkQuality.summary", "diagnostics.networkNarrative"),
                Map.entry("networkQuality.metrics.downloadMbpsBand", "diagnostics.downloadMbpsBand"),
                Map.entry("networkQuality.metrics.stabilityIndex", "diagnostics.stabilityIndex"),
                Map.entry("networkQuality.metrics.coverageTier", "diagnostics.coverageTier"),
                Map.entry("overrideReason", "flags.manualOverrideNote")
        ));
        metadata.put("sourceTrace", Map.of(
                "adapterClass", getClass().getSimpleName(),
                "parsedFormat", "json",
                "schemaVersion", "v1"
        ));
        return metadata;
    }

    private Iterable<JsonNode> arrayNode(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isArray() ? value : List.of();
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
                    "ALLIANCE_V1_INVALID_AMOUNT",
                    "AlliancePortalV1 fee field must be a valid decimal number.",
                    Map.of("field", fieldName, "value", value)
            );
        }
    }

    private String joinNonBlank(String delimiter, String first, String second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first + delimiter + second;
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
