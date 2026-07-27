package com.miniecommerce.seed;

import liquibase.integration.spring.SpringLiquibase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

@Component
public class SeedLiquibaseRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedLiquibaseRunner.class);

    private final SeedTargetsProperties properties;
    private final JdbcTemplate jdbcTemplate;

    public SeedLiquibaseRunner(SeedTargetsProperties properties, JdbcTemplate jdbcTemplate) {
        this.properties = properties;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        ensureSeedRunHistoryTable();

        for (SeedTargetsProperties.Target target : properties.getTargets()) {
            log.info("Running seed changelog for target '{}'", target.getName());
            Long historyId = startSeedRun(target);

            try {
                DriverManagerDataSource dataSource = new DriverManagerDataSource();
                dataSource.setDriverClassName("org.postgresql.Driver");
                dataSource.setUrl(target.getUrl());
                dataSource.setUsername(target.getUsername());
                dataSource.setPassword(target.getPassword());

                SpringLiquibase liquibase = new SpringLiquibase();
                liquibase.setDataSource(dataSource);
                liquibase.setChangeLog(target.getChangeLog());
                liquibase.afterPropertiesSet();

                finishSeedRun(historyId);
                log.info("Finished seed changelog for target '{}'", target.getName());
            } catch (Exception exception) {
                failSeedRun(historyId, exception);
                throw exception;
            }
        }
    }

    private void ensureSeedRunHistoryTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS seed_run_history (
                    id BIGSERIAL PRIMARY KEY,
                    target_name VARCHAR(128) NOT NULL,
                    target_url TEXT NOT NULL,
                    change_log TEXT NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    finished_at TIMESTAMP WITH TIME ZONE,
                    error_message TEXT
                )
                """);
    }

    private Long startSeedRun(SeedTargetsProperties.Target target) {
        return jdbcTemplate.queryForObject("""
                        INSERT INTO seed_run_history (target_name, target_url, change_log, status)
                        VALUES (?, ?, ?, 'RUNNING')
                        RETURNING id
                        """,
                Long.class,
                target.getName(),
                target.getUrl(),
                target.getChangeLog());
    }

    private void finishSeedRun(Long historyId) {
        jdbcTemplate.update("""
                        UPDATE seed_run_history
                        SET status = 'SUCCESS',
                            finished_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                historyId);
    }

    private void failSeedRun(Long historyId, Exception exception) {
        jdbcTemplate.update("""
                        UPDATE seed_run_history
                        SET status = 'FAILED',
                            finished_at = CURRENT_TIMESTAMP,
                            error_message = ?
                        WHERE id = ?
                        """,
                exception.getMessage(),
                historyId);
    }
}
