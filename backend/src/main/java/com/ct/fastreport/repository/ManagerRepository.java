package com.ct.fastreport.repository;

import com.ct.fastreport.model.InternalReportRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ManagerRepository {

    private final JdbcTemplate db;

    public ManagerRepository(JdbcTemplate db) {
        this.db = db;
    }

    public Long upsert(InternalReportRequest req) {
        return db.queryForObject(
                "INSERT INTO managers (manager_id, manager_name) VALUES (?,?) " +
                        "ON CONFLICT (manager_id) DO UPDATE SET " +
                        "manager_name = EXCLUDED.manager_name, " +
                        "updated_at = NOW() " +
                "RETURNING id",
                Long.class,
                req.manager().managerId(),
                req.manager().managerName()
        );
    }

    public boolean existsDifferentManagerName(String managerId, String managerName) {
        Integer count = db.queryForObject(
                "SELECT COUNT(*) FROM managers WHERE manager_id = ? AND manager_name <> ?",
                Integer.class,
                managerId,
                managerName
        );
        return count != null && count > 0;
    }
}
