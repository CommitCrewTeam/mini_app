package com.miniecommerce.inventory.adapter.outbound.persistence.phone;

import com.miniecommerce.inventory.adapter.outbound.persistence.phone.entity.PhoneDraftEntity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface PhoneDraftSpringDataRepository
        extends ReactiveCrudRepository<PhoneDraftEntity, Long> {

    @org.springframework.data.r2dbc.repository.Query("""
            SELECT EXISTS (SELECT 1 FROM phone_draft WHERE import_id = :importId)
               AND NOT EXISTS (
                   SELECT 1 FROM phone_draft
                   WHERE import_id = :importId AND (status IS NULL OR status <> :validStatus)
               )
            """)
    reactor.core.publisher.Mono<Boolean> allValid(Long importId, String validStatus);


    Flux<PhoneDraftEntity> findAllByImportIdOrderByIdAsc(Long importId, Pageable pageable);

    reactor.core.publisher.Mono<Boolean> existsByImportIdAndStatus(Long importId, String status);

    Flux<PhoneDraftEntity> findAllByImportIdAndKeyIn(Long importId, java.util.Collection<String> keys);

    Flux<PhoneDraftEntity> findAllByImportIdAndIdIn(Long importId, java.util.Collection<Long> ids);

    Flux<PhoneDraftEntity> findAllByImportIdAndIdGreaterThanOrderByIdAsc(
            Long importId, long afterId, Pageable pageable);

    Flux<PhoneDraftEntity> findAllByImportIdAndStatusAndIdGreaterThanOrderByIdAsc(
            Long importId, String status, long afterId, Pageable pageable);
}
