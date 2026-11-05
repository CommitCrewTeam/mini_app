package com.miniecommerce.inventory.adapter.outbound.persistence.phone;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportStat;

import com.miniecommerce.inventory.adapter.outbound.mapper.PhoneImportPersistenceMapper;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneImportRepositoryPort;
import com.miniecommerce.inventory.domain.PhoneImport;
import com.miniecommerce.inventory.domain.enums.PhoneImportStatus;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class PhoneImportPersistenceAdapter implements PhoneImportRepositoryPort {

    private final PhoneImportSpringDataRepository repository;
    private final PhoneImportPersistenceMapper mapper;

    public PhoneImportPersistenceAdapter(
            PhoneImportSpringDataRepository repository,
            PhoneImportPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
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
    public Mono<Boolean> compareAndSetStatus(Long id, PhoneImportStatus expectedStatus,
            Long expectedVersion, PhoneImportStatus nextStatus) {
        return repository.compareAndSetStatus(id, expectedStatus.name(), expectedVersion, nextStatus.name())
                .map(updatedRows -> updatedRows == 1);
    }

    @Override
    public Mono<PhoneImport> findById(Long id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }
}
