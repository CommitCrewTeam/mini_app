package com.miniecommerce.inventory.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
@EnableR2dbcRepositories(basePackages = "com.miniecommerce.inventory.adapter.outbound.persistence.r2dbc")
public class R2dbcInfrastructureConfiguration {
    @Bean
    public TransactionalOperator inventoryTransactions(ConnectionFactory connectionFactory) {
        return TransactionalOperator.create(new R2dbcTransactionManager(connectionFactory));
    }
}
