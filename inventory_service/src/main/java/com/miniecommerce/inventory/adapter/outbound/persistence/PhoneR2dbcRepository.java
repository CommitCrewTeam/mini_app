package com.miniecommerce.inventory.adapter.outbound.persistence;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface PhoneR2dbcRepository extends ReactiveCrudRepository<PhoneEntity, Long> {

    @Query("SELECT stock FROM phones WHERE id = :id")
    Mono<Integer> findStockById(@Param("id") Long id);
}