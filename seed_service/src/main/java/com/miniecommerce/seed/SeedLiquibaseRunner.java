package com.miniecommerce.seed;

import liquibase.integration.spring.SpringLiquibase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

@Component
public class SeedLiquibaseRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedLiquibaseRunner.class);

    private final SeedTargetsProperties properties;

    public SeedLiquibaseRunner(SeedTargetsProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(String... args) throws Exception {
        for (SeedTargetsProperties.Target target : properties.getTargets()) {
            log.info("Running seed changelog for target '{}'", target.getName());

            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.postgresql.Driver");
            dataSource.setUrl(target.getUrl());
            dataSource.setUsername(target.getUsername());
            dataSource.setPassword(target.getPassword());

            SpringLiquibase liquibase = new SpringLiquibase();
            liquibase.setDataSource(dataSource);
            liquibase.setChangeLog(target.getChangeLog());
            liquibase.afterPropertiesSet();

            log.info("Finished seed changelog for target '{}'", target.getName());
        }
    }
}
