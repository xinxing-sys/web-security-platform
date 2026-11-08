package com.websecurity.platform;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DbTestController {

    private final JdbcTemplate jdbcTemplate;

    public DbTestController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/test/db")
    public Map<String, Object> testDatabase() {

        return jdbcTemplate.queryForMap(
                "SELECT VERSION() AS mysqlVersion, " +
                        "DATABASE() AS databaseName"
        );
    }
}