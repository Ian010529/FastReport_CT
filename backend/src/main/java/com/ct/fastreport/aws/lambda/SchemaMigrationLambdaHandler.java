package com.ct.fastreport.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class SchemaMigrationLambdaHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final JdbcTemplate jdbcTemplate;

    public SchemaMigrationLambdaHandler() {
        ConfigurableApplicationContext context = SpringLambdaContext.get();
        this.jdbcTemplate = context.getBean(JdbcTemplate.class);
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        try {
            String schema = new ClassPathResource("schema.sql")
                    .getContentAsString(StandardCharsets.UTF_8);
            int statements = executeSchema(schema);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "ok");
            response.put("statementsExecuted", statements);
            return response;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to apply schema.sql", ex);
        }
    }

    private int executeSchema(String schema) {
        int count = 0;
        for (String statement : schema.split(";")) {
            String sql = stripLineComments(statement).trim();
            if (sql.isBlank()) {
                continue;
            }
            jdbcTemplate.execute(sql);
            count++;
        }
        return count;
    }

    private String stripLineComments(String statement) {
        StringBuilder sql = new StringBuilder();
        for (String line : statement.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("--")) {
                sql.append(line).append('\n');
            }
        }
        return sql.toString();
    }
}
