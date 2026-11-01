package com.miniecommerce.order.app.service;

import com.miniecommerce.order.app.config.OrderExpirationProperties;
import com.miniecommerce.order.app.port.inbound.ExpirePendingOrdersUseCase;
import com.miniecommerce.order.app.port.outbound.LoadExpiredOrdersPort;
import com.miniecommerce.order.app.port.outbound.SaveOrderPort;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpirePendingOrdersApplicationService implements ExpirePendingOrdersUseCase {
    private final LoadExpiredOrdersPort loadOrders;
    private final SaveOrderPort saveOrders;
    private final Clock clock;
    private final OrderExpirationProperties properties;

    public ExpirePendingOrdersApplicationService(LoadExpiredOrdersPort loadOrders, SaveOrderPort saveOrders,
                                                 Clock clock, OrderExpirationProperties properties) {
        this.loadOrders = loadOrders;
        this.saveOrders = saveOrders;
        this.clock = clock;
        this.properties = properties;
    }

    @Override
    @Transactional
    public int expirePendingOrders() {
        var now = clock.instant();
        int expired = 0;
        for (var order : loadOrders.findExpiredForUpdate(now, properties.batchSize())) {
            if (order.expire(now)) {
                saveOrders.save(order);
                expired++;
            }
        }
        return expired;
    }
}
