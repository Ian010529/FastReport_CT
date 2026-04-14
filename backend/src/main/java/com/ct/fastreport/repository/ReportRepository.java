package com.ct.fastreport.repository;

import com.ct.fastreport.dto.ReportResponse;
import com.ct.fastreport.model.InternalReportRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Repository
public class ReportRepository {

    private final JdbcTemplate db;

    public ReportRepository(JdbcTemplate db) {
        this.db = db;
    }

    public long insertReport(Long customerDbId, Long managerDbId, InternalReportRequest req) {
        String additionalServices = req.additionalServices() == null || req.additionalServices().isEmpty()
                ? null
                : String.join(",", req.additionalServices());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        db.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO reports (customer_id, manager_id, service_code, current_plan, additional_services, status) " +
                            "VALUES (?,?,?,?,?,?)",
                    new String[]{"id"}
            );
            ps.setLong(1, customerDbId);
            ps.setLong(2, managerDbId);
            ps.setString(3, req.service().serviceCode());
            ps.setString(4, req.currentPlan());
            ps.setString(5, additionalServices);
            ps.setString(6, "pending");
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey(), "insert report id missing").longValue();
    }

    public List<ReportResponse> findAll(String search, List<String> statuses, int limit, int offset) {
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

        QueryParts queryParts = buildFilters(search, statuses);
        List<Object> params = new ArrayList<>(queryParts.params());
        params.add(limit);
        params.add(offset);
        return db.query(
                baseSql + queryParts.whereClause() + " ORDER BY r.created_at DESC LIMIT ? OFFSET ?",
                (rs, i) -> mapRow(rs),
                params.toArray()
        );
    }

    public int countAll(String search, List<String> statuses) {
        String sql = "SELECT COUNT(*) FROM reports r " +
                "JOIN customers c ON r.customer_id = c.id " +
                "JOIN managers m ON r.manager_id = m.id ";
        QueryParts queryParts = buildFilters(search, statuses);
        Integer count = db.queryForObject(
                sql + queryParts.whereClause(),
                Integer.class,
                queryParts.params().toArray()
        );
        return count == null ? 0 : count;
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

    public void insertSpendingHistory(Long reportId, List<InternalReportRequest.SpendingPoint> spendingHistory) {
        if (spendingHistory == null || spendingHistory.isEmpty()) {
            return;
        }

        for (InternalReportRequest.SpendingPoint point : spendingHistory) {
            YearMonth ym = point.period() == null || point.period().isBlank()
                    ? YearMonth.now()
                    : YearMonth.parse(point.period());
            db.update(
                    "INSERT INTO spending_history (report_id, month, amount) VALUES (?,?,?)",
                    reportId,
                    java.sql.Date.valueOf(ym.atDay(1)),
                    point.amount()
            );
        }
    }

    public void insertComplaints(Long reportId, List<InternalReportRequest.ComplaintRecord> complaintHistory) {
        if (complaintHistory == null || complaintHistory.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();
        for (int i = 0; i < complaintHistory.size(); i++) {
            InternalReportRequest.ComplaintRecord complaint = complaintHistory.get(i);
            LocalDate complaintDate = complaint.occurredOn() == null || complaint.occurredOn().isBlank()
                    ? today.minusDays(complaintHistory.size() - 1L - i)
                    : LocalDate.parse(complaint.occurredOn());
            db.update(
                    "INSERT INTO complaints (report_id, complaint_date, description) VALUES (?,?,?)",
                    reportId,
                    java.sql.Date.valueOf(complaintDate),
                    complaint.description()
            );
        }
    }

    public void insertNetworkQuality(Long reportId, InternalReportRequest.NetworkQualitySnapshot networkQuality) {
        if (networkQuality == null || networkQuality.summary() == null || networkQuality.summary().isBlank()) {
            return;
        }
        db.update(
                "INSERT INTO network_quality (report_id, metric, value) VALUES (?,?,?)",
                reportId,
                "summary",
                networkQuality.summary()
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

    private QueryParts buildFilters(String search, List<String> statuses) {
        List<String> clauses = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            String like = "%" + search.trim() + "%";
            clauses.add(
                    "(c.customer_id ILIKE ? OR c.customer_name ILIKE ? OR m.manager_name ILIKE ? " +
                            "OR r.service_code ILIKE ? OR r.current_plan ILIKE ? OR m.manager_id ILIKE ?)"
            );
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }

        if (statuses != null && !statuses.isEmpty()) {
            clauses.add("r.status IN (" + String.join(",", java.util.Collections.nCopies(statuses.size(), "?")) + ")");
            params.addAll(statuses);
        }

        if (clauses.isEmpty()) {
            return new QueryParts("", params);
        }
        return new QueryParts(" WHERE " + String.join(" AND ", clauses), params);
    }

    private record QueryParts(String whereClause, List<Object> params) {
    }
}
