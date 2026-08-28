package com.miniecommerce.inventory.app.port.inbound;

import reactor.core.publisher.Mono;

public interface GetPhoneStockUseCase {
    Mono<Integer> getStock(Long id);
}
