package com.ct.fastreport;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerationService.class);

    private final JdbcTemplate db;
    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.base-url}")
    private String baseUrl;

    @Value("${openai.model}")
    private String model;

    public ReportGenerationService(JdbcTemplate db) {
        this.db = db;
    }

    public String generateCarePlan(Long reportId) throws Exception {
        ReportPromptData data = loadPromptData(reportId);
        if (data == null) {
            throw new IllegalArgumentException("Report not found: " + reportId);
        }

        markProcessing(reportId);
        String content = callLLM(data);
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("LLM returned empty care plan for report " + reportId);
        }

        db.update(
                "UPDATE reports SET status = ?, report_content = ?, updated_at = NOW() WHERE id = ?",
                "completed",
                content,
                reportId
        );
        return content;
    }

    public void markFailed(Long reportId) {
        db.update(
                "UPDATE reports SET status = ?, updated_at = NOW() WHERE id = ?",
                "failed",
                reportId
        );
    }

    private void markProcessing(Long reportId) {
        db.update(
                "UPDATE reports SET status = ?, updated_at = NOW() WHERE id = ?",
                "processing",
                reportId
        );
    }

    private ReportPromptData loadPromptData(Long reportId) {
        List<ReportPromptData> rows = db.query(
                "SELECT " +
                        "r.id, " +
                        "c.customer_id, " +
                        "c.customer_name, " +
                        "c.national_id, " +
                        "m.manager_name, " +
                        "m.manager_id, " +
                        "r.service_code, " +
                        "r.current_plan, " +
                        "COALESCE(r.additional_services, '') AS additional_services, " +
                        "COALESCE((SELECT '[' || string_agg(sh.amount::text, ',' ORDER BY sh.month) || ']' " +
                        "          FROM spending_history sh WHERE sh.report_id = r.id), '[]') AS spending_last6, " +
                        "COALESCE((SELECT string_agg(cp.description, '||' ORDER BY cp.complaint_date, cp.id) " +
                        "          FROM complaints cp WHERE cp.report_id = r.id), '') AS complaint_history, " +
                        "COALESCE((SELECT string_agg(nq.metric || ':' || nq.value, '||' ORDER BY nq.id) " +
                        "          FROM network_quality nq WHERE nq.report_id = r.id), '') AS network_quality " +
                        "FROM reports r " +
                        "JOIN customers c ON r.customer_id = c.id " +
                        "JOIN managers m ON r.manager_id = m.id " +
                        "WHERE r.id = ?",
                (rs, rowNum) -> {
                    ReportPromptData data = new ReportPromptData();
                    data.reportId = rs.getLong("id");
                    data.customerId = rs.getString("customer_id");
                    data.customerName = rs.getString("customer_name");
                    data.nationalId = rs.getString("national_id");
                    data.managerName = rs.getString("manager_name");
                    data.managerId = rs.getString("manager_id");
                    data.serviceCode = rs.getString("service_code");
                    data.currentPlan = rs.getString("current_plan");
                    data.additionalServices = rs.getString("additional_services");
                    data.spendingLast6 = rs.getString("spending_last6");
                    data.complaintHistory = rs.getString("complaint_history");
                    data.networkQuality = rs.getString("network_quality");
                    return data;
                },
                reportId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String callLLM(ReportPromptData data) throws Exception {
        String prompt = buildPrompt(data);
        log.debug("Care plan prompt length={} for report {}", prompt.length(), data.reportId);

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
        log.info("Calling LLM for report {} at {}", data.reportId, url);

        ResponseEntity<String> response = rest.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(mapper.writeValueAsString(body), headers),
                String.class
        );

        JsonNode root = mapper.readTree(response.getBody());
        return root.at("/choices/0/message/content").asText();
    }

    private String buildPrompt(ReportPromptData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("请基于以下客户资料，生成一个可执行的 Care Plan / 客户关怀方案。\n\n");
        sb.append("报告ID: ").append(data.reportId).append("\n");
        sb.append("客户编号: ").append(data.customerId).append("\n");
        sb.append("客户姓名: ").append(data.customerName).append("\n");
        sb.append("身份证号: ").append(data.nationalId).append("\n");
        sb.append("客户经理: ").append(data.managerName).append(" (").append(data.managerId).append(")\n");
        sb.append("业务编码: ").append(data.serviceCode).append("\n");
        sb.append("当前套餐: ").append(data.currentPlan).append("\n");
        if (data.additionalServices != null && !data.additionalServices.isBlank()) {
            sb.append("附加服务: ").append(data.additionalServices).append("\n");
        }
        if (data.spendingLast6 != null && !data.spendingLast6.isBlank()) {
            sb.append("近6月消费: ").append(data.spendingLast6).append("\n");
        }
        if (data.complaintHistory != null && !data.complaintHistory.isBlank()) {
            sb.append("投诉记录: ").append(data.complaintHistory).append("\n");
        }
        if (data.networkQuality != null && !data.networkQuality.isBlank()) {
            sb.append("网络质量: ").append(data.networkQuality).append("\n");
        }
        sb.append("\n请给出具体、可执行、适合客户经理落地的关怀方案。");
        return sb.toString();
    }

    private static class ReportPromptData {
        private Long reportId;
        private String customerId;
        private String customerName;
        private String nationalId;
        private String managerName;
        private String managerId;
        private String serviceCode;
        private String currentPlan;
        private String additionalServices;
        private String spendingLast6;
        private String complaintHistory;
        private String networkQuality;
    }
}
