package com.miniecommerce.order.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import com.miniecommerce.order.domain.OrderStatus;
import java.time.Instant;
import java.util.List;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<OrderEntity> findByStatusAndConfirmationDeadlineLessThanEqualOrderByConfirmationDeadlineAscIdAsc(
            OrderStatus status, Instant deadline, Pageable pageable);
}
