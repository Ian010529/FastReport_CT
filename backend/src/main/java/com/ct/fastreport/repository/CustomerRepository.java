package com.ct.fastreport.repository;

import com.ct.fastreport.model.InternalReportRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerRepository {

    private final JdbcTemplate db;

    public CustomerRepository(JdbcTemplate db) {
        this.db = db;
    }

    public Long upsert(InternalReportRequest req) {
        return db.queryForObject(
                "INSERT INTO customers (customer_id, customer_name, national_id) VALUES (?,?,?) " +
                        "ON CONFLICT (customer_id) DO UPDATE SET " +
                        "customer_name = EXCLUDED.customer_name, " +
                        "national_id = EXCLUDED.national_id, " +
                        "updated_at = NOW() " +
                "RETURNING id",
                Long.class,
                req.customer().customerId(),
                req.customer().customerName(),
                req.customer().nationalId()
        );
    }

    public boolean existsDifferentNationalId(String customerId, String nationalId) {
        Integer count = db.queryForObject(
                "SELECT COUNT(*) FROM customers WHERE customer_id = ? AND national_id <> ?",
                Integer.class,
                customerId,
                nationalId
        );
        return count != null && count > 0;
    }
}
