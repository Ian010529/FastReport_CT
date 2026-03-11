package com.ct.fastreport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final JdbcTemplate db;
    private final StringRedisTemplate redis;
    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String QUEUE_KEY = "report:queue";

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.base-url}")
    private String baseUrl;

    @Value("${openai.model}")
    private String model;

    public ReportController(JdbcTemplate db, StringRedisTemplate redis) {
        this.db = db;
        this.redis = redis;
    }

    // ────────── DTO (inner classes, keep it flat) ──────────

    public static class ReportRequest {
        public String customerId;
        public String customerName;
        public String nationalId;
        public String managerName;
        public String managerId;
        public String serviceCode;
        public String currentPlan;
        public List<String> additionalServices;
        public List<Double> spendingLast6;
        public List<String> complaintHistory;
        public String networkQuality;
    }

    public static class ReportResponse {
        public Long id;
        public String customerId;
        public String customerName;
        public String nationalId;
        public String managerName;
        public String managerId;
        public String serviceCode;
        public String currentPlan;
        public String additionalServices;
        public String spendingLast6;
        public String complaintHistory;
        public String networkQuality;
        public String status;
        public String reportContent;
        public String createdAt;
        public String updatedAt;
    }

    // ────────── POST /api/reports  →  create + enqueue (async) ──────────

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody ReportRequest req) {
        log.info("Creating report for customer {}", req.customerId);

        String additionalSvc = req.additionalServices != null ? String.join(",", req.additionalServices) : null;

        Long customerDbId = upsertCustomer(req);
        Long managerDbId = upsertManager(req);

        // 1. INSERT  status = pending
        KeyHolder keyHolder = new GeneratedKeyHolder();
        db.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO reports (customer_id, manager_id, service_code, current_plan, additional_services, status) " +
                "VALUES (?,?,?,?,?,?)",
                new String[]{"id"}
            );
            ps.setLong(1, customerDbId);
            ps.setLong(2, managerDbId);
            ps.setString(3, req.serviceCode);
            ps.setString(4, req.currentPlan);
            ps.setString(5, additionalSvc);
            ps.setString(6, "pending");
            return ps;
        }, keyHolder);

        long id = Objects.requireNonNull(keyHolder.getKey(), "insert report id missing").longValue();
        log.info("Inserted report id={}", id);

        insertSpendingHistory(id, req.spendingLast6);
        insertComplaints(id, req.complaintHistory);
        insertNetworkQuality(id, req.networkQuality);

        // 2. Push careplan_id to Redis queue
        redis.opsForList().leftPush(QUEUE_KEY, String.valueOf(id));
        log.info("Enqueued report id={} to Redis queue '{}'", id, QUEUE_KEY);

        // 3. Return immediately
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id);
        body.put("status", "pending");
        body.put("message", "已收到，报告将在后台生成");
        return ResponseEntity.accepted().body(body);
    }

    // ────────── GET /api/reports?search=xxx ──────────

    @GetMapping
    public List<ReportResponse> list(@RequestParam(required = false) String search) {
        String baseSql = "SELECT " +
                "r.id, " +
                "c.customer_id AS customer_id, " +
                "c.customer_name AS customer_name, " +
                "c.national_id AS national_id, " +
                "m.manager_name AS manager_name, " +
                "m.manager_id AS manager_id, " +
                "r.service_code, " +
                "r.current_plan, " +
                "r.additional_services, " +
                "COALESCE((SELECT '[' || string_agg(sh.amount::text, ',' ORDER BY sh.month) || ']' " +
                "          FROM spending_history sh WHERE sh.report_id = r.id), '[]') AS spending_last6, " +
                "COALESCE((SELECT string_agg(cp.description, '||' ORDER BY cp.complaint_date, cp.id) " +
                "          FROM complaints cp WHERE cp.report_id = r.id), '') AS complaint_history, " +
                "COALESCE((SELECT nq.value FROM network_quality nq WHERE nq.report_id = r.id ORDER BY nq.id DESC LIMIT 1), '') AS network_quality, " +
                "r.status, " +
                "r.report_content, " +
                "r.created_at, " +
                "r.updated_at " +
                "FROM reports r " +
                "JOIN customers c ON r.customer_id = c.id " +
                "JOIN managers m ON r.manager_id = m.id ";

        if (search != null && !search.isBlank()) {
            String like = "%" + search.trim() + "%";
            return db.query(
                baseSql +
                "WHERE c.customer_id ILIKE ? OR c.customer_name ILIKE ? OR m.manager_name ILIKE ? " +
                "OR r.service_code ILIKE ? OR r.current_plan ILIKE ? OR m.manager_id ILIKE ? " +
                "ORDER BY r.created_at DESC",
                (rs, i) -> mapRow(rs),
                like, like, like, like, like, like
            );
        }
        return db.query(baseSql + "ORDER BY r.created_at DESC",
                (rs, i) -> mapRow(rs));
    }

    // ────────── GET /api/reports/{id} ──────────

    @GetMapping("/{id}")
    public ResponseEntity<ReportResponse> get(@PathVariable Long id) {
        ReportResponse r = getById(id);
        if (r == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(r);
    }

    // ────────── GET /api/reports/{id}/download?format=txt|pdf|csv ──────────

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id,
                                           @RequestParam(defaultValue = "txt") String format) {
        ReportResponse r = getById(id);
        if (r == null) return ResponseEntity.notFound().build();

        return switch (format.toLowerCase()) {
            case "pdf" -> buildPdfResponse(r);
            case "csv" -> buildCsvResponse(r);
            default    -> buildTxtResponse(r);
        };
    }

    // ────────── private helpers ──────────

    private ReportResponse getById(Long id) {
        List<ReportResponse> rows = db.query(
                "SELECT " +
                "r.id, " +
                "c.customer_id AS customer_id, " +
                "c.customer_name AS customer_name, " +
                "c.national_id AS national_id, " +
                "m.manager_name AS manager_name, " +
                "m.manager_id AS manager_id, " +
                "r.service_code, " +
                "r.current_plan, " +
                "r.additional_services, " +
                "COALESCE((SELECT '[' || string_agg(sh.amount::text, ',' ORDER BY sh.month) || ']' " +
                "          FROM spending_history sh WHERE sh.report_id = r.id), '[]') AS spending_last6, " +
                "COALESCE((SELECT string_agg(cp.description, '||' ORDER BY cp.complaint_date, cp.id) " +
                "          FROM complaints cp WHERE cp.report_id = r.id), '') AS complaint_history, " +
                "COALESCE((SELECT nq.value FROM network_quality nq WHERE nq.report_id = r.id ORDER BY nq.id DESC LIMIT 1), '') AS network_quality, " +
                "r.status, " +
                "r.report_content, " +
                "r.created_at, " +
                "r.updated_at " +
                "FROM reports r " +
                "JOIN customers c ON r.customer_id = c.id " +
                "JOIN managers m ON r.manager_id = m.id " +
                "WHERE r.id=?",
                (rs, i) -> mapRow(rs),
                id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Long upsertCustomer(ReportRequest req) {
        return db.queryForObject(
                "INSERT INTO customers (customer_id, customer_name, national_id) VALUES (?,?,?) " +
                "ON CONFLICT (customer_id) DO UPDATE SET " +
                "customer_name = EXCLUDED.customer_name, " +
                "national_id = EXCLUDED.national_id, " +
                "updated_at = NOW() " +
                "RETURNING id",
                Long.class,
                req.customerId,
                req.customerName,
                req.nationalId
        );
    }

    private Long upsertManager(ReportRequest req) {
        return db.queryForObject(
                "INSERT INTO managers (manager_id, manager_name) VALUES (?,?) " +
                "ON CONFLICT (manager_id) DO UPDATE SET " +
                "manager_name = EXCLUDED.manager_name, " +
                "updated_at = NOW() " +
                "RETURNING id",
                Long.class,
                req.managerId,
                req.managerName
        );
    }

    private void insertSpendingHistory(Long reportId, List<Double> spendingLast6) {
        if (spendingLast6 == null || spendingLast6.isEmpty()) return;

        YearMonth start = YearMonth.now().minusMonths(spendingLast6.size() - 1L);
        for (int i = 0; i < spendingLast6.size(); i++) {
            YearMonth ym = start.plusMonths(i);
            db.update(
                    "INSERT INTO spending_history (report_id, month, amount) VALUES (?,?,?)",
                    reportId,
                    java.sql.Date.valueOf(ym.atDay(1)),
                    spendingLast6.get(i)
            );
        }
    }

    private void insertComplaints(Long reportId, List<String> complaintHistory) {
        if (complaintHistory == null || complaintHistory.isEmpty()) return;

        LocalDate today = LocalDate.now();
        for (int i = 0; i < complaintHistory.size(); i++) {
            db.update(
                    "INSERT INTO complaints (report_id, complaint_date, description) VALUES (?,?,?)",
                    reportId,
                    java.sql.Date.valueOf(today.minusDays(complaintHistory.size() - 1L - i)),
                    complaintHistory.get(i)
            );
        }
    }

    private void insertNetworkQuality(Long reportId, String networkQuality) {
        if (networkQuality == null || networkQuality.isBlank()) return;
        db.update(
                "INSERT INTO network_quality (report_id, metric, value) VALUES (?,?,?)",
                reportId,
                "summary",
                networkQuality
        );
    }

    private ReportResponse mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        ReportResponse r = new ReportResponse();
        r.id = rs.getLong("id");
        r.customerId = rs.getString("customer_id");
        r.customerName = rs.getString("customer_name");
        r.nationalId = rs.getString("national_id");
        r.managerName = rs.getString("manager_name");
        r.managerId = rs.getString("manager_id");
        r.serviceCode = rs.getString("service_code");
        r.currentPlan = rs.getString("current_plan");
        r.additionalServices = rs.getString("additional_services");
        r.spendingLast6 = rs.getString("spending_last6");
        r.complaintHistory = rs.getString("complaint_history");
        r.networkQuality = rs.getString("network_quality");
        r.status = rs.getString("status");
        r.reportContent = rs.getString("report_content");
        r.createdAt = rs.getTimestamp("created_at").toString();
        r.updatedAt = rs.getTimestamp("updated_at").toString();
        return r;
    }

    // ────────── download helpers ──────────

    private ResponseEntity<byte[]> buildTxtResponse(ReportResponse r) {
        String txt = buildPlainText(r);
        byte[] bytes = txt.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=report_" + r.id + ".txt")
                .contentType(MediaType.TEXT_PLAIN)
                .body(bytes);
    }

    private ResponseEntity<byte[]> buildCsvResponse(ReportResponse r) {
        StringBuilder csv = new StringBuilder();
        csv.append("字段,值\n");
        csv.append("报告ID,").append(r.id).append("\n");
        csv.append("客户编号,").append(r.customerId).append("\n");
        csv.append("客户姓名,").append(r.customerName).append("\n");
        csv.append("身份证号,").append(r.nationalId).append("\n");
        csv.append("客户经理,").append(r.managerName).append("\n");
        csv.append("经理工号,").append(r.managerId).append("\n");
        csv.append("业务编码,").append(r.serviceCode).append("\n");
        csv.append("当前套餐,").append(r.currentPlan).append("\n");
        csv.append("附加服务,").append(r.additionalServices != null ? r.additionalServices : "").append("\n");
        csv.append("近6月消费,").append(r.spendingLast6 != null ? r.spendingLast6 : "").append("\n");
        csv.append("状态,").append(r.status).append("\n");
        csv.append("创建时间,").append(r.createdAt).append("\n");
        // Report content in a quoted field (handle newlines)
        String content = r.reportContent != null ? r.reportContent.replace("\"", "\"\"") : "";
        csv.append("报告内容,\"").append(content).append("\"\n");

        // BOM + content so Excel opens with correct encoding
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(body, 0, result, bom.length, body.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=report_" + r.id + ".csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(result);
    }

    private ResponseEntity<byte[]> buildPdfResponse(ReportResponse r) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);

            // Use a built-in font that supports basic CJK via identity encoding
            // For full CJK, a Chinese TTF font file would be needed;
            // here we fall back to Helvetica and the text will still be in the PDF
            PdfFont font;
            try {
                // Try to load a system CJK font (macOS / Linux common paths)
                String[] fontPaths = {
                    "/System/Library/Fonts/STHeiti Light.ttc,0",
                    "/System/Library/Fonts/PingFang.ttc,0",
                    "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc,0",
                    "/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf"
                };
                PdfFont loaded = null;
                for (String path : fontPaths) {
                    try {
                        String clean = path.contains(",") ? path.split(",")[0] : path;
                        if (new java.io.File(clean).exists()) {
                            loaded = PdfFontFactory.createFont(path, PdfEncodings.IDENTITY_H);
                            break;
                        }
                    } catch (Exception ignored) {}
                }
                font = loaded != null ? loaded : PdfFontFactory.createFont();
            } catch (Exception e) {
                font = PdfFontFactory.createFont();
            }

            doc.setFont(font).setFontSize(11);
            doc.add(new Paragraph("客户服务优化报告 #" + r.id).setFontSize(16).setBold());
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("客户: " + r.customerName + " (" + r.customerId + ")"));
            doc.add(new Paragraph("客户经理: " + r.managerName + " (" + r.managerId + ")"));
            doc.add(new Paragraph("业务编码: " + r.serviceCode));
            doc.add(new Paragraph("当前套餐: " + r.currentPlan));
            doc.add(new Paragraph("创建时间: " + r.createdAt));
            doc.add(new Paragraph(" "));

            if (r.reportContent != null) {
                // Split markdown content into paragraphs
                for (String line : r.reportContent.split("\n")) {
                    Paragraph p = new Paragraph(line);
                    if (line.startsWith("#")) {
                        p.setBold().setFontSize(13);
                    }
                    doc.add(p);
                }
            }

            doc.close();
            byte[] bytes = baos.toByteArray();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=report_" + r.id + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(bytes);
        } catch (Exception e) {
            log.error("PDF generation failed for report {}", r.id, e);
            // Fallback to TXT
            return buildTxtResponse(r);
        }
    }

    private String buildPlainText(ReportResponse r) {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("  客户服务优化报告 #").append(r.id).append("\n");
        sb.append("========================================\n\n");
        sb.append("客户编号: ").append(r.customerId).append("\n");
        sb.append("客户姓名: ").append(r.customerName).append("\n");
        sb.append("身份证号: ").append(r.nationalId).append("\n");
        sb.append("客户经理: ").append(r.managerName).append(" (").append(r.managerId).append(")\n");
        sb.append("业务编码: ").append(r.serviceCode).append("\n");
        sb.append("当前套餐: ").append(r.currentPlan).append("\n");
        sb.append("创建时间: ").append(r.createdAt).append("\n");
        sb.append("\n────────────────────────────────────────\n\n");
        sb.append(r.reportContent != null ? r.reportContent : "（无内容）");
        sb.append("\n");
        return sb.toString();
    }

    // ────────── LLM call ──────────

    private String callLLM(ReportRequest req) throws Exception {
        String prompt = buildPrompt(req);
        log.debug("LLM prompt length={}", prompt.length());

        // Build OpenAI-compatible request body
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content",
                        "You are a China Telecom customer service optimization expert. " +
                        "Generate a structured report in Chinese with the following sections:\n" +
                        "1. 客户概况 (Customer Profile Summary)\n" +
                        "2. 问题识别 (Identified Issues)\n" +
                        "3. 优化建议 (Optimization Recommendations)\n" +
                        "4. 风险评估 (Risk Assessment)\n" +
                        "5. 后续跟进计划 (Follow-up Plan)\n" +
                        "Use markdown formatting."),
                Map.of("role", "user", "content", prompt)
        ));
        body.put("temperature", 0.7);
        body.put("max_tokens", 2000);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        String url = baseUrl + "/v1/chat/completions";
        log.info("Calling LLM at {}", url);

        ResponseEntity<String> resp = rest.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(mapper.writeValueAsString(body), headers),
                String.class
        );

        JsonNode root = mapper.readTree(resp.getBody());
        return root.at("/choices/0/message/content").asText();
    }

    private String buildPrompt(ReportRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("Please generate a customer service optimization report based on the following data:\n\n");
        sb.append("Customer ID: ").append(req.customerId).append("\n");
        sb.append("Customer Name: ").append(req.customerName).append("\n");
        sb.append("National ID: ").append(req.nationalId).append("\n");
        sb.append("Account Manager: ").append(req.managerName).append(" (").append(req.managerId).append(")\n");
        sb.append("Service Code: ").append(req.serviceCode).append("\n");
        sb.append("Current Plan: ").append(req.currentPlan).append("\n");

        if (req.additionalServices != null && !req.additionalServices.isEmpty()) {
            sb.append("Additional Services: ").append(String.join(", ", req.additionalServices)).append("\n");
        }
        if (req.spendingLast6 != null && !req.spendingLast6.isEmpty()) {
            sb.append("Last 6-Month Spending: ").append(req.spendingLast6).append("\n");
        }
        if (req.complaintHistory != null && !req.complaintHistory.isEmpty()) {
            sb.append("Complaint History:\n");
            req.complaintHistory.forEach(c -> sb.append("  - ").append(c).append("\n"));
        }
        if (req.networkQuality != null) {
            sb.append("Network Quality: ").append(req.networkQuality).append("\n");
        }
        return sb.toString();
    }
}
