package com.ct.fastreport.repository;

import com.ct.fastreport.dto.ReportRequest;
import com.ct.fastreport.dto.ReportResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

@Repository
public class ReportRepository {

    private final JdbcTemplate db;

    public ReportRepository(JdbcTemplate db) {
        this.db = db;
    }

    public long insertReport(Long customerDbId, Long managerDbId, ReportRequest req, String additionalSvc) {
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
        return Objects.requireNonNull(keyHolder.getKey(), "insert report id missing").longValue();
    }

    public List<ReportResponse> findAll(String search) {
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
        return db.query(baseSql + "ORDER BY r.created_at DESC", (rs, i) -> mapRow(rs));
    }

    public ReportResponse findById(Long id) {
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
                        "WHERE r.id = ?",
                (rs, i) -> mapRow(rs),
                id
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void insertSpendingHistory(Long reportId, List<Double> spendingLast6) {
        if (spendingLast6 == null || spendingLast6.isEmpty()) {
            return;
        }

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

    public void insertComplaints(Long reportId, List<String> complaintHistory) {
        if (complaintHistory == null || complaintHistory.isEmpty()) {
            return;
        }

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

    public void insertNetworkQuality(Long reportId, String networkQuality) {
        if (networkQuality == null || networkQuality.isBlank()) {
            return;
        }
        db.update(
                "INSERT INTO network_quality (report_id, metric, value) VALUES (?,?,?)",
                reportId,
                "summary",
                networkQuality
        );
    }

    public void markProcessing(Long reportId) {
        db.update(
                "UPDATE reports SET status = ?, updated_at = NOW() WHERE id = ?",
                "processing",
                reportId
        );
    }

    public void markCompleted(Long reportId, String content) {
        db.update(
                "UPDATE reports SET status = ?, report_content = ?, updated_at = NOW() WHERE id = ?",
                "completed",
                content,
                reportId
        );
    }

    public void markFailed(Long reportId) {
        db.update(
                "UPDATE reports SET status = ?, updated_at = NOW() WHERE id = ?",
                "failed",
                reportId
        );
    }

    public boolean existsSameCustomerServiceSameDay(String customerId, String serviceCode) {
        Integer count = db.queryForObject(
                "SELECT COUNT(*) " +
                        "FROM reports r " +
                        "JOIN customers c ON r.customer_id = c.id " +
                        "WHERE c.customer_id = ? " +
                        "AND r.service_code = ? " +
                        "AND DATE(r.created_at) = CURRENT_DATE",
                Integer.class,
                customerId,
                serviceCode
        );
        return count != null && count > 0;
    }

    public boolean existsSameCustomerServiceDifferentDay(String customerId, String serviceCode) {
        Integer count = db.queryForObject(
                "SELECT COUNT(*) " +
                        "FROM reports r " +
                        "JOIN customers c ON r.customer_id = c.id " +
                        "WHERE c.customer_id = ? " +
                        "AND r.service_code = ? " +
                        "AND DATE(r.created_at) <> CURRENT_DATE",
                Integer.class,
                customerId,
                serviceCode
        );
        return count != null && count > 0;
    }

    public boolean existsDifferentCustomerForNationalId(String customerId, String nationalId) {
        Integer count = db.queryForObject(
                "SELECT COUNT(*) FROM customers WHERE national_id = ? AND customer_id <> ?",
                Integer.class,
                nationalId,
                customerId
        );
        return count != null && count > 0;
    }

    private ReportResponse mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        ReportResponse report = new ReportResponse();
        report.id = rs.getLong("id");
        report.customerId = rs.getString("customer_id");
        report.customerName = rs.getString("customer_name");
        report.nationalId = rs.getString("national_id");
        report.managerName = rs.getString("manager_name");
        report.managerId = rs.getString("manager_id");
        report.serviceCode = rs.getString("service_code");
        report.currentPlan = rs.getString("current_plan");
        report.additionalServices = rs.getString("additional_services");
        report.spendingLast6 = rs.getString("spending_last6");
        report.complaintHistory = rs.getString("complaint_history");
        report.networkQuality = rs.getString("network_quality");
        report.status = rs.getString("status");
        report.reportContent = rs.getString("report_content");
        report.createdAt = rs.getTimestamp("created_at").toString();
        report.updatedAt = rs.getTimestamp("updated_at").toString();
        return report;
    }
}
