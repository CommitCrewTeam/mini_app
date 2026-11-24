package com.miniecommerce.inventory.adapter.outbound.persistence.r2dbc;

import org.springframework.data.r2dbc.repository.Query;

import reactor.core.publisher.Mono;

import reactor.core.publisher.Flux;

import com.miniecommerce.inventory.adapter.outbound.persistence.r2dbc.entity.PhoneDraftEntity;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface PhoneDraftReactiveRepository
        extends ReactiveCrudRepository<PhoneDraftEntity, Long> {
    @Query("SELECT * FROM phone_draft WHERE import_id = :importId ORDER BY row_number, id LIMIT :size OFFSET :offset")
    Flux<PhoneDraftEntity> findPage(Long importId, int size, long offset);

    Mono<Long> countByImportId(Long importId);


}
