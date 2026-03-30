package com.ct.fastreport.service;

import com.ct.fastreport.dto.ReportResponse;
import com.ct.fastreport.repository.ReportRepository;
import com.ct.fastreport.service.llm.LLMGenerationRequest;
import com.ct.fastreport.service.llm.LLMServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ReportGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerationService.class);

    private final ReportRepository reportRepository;
    private final LLMServiceFactory llmServiceFactory;

    public ReportGenerationService(ReportRepository reportRepository,
                                   LLMServiceFactory llmServiceFactory) {
        this.reportRepository = reportRepository;
        this.llmServiceFactory = llmServiceFactory;
    }

    public String generateCarePlan(Long reportId) throws Exception {
        ReportResponse report = reportRepository.findById(reportId);
        if (report == null) {
            throw new IllegalArgumentException("Report not found: " + reportId);
        }

        reportRepository.markProcessing(reportId);
        String content = callLLM(report);
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("LLM returned empty report content for report " + reportId);
        }

        reportRepository.markCompleted(reportId, content);
        return content;
    }

    public void markFailed(Long reportId) {
        reportRepository.markFailed(reportId);
    }

    private String callLLM(ReportResponse report) throws Exception {
        String prompt = buildPrompt(report);
        log.debug("Report prompt length={} for report {}", prompt.length(), report.id);
        return llmServiceFactory.getService().generate(new LLMGenerationRequest(
                "You are a telecom reporting specialist for China Telecom. " +
                        "Generate a professional telecom customer report in English using clean, structured markdown. " +
                        "The output must be easy to scan, operationally useful, and suitable for a business user rather than an engineer. " +
                        "Do not write in Chinese. Do not add preambles or closing remarks. " +
                        "Use this exact structure and exact heading names: " +
                        "# Report Summary " +
                        "## Executive Summary " +
                        "## Customer Profile " +
                        "## Service Assessment " +
                        "## Risk Signals " +
                        "## Recommended Actions " +
                        "## Follow-Up Plan. " +
                        "Each section must contain short, concrete bullet points. " +
                        "Recommended Actions must be a numbered list. " +
                        "Follow-Up Plan must include Owner, Timeline, and Success Measure as bullets.",
                prompt,
                0.4,
                2000
        ));
    }

    private String buildPrompt(ReportResponse report) {
        StringBuilder sb = new StringBuilder();
        sb.append("Create a telecom customer report based on the following data.\n\n");
        sb.append("Report ID: ").append(report.id).append("\n");
        sb.append("Customer ID: ").append(report.customerId).append("\n");
        sb.append("Customer Name: ").append(report.customerName).append("\n");
        sb.append("National ID: ").append(report.nationalId).append("\n");
        sb.append("Manager: ").append(report.managerName).append(" (").append(report.managerId).append(")\n");
        sb.append("Service Code: ").append(report.serviceCode).append("\n");
        sb.append("Current Plan: ").append(report.currentPlan).append("\n");
        if (report.additionalServices != null && !report.additionalServices.isBlank()) {
            sb.append("Additional Services: ").append(report.additionalServices).append("\n");
        }
        if (report.spendingLast6 != null && !report.spendingLast6.isBlank()) {
            sb.append("Spending (Last 6 Months): ").append(report.spendingLast6).append("\n");
        }
        if (report.complaintHistory != null && !report.complaintHistory.isBlank()) {
            sb.append("Complaint History: ").append(report.complaintHistory).append("\n");
        }
        if (report.networkQuality != null && !report.networkQuality.isBlank()) {
            sb.append("Network Quality: ").append(report.networkQuality).append("\n");
        }
        sb.append("\nRequirements:\n");
        sb.append("- Write the report in English.\n");
        sb.append("- Keep it concise, executive-friendly, and easy to scan.\n");
        sb.append("- Use the exact heading structure defined in the system instruction.\n");
        sb.append("- Use bullet lists in every section.\n");
        sb.append("- Make the content specific to the provided customer data.\n");
        sb.append("- Avoid generic filler, marketing language, or internal system terminology.\n");
        sb.append("- Do not mention care plans; this is a report.\n");
        return sb.toString();
    }
}
