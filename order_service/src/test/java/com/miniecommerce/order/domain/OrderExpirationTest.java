package com.miniecommerce.order.domain;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class OrderExpirationTest {
    private final Instant created = Instant.parse("2026-11-01T00:00:00Z");

    private OrderAggregateRoot order() {
        var order = OrderAggregateRoot.create("customer", MoneyValue.ZERO, created, Duration.ofMinutes(30));
        order.addItem("product", 1, MoneyValue.of(100));
        return order;
    }

    @Test
    void expiresExactlyAtDeadlineAndIsIdempotent() {
        var order = order();
        var deadline = created.plusSeconds(1800);
        assertThat(order.expire(deadline.minusNanos(1))).isFalse();
        assertThat(order.expire(deadline)).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancellationReason()).isEqualTo("CONFIRMATION_TIMEOUT");
        assertThat(order.getUpdatedAt()).isEqualTo(deadline);
        assertThat(order.expire(deadline.plusSeconds(60))).isFalse();
        assertThat(order.getUpdatedAt()).isEqualTo(deadline);
    }

    @Test
    void neverExpiresConfirmedOrManuallyCancelledOrders() {
        var confirmed = order();
        confirmed.confirm();
        var cancelled = order();
        cancelled.cancel();
        assertThat(confirmed.expire(created.plusSeconds(3600))).isFalse();
        assertThat(cancelled.expire(created.plusSeconds(3600))).isFalse();
        assertThat(confirmed.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(cancelled.getCancellationReason()).isNull();
    }
}
