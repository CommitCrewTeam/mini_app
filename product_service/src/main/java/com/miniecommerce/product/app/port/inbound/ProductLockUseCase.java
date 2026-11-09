package com.miniecommerce.product.app.port.inbound;

import com.miniecommerce.product.app.port.dto.ProductResponse;

public interface ProductLockUseCase {

    void resetStock(Long productId, int initialStock);

    int getStock(Long productId);

    boolean buyWithoutLock(Long productId);

    boolean buyWithRedissonLock(Long productId);

    ProductResponse runConcurrentTest(Long productId, boolean useLock, int totalRequests);
}
