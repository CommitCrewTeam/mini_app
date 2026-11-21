package com.miniecommerce.inventory.adapter.outbound.persistence.r2dbc;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import com.miniecommerce.common.query.Filter;
import com.miniecommerce.common.query.FilterConverter;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportStat;

import com.miniecommerce.inventory.adapter.outbound.persistence.r2dbc.mapper.PhoneImportPersistenceMapper;
import com.miniecommerce.inventory.adapter.outbound.persistence.r2dbc.entity.PhoneImportEntity;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneImportRepositoryPort;
import com.miniecommerce.inventory.domain.PhoneImport;
import com.miniecommerce.inventory.domain.enums.ImportMode;
import com.miniecommerce.inventory.domain.enums.PhoneImportStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class PhoneImportRepositoryAdapter implements PhoneImportRepositoryPort {

    private static final Map<String, Class<?>> FIELD_TYPES = Map.of(
            "id", Long.class,
            "status", String.class,
            "mode", ImportMode.class,
            "version", Long.class,
            "error", String.class,
            "createdAt", OffsetDateTime.class,
            "updatedAt", OffsetDateTime.class);

    private final PhoneImportReactiveRepository repository;
    private final PhoneImportPersistenceMapper mapper;
    private final R2dbcEntityOperations entityOperations;

    public PhoneImportRepositoryAdapter(
            PhoneImportReactiveRepository repository,
            PhoneImportPersistenceMapper mapper,
            R2dbcEntityOperations entityOperations
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.entityOperations = entityOperations;
    }

    @Override
    public Mono<PhoneImportStat> getStat(Long importId) {
        return repository.getStat(importId);
    }

    @Override
    public Mono<PhoneImport> save(PhoneImport phoneImport) {
        return repository.save(mapper.toEntity(phoneImport))
                .onErrorMap(OptimisticLockingFailureException.class,
                        error -> new AppException(ErrorCode.CONFLICT,
                                "Import changed while it was being saved"))
                .map(mapper::toDomain);
    }


    @Override
    public Mono<Boolean> writeStatusIfStatusIn(Long id, Collection<PhoneImportStatus> statuses,
            PhoneImportStatus nextStatus) {
        return repository.writeStatusIfStatusIn(id, statuses.stream().map(Enum::name).toList(), nextStatus.name())
                .map(updatedRows -> updatedRows == 1);
    }


    @Override
    public Mono<PhoneImport> findById(Long id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<PhoneImport> findPage(List<Filter> filters, int page, int size) {
        Query query = matching(filters)
                .sort(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))
                .with(PageRequest.of(page, size));
        return entityOperations.select(query, PhoneImportEntity.class)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Long> count(List<Filter> filters) {
        return entityOperations.count(matching(filters), PhoneImportEntity.class);
    }

    /** Spring Data builds the WHERE clause from Criteria; no hand-written SQL here. */
    private Query matching(List<Filter> filters) {
        if (filters == null || filters.isEmpty()) return Query.empty();
        return Query.query(Criteria.from(filters.stream()
                .map(filter -> FilterConverter.convertFilterToCriteria(filter, FIELD_TYPES))
                .toList()));
    }
}
