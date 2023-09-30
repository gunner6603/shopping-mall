package com.gugucon.shopping.integration.config;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

public class DatabaseTruncationTestExecutionListener extends AbstractTestExecutionListener {

    @Override
    public void afterTestMethod(final TestContext testContext) {
        final JdbcTemplate jdbcTemplate = getJdbcTemplate(testContext);
        truncateTables(jdbcTemplate);
    }

    private JdbcTemplate getJdbcTemplate(final TestContext testContext) {
        return testContext.getApplicationContext().getBean(JdbcTemplate.class);
    }

    private void truncateTables(final JdbcTemplate jdbcTemplate) {
        execute(jdbcTemplate, "SET REFERENTIAL_INTEGRITY FALSE");
        getTruncateQueries(jdbcTemplate).forEach(query -> execute(jdbcTemplate, query));
        execute(jdbcTemplate, "SET REFERENTIAL_INTEGRITY TRUE");
    }

    private List<String> getTruncateQueries(final JdbcTemplate jdbcTemplate) {
        String sql = "SELECT Concat('TRUNCATE TABLE ', TABLE_NAME, ';') "
            + "FROM INFORMATION_SCHEMA.TABLES "
            + "WHERE table_schema = 'PUBLIC'";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    private void execute(final JdbcTemplate jdbcTemplate, final String query) {
        jdbcTemplate.execute(query);
    }
}
