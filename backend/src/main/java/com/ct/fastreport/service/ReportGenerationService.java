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
            throw new IllegalStateException("LLM returned empty care plan for report " + reportId);
        }

        reportRepository.markCompleted(reportId, content);
        return content;
    }

    public void markFailed(Long reportId) {
        reportRepository.markFailed(reportId);
    }

    private String callLLM(ReportResponse report) throws Exception {
        String prompt = buildPrompt(report);
        log.debug("Care plan prompt length={} for report {}", prompt.length(), report.id);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content",
                        "You are a China Telecom customer retention and service optimization expert. " +
                        "Generate a practical care plan in Chinese with markdown headings. " +
                        "The care plan must include: 1. 客户概况 2. 关键问题 3. 挽留/关怀动作 4. 跟进建议 5. 风险提示."),
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
        sb.append("请基于以下客户资料，生成一个可执行的 Care Plan / 客户关怀方案。\n\n");
        sb.append("报告ID: ").append(report.id).append("\n");
        sb.append("客户编号: ").append(report.customerId).append("\n");
        sb.append("客户姓名: ").append(report.customerName).append("\n");
        sb.append("身份证号: ").append(report.nationalId).append("\n");
        sb.append("客户经理: ").append(report.managerName).append(" (").append(report.managerId).append(")\n");
        sb.append("业务编码: ").append(report.serviceCode).append("\n");
        sb.append("当前套餐: ").append(report.currentPlan).append("\n");
        if (report.additionalServices != null && !report.additionalServices.isBlank()) {
            sb.append("附加服务: ").append(report.additionalServices).append("\n");
        }
        if (report.spendingLast6 != null && !report.spendingLast6.isBlank()) {
            sb.append("近6月消费: ").append(report.spendingLast6).append("\n");
        }
        if (report.complaintHistory != null && !report.complaintHistory.isBlank()) {
            sb.append("投诉记录: ").append(report.complaintHistory).append("\n");
        }
        if (report.networkQuality != null && !report.networkQuality.isBlank()) {
            sb.append("网络质量: ").append(report.networkQuality).append("\n");
        }
        sb.append("\n请给出具体、可执行、适合客户经理落地的关怀方案。");
        return sb.toString();
    }
}
