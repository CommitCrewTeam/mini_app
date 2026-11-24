package com.miniecommerce.inventory.adapter.outbound.persistence.r2dbc;

import com.miniecommerce.inventory.domain.PhoneDraft;

import reactor.core.publisher.Mono;

import reactor.core.publisher.Flux;

import com.miniecommerce.inventory.adapter.outbound.persistence.r2dbc.mapper.PhoneImportPersistenceMapper;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneDraftRepositoryPort;
import org.springframework.stereotype.Repository;

@Repository
public class PhoneDraftRepositoryAdapter implements PhoneDraftRepositoryPort {
    @Override
    public Flux<PhoneDraft> findPage(Long importId, int page, int size) {
        return repository.findPage(importId, size, (long) page * size).map(mapper::toDomain);
    }

    @Override
    public Mono<Long> countByImportId(Long importId) {
        return repository.countByImportId(importId);
    }

    private final PhoneDraftReactiveRepository repository;
    private final PhoneImportPersistenceMapper mapper;

    public PhoneDraftRepositoryAdapter(
            PhoneDraftReactiveRepository repository,
            PhoneImportPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

}
