package com.miniecommerce.inventory.adapter.outbound.persistence.phone;

import java.util.UUID;
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
    reactor.core.publisher.Mono<Boolean> allValid(UUID importId, String validStatus);

    Flux<PhoneDraftEntity> findAllByImportId(UUID importId);

    Flux<PhoneDraftEntity> findAllByImportIdOrderByIdAsc(UUID importId, Pageable pageable);

    reactor.core.publisher.Mono<Boolean> existsByImportIdAndStatus(UUID importId, String status);

    Flux<PhoneDraftEntity> findAllByImportIdAndKeyIn(UUID importId, java.util.Collection<String> keys);

    Flux<PhoneDraftEntity> findAllByImportIdAndIdGreaterThanOrderByIdAsc(
            UUID importId, long afterId, Pageable pageable);

    Flux<PhoneDraftEntity> findAllByImportIdAndStatusAndIdGreaterThanOrderByIdAsc(
            UUID importId, String status, long afterId, Pageable pageable);
}
