package com.miniecommerce.seed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        LiquibaseAutoConfiguration.class
})
@EnableConfigurationProperties(SeedTargetsProperties.class)
public class SeedServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeedServiceApplication.class, args);
    }
}
