package com.ct.fastreport.service;

import com.ct.fastreport.dto.ReportRequest;
import com.ct.fastreport.model.InternalReportRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ReportRequestMapper {

    public InternalReportRequest fromLegacyRequest(ReportRequest request) {
        if (request == null) {
            return null;
        }

        return new InternalReportRequest(
                "legacy-report-api",
                null,
                new InternalReportRequest.Customer(
                        request.customerId,
                        request.customerName,
                        request.nationalId
                ),
                new InternalReportRequest.Manager(
                        request.managerId,
                        request.managerName
                ),
                new InternalReportRequest.Service(request.serviceCode),
                request.currentPlan,
                defaultList(request.additionalServices),
                mapSpendingHistory(request.spendingLast6),
                mapComplaintHistory(request.complaintHistory),
                new InternalReportRequest.NetworkQualitySnapshot(
                        request.networkQuality,
                        request.networkQuality == null || request.networkQuality.isBlank()
                                ? Map.of()
                                : Map.of("summary", request.networkQuality)
                ),
                request.overrideReason,
                buildSourceMetadata(request)
        );
    }

    private List<String> defaultList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private List<InternalReportRequest.SpendingPoint> mapSpendingHistory(List<Double> spendingLast6) {
        if (spendingLast6 == null || spendingLast6.isEmpty()) {
            return List.of();
        }

        YearMonth start = YearMonth.now().minusMonths(spendingLast6.size() - 1L);
        return java.util.stream.IntStream.range(0, spendingLast6.size())
                .mapToObj(index -> new InternalReportRequest.SpendingPoint(
                        start.plusMonths(index).toString(),
                        spendingLast6.get(index) == null ? null : BigDecimal.valueOf(spendingLast6.get(index))
                ))
                .toList();
    }

    private List<InternalReportRequest.ComplaintRecord> mapComplaintHistory(List<String> complaintHistory) {
        if (complaintHistory == null || complaintHistory.isEmpty()) {
            return List.of();
        }

        return java.util.stream.IntStream.range(0, complaintHistory.size())
                .mapToObj(index -> new InternalReportRequest.ComplaintRecord(
                        null,
                        complaintHistory.get(index)
                ))
                .toList();
    }

    private Map<String, Object> buildSourceMetadata(ReportRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("ingestionMode", "legacy-flat-request");
        metadata.put("fieldAliases", Map.ofEntries(
                Map.entry("customer.customerId", "customerId"),
                Map.entry("customer.customerName", "customerName"),
                Map.entry("customer.nationalId", "nationalId"),
                Map.entry("manager.managerId", "managerId"),
                Map.entry("manager.managerName", "managerName"),
                Map.entry("service.serviceCode", "serviceCode"),
                Map.entry("currentPlan", "currentPlan"),
                Map.entry("additionalServices", "additionalServices"),
                Map.entry("spendingHistory", "spendingLast6"),
                Map.entry("complaintHistory", "complaintHistory"),
                Map.entry("networkQuality.summary", "networkQuality"),
                Map.entry("overrideReason", "overrideReason")
        ));
        metadata.put("rawFieldCount", countNonNullFields(request));
        return Collections.unmodifiableMap(metadata);
    }

    private int countNonNullFields(ReportRequest request) {
        int count = 0;
        if (request.customerId != null) count++;
        if (request.customerName != null) count++;
        if (request.nationalId != null) count++;
        if (request.managerId != null) count++;
        if (request.managerName != null) count++;
        if (request.serviceCode != null) count++;
        if (request.currentPlan != null) count++;
        if (request.additionalServices != null) count++;
        if (request.spendingLast6 != null) count++;
        if (request.complaintHistory != null) count++;
        if (request.networkQuality != null) count++;
        if (request.overrideReason != null) count++;
        return count;
    }
}
