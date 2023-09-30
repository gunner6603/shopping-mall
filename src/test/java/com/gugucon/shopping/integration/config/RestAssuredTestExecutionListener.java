package com.gugucon.shopping.integration.config;

import io.restassured.RestAssured;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import java.util.List;

public class RestAssuredTestExecutionListener extends AbstractTestExecutionListener {

    @Override
    public void beforeTestMethod(final TestContext testContext) {
        final Integer serverPort = testContext.getApplicationContext()
                .getEnvironment()
                .getProperty("local.server.port", Integer.class);
        if (serverPort == null) {
            throw new IllegalStateException("localServerPort는 null일 수 없습니다.");
        }

        RestAssured.port = serverPort;
    }

}
