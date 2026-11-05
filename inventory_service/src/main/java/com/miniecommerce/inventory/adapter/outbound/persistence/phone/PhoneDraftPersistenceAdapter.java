package com.miniecommerce.inventory.adapter.outbound.persistence.phone;

import com.miniecommerce.inventory.domain.PhoneDraftStatus;
import com.miniecommerce.inventory.adapter.outbound.mapper.PhoneImportPersistenceMapper;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneDraftRepositoryPort;
import com.miniecommerce.inventory.domain.PhoneDraft;
import io.r2dbc.spi.ConnectionFactory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class PhoneDraftPersistenceAdapter implements PhoneDraftRepositoryPort {

    private final PhoneDraftSpringDataRepository repository;
    private final PhoneImportPersistenceMapper mapper;
    private final TransactionalOperator transactions;

    public PhoneDraftPersistenceAdapter(
            PhoneDraftSpringDataRepository repository,
            PhoneImportPersistenceMapper mapper,
            ConnectionFactory connectionFactory
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.transactions = TransactionalOperator.create(
                new R2dbcTransactionManager(connectionFactory)
        );
    }

    @Override
    public Mono<Boolean> allValid(UUID importId) {
        return repository.allValid(importId, PhoneDraftStatus.VALID.name());
    }

    @Override
    public Mono<PhoneDraft> findById(UUID importId, Long draftId) {
        return repository.findById(draftId)
                .filter(entity -> importId.equals(entity.getImportId()))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Boolean> hasApplied(UUID importId) {
        return repository.existsByImportIdAndStatus(importId, PhoneDraftStatus.APPLIED.name());
    }

    @Override
    public Flux<PhoneDraft> findPage(UUID importId, int page, int size) {
        return repository.findAllByImportIdOrderByIdAsc(importId, PageRequest.of(page, size))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<java.util.Set<String>> findDuplicateKeys(UUID importId, List<String> keys) {
        if (keys.isEmpty()) return Mono.just(java.util.Set.of());
        return repository.findAllByImportIdAndKeyIn(importId, keys)
                .collect(java.util.HashMap<String, Integer>::new,
                        (counts, row) -> counts.merge(row.getKey(), 1, Integer::sum))
                .map(counts -> counts.entrySet().stream().filter(entry -> entry.getValue() > 1)
                        .map(java.util.Map.Entry::getKey).collect(java.util.stream.Collectors.toSet()));
    }

    @Override
    public Flux<PhoneDraft> findChunkAfterId(UUID importId, long afterId, int size) {
        return repository.findAllByImportIdAndIdGreaterThanOrderByIdAsc(
                importId, afterId, PageRequest.of(0, size)).map(mapper::toDomain);
    }

    @Override
    public Flux<PhoneDraft> findPendingChunkAfterId(UUID importId, long afterId, int size) {
        // Page zero supplies only the batch limit; afterId advances the keyset, never a page offset.
        return repository.findAllByImportIdAndStatusAndIdGreaterThanOrderByIdAsc(
                        importId, PhoneDraftStatus.PENDING.name(), afterId, PageRequest.of(0, size))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Void> saveAll(
            UUID importId,
            List<? extends PhoneDraft> drafts
    ) {
        return Flux.fromIterable(drafts)
                .map(draft -> mapper.toEntity(importId, draft))
                .concatMap(repository::save)
                .then()
                .as(transactions::transactional);
    }
}
