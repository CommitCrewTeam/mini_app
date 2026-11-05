package com.miniecommerce.inventory.adapter.outbound.persistence.phone;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import com.miniecommerce.inventory.adapter.outbound.mapper.PhoneImportPersistenceMapper;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneDraftRepositoryPort;
import com.miniecommerce.inventory.domain.PhoneDraft;
import com.miniecommerce.inventory.domain.enums.PhoneDraftStatus;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
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
            @Qualifier("inventoryTransactions") TransactionalOperator transactions
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.transactions = transactions;
    }

    @Override
    public Mono<Boolean> allValid(Long importId) {
        return repository.allValid(importId, PhoneDraftStatus.VALID.name());
    }

    @Override
    public Mono<PhoneDraft> findById(Long importId, Long draftId) {
        return repository.findById(draftId)
                .filter(entity -> importId.equals(entity.getImportId()))
                .map(mapper::toDomain);
    }

    @Override
    public Flux<PhoneDraft> findByIds(Long importId, List<Long> draftIds) {
        if (draftIds.isEmpty()) {
            return Flux.empty();
        }
        return repository.findAllByImportIdAndIdIn(importId, draftIds).map(mapper::toDomain);
    }


    @Override
    public Flux<PhoneDraft> findPage(Long importId, int page, int size) {
        return repository.findAllByImportIdOrderByIdAsc(importId, PageRequest.of(page, size))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<java.util.Set<String>> findDuplicateKeys(Long importId, List<String> keys) {
        if (keys.isEmpty()) return Mono.just(java.util.Set.of());
        return repository.findAllByImportIdAndKeyIn(importId, keys)
                .collect(java.util.HashMap<String, Integer>::new,
                        (counts, row) -> counts.merge(row.getKey(), 1, Integer::sum))
                .map(counts -> counts.entrySet().stream().filter(entry -> entry.getValue() > 1)
                        .map(java.util.Map.Entry::getKey).collect(java.util.stream.Collectors.toSet()));
    }

    @Override
    public Flux<PhoneDraft> findChunkAfterId(Long importId, long afterId, int size) {
        return repository.findAllByImportIdAndIdGreaterThanOrderByIdAsc(
                importId, afterId, PageRequest.of(0, size)).map(mapper::toDomain);
    }



    @Override
    public Mono<Void> saveAll(
            Long importId,
            List<? extends PhoneDraft> drafts
    ) {
        var entities = drafts.stream()
                .map(draft -> mapper.toEntity(importId, draft))
                .toList();
        // saveAll still saves sequentially (concatMap internally), so @Version checks
        // each UPDATE against the version that was read.
        return repository.saveAll(entities)
                .then()
                .as(transactions::transactional)
                // Translate the persistence exception at the adapter boundary so the
                // application receives the same conflict language as the domain check.
                .onErrorMap(OptimisticLockingFailureException.class,
                        error -> new AppException(ErrorCode.CONFLICT,
                                "Draft changed while it was being saved"));
    }
}
