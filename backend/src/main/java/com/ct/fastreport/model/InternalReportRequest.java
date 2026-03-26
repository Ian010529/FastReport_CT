package com.ct.fastreport.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record InternalReportRequest(
        String sourceSystem,
        String sourceRecordId,
        Customer customer,
        Manager manager,
        Service service,
        String currentPlan,
        List<String> additionalServices,
        List<SpendingPoint> spendingHistory,
        List<ComplaintRecord> complaintHistory,
        NetworkQualitySnapshot networkQuality,
        String overrideReason,
        Map<String, Object> sourceMetadata
) {

    public record Customer(
            String customerId,
            String customerName,
            String nationalId
    ) {
    }

    public record Manager(
            String managerId,
            String managerName
    ) {
    }

    public record Service(
            String serviceCode
    ) {
    }

    public record SpendingPoint(
            String period,
            BigDecimal amount
    ) {
    }

    public record ComplaintRecord(
            String occurredOn,
            String description
    ) {
    }

    public record NetworkQualitySnapshot(
            String summary,
            Map<String, String> metrics
    ) {
    }
}
