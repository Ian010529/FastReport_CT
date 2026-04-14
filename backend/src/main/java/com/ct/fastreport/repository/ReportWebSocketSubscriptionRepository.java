package com.ct.fastreport.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class ReportWebSocketSubscriptionRepository {

    private final JdbcTemplate db;

    public ReportWebSocketSubscriptionRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void subscribe(Long reportId, String connectionId, Instant expiresAt) {
        db.update(
                "INSERT INTO report_ws_subscriptions (report_id, connection_id, expires_at) " +
                        "VALUES (?, ?, ?) " +
                        "ON CONFLICT (report_id, connection_id) DO UPDATE SET expires_at = EXCLUDED.expires_at",
                reportId,
                connectionId,
                java.sql.Timestamp.from(expiresAt)
        );
    }

    public List<String> findConnectionIds(Long reportId) {
        return db.queryForList(
                "SELECT connection_id FROM report_ws_subscriptions " +
                        "WHERE report_id = ? AND expires_at > NOW()",
                String.class,
                reportId
        );
    }

    public void deleteConnection(String connectionId) {
        db.update("DELETE FROM report_ws_subscriptions WHERE connection_id = ?", connectionId);
    }

    public void deleteReportSubscriptions(Long reportId) {
        db.update("DELETE FROM report_ws_subscriptions WHERE report_id = ?", reportId);
    }

    public void deleteExpired() {
        db.update("DELETE FROM report_ws_subscriptions WHERE expires_at <= NOW()");
    }
}
