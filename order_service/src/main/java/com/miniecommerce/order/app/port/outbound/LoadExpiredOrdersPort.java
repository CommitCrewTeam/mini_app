package com.miniecommerce.order.app.port.outbound;

import com.miniecommerce.order.domain.OrderAggregateRoot;
import java.time.Instant;
import java.util.List;

public interface LoadExpiredOrdersPort {
    // Caller must hold a transaction until the returned orders have been saved.
    List<OrderAggregateRoot> findExpiredForUpdate(Instant now, int limit);
}
