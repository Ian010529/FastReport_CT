package com.ct.fastreport.service;

import com.ct.fastreport.dto.ReportResponse;
import com.ct.fastreport.repository.ReportRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerationService.class);

    private final ReportRepository reportRepository;
    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.base-url}")
    private String baseUrl;

    @Value("${openai.model}")
    private String model;

    public ReportGenerationService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
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

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content",
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
                        "Follow-Up Plan must include Owner, Timeline, and Success Measure as bullets."),
                Map.of("role", "user", "content", prompt)
        ));
        body.put("temperature", 0.4);
        body.put("max_tokens", 2000);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        String url = baseUrl + "/v1/chat/completions";
        log.info("Calling LLM for report {} at {}", report.id, url);

        ResponseEntity<String> response = rest.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(mapper.writeValueAsString(body), headers),
                String.class
        );

        JsonNode root = mapper.readTree(response.getBody());
        return root.at("/choices/0/message/content").asText();
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
