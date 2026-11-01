package com.miniecommerce.order.adapter.outbound.persistence;

import com.miniecommerce.order.adapter.outbound.persistence.mapper.OrderPersistenceMapper;
import com.miniecommerce.order.app.config.OrderExpirationProperties;
import com.miniecommerce.order.app.service.ExpirePendingOrdersApplicationService;
import com.miniecommerce.order.domain.*;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = {"spring.liquibase.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@Import({OrderPersistenceAdapter.class, OrderPersistenceMapper.class,
        ExpirePendingOrdersApplicationService.class, OrderExpirationPersistenceTest.Config.class})
class OrderExpirationPersistenceTest {
    private static final Instant NOW = Instant.parse("2026-11-01T01:00:00Z");
    @Autowired OrderPersistenceAdapter orders;
    @Autowired ExpirePendingOrdersApplicationService expiration;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired OrderJpaRepository repository;

    @TestConfiguration
    static class Config {
        @Bean Clock clock() { return Clock.fixed(NOW, ZoneOffset.UTC); }
        @Bean OrderExpirationProperties properties() {
            return new OrderExpirationProperties(Duration.ofMinutes(30), "0 * * * * ?", 1);
        }
    }

    private OrderAggregateRoot newOrder(Instant created) {
        var order = OrderAggregateRoot.create("customer", MoneyValue.ZERO, created, Duration.ofMinutes(30));
        order.addItem("product", 1, MoneyValue.of(100));
        return order;
    }

    @Test
    void scansOldestFirstWithBatchLimitAndSkipsConfirmedAndFutureOrders() {
        var oldest = orders.save(newOrder(NOW.minusSeconds(3600)));
        var due = orders.save(newOrder(NOW.minusSeconds(1800)));
        var future = orders.save(newOrder(NOW));
        var confirmed = newOrder(NOW.minusSeconds(7200));
        confirmed.confirm();
        orders.save(confirmed);

        assertThat(expiration.expirePendingOrders()).isEqualTo(1);
        assertThat(orders.findById(oldest.getId()).orElseThrow().getCancellationReason())
                .isEqualTo("CONFIRMATION_TIMEOUT");
        assertThat(orders.findById(due.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(expiration.expirePendingOrders()).isEqualTo(1);
        assertThat(expiration.expirePendingOrders()).isZero();
        assertThat(orders.findById(future.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(orders.findById(confirmed.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void staleConfirmationCannotOverwriteCommittedExpiration() {
        var tx = new TransactionTemplate(transactionManager);
        var stale = orders.save(newOrder(NOW.minusSeconds(3600)));
        try {
            assertThat(expiration.expirePendingOrders()).isEqualTo(1);
            stale.confirm();
            assertThatThrownBy(() -> tx.execute(status -> orders.save(stale)))
                    .isInstanceOf(ObjectOptimisticLockingFailureException.class);
            assertThat(orders.findById(stale.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        } finally {
            repository.deleteAll();
        }
    }
}
