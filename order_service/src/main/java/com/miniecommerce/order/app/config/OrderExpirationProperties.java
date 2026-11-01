package com.miniecommerce.order.app.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.order-expiration")
public record OrderExpirationProperties(
        @DefaultValue("30m") Duration confirmationTimeout,
        @DefaultValue("0 * * * * ?") String cron,
        @DefaultValue("100") int batchSize) {
    public OrderExpirationProperties {
        if (confirmationTimeout == null || confirmationTimeout.isNegative() || confirmationTimeout.isZero()) {
            throw new IllegalArgumentException("confirmation-timeout must be positive");
        }
        if (batchSize < 1 || batchSize > 1000) {
            throw new IllegalArgumentException("batch-size must be between 1 and 1000");
        }
    }
}
