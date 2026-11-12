package com.miniecommerce.inventory.adapter.outbound.persistence.phone;

import com.miniecommerce.inventory.adapter.outbound.mapper.PhoneImportPersistenceMapper;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneImportRepositoryPort;
import com.miniecommerce.inventory.domain.PhoneImport;
import com.miniecommerce.inventory.domain.PhoneImportStatus;
import java.util.UUID;
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
    public Mono<PhoneImport> save(PhoneImport phoneImport) {
        return repository.save(mapper.toEntity(phoneImport)).map(mapper::toDomain);
    }

    @Override
    public Mono<Boolean> compareAndSetStatus(UUID id, PhoneImportStatus expectedStatus,
            Long expectedVersion, PhoneImportStatus nextStatus) {
        return repository.compareAndSetStatus(id, expectedStatus.name(), expectedVersion, nextStatus.name())
                .map(updatedRows -> updatedRows == 1);
    }

    @Override
    public Mono<PhoneImport> findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }
}
