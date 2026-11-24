package com.miniecommerce.inventory.config;

import io.lettuce.core.dynamic.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@Configuration
public class AsyncConfiguration {

    @Bean
    public SimpleAsyncTaskExecutor phoneValidationVirtualThreadExecutor() {
        return virtualThreads("phone-validate-", 5);
    }

    @Bean
    public SimpleAsyncTaskExecutor phoneApprovalVirtualThreadExecutor() {
        return virtualThreads("phone-approve-", 5);
    }

    /** Virtual thread rẻ, nhưng connection DB thì không: luôn đặt trần số worker. */
    private static SimpleAsyncTaskExecutor virtualThreads(String prefix, int limit) {
        var executor = new SimpleAsyncTaskExecutor(prefix);
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(limit);
        return executor;
    }
}
