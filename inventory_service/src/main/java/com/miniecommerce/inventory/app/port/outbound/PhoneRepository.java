package com.miniecommerce.inventory.app.port.outbound;

import com.miniecommerce.inventory.domain.Phone;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PhoneRepository {
    Flux<Phone> findAll();

    Mono<Phone> save(Phone phone);

    Mono<Integer> getStock(Long id);

    Flux<Phone> findByIds(List<Long> ids);
}