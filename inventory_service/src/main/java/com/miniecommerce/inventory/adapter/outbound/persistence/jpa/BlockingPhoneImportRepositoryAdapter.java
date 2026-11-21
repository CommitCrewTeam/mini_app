package com.miniecommerce.inventory.adapter.outbound.persistence.jpa;

import com.miniecommerce.inventory.app.port.outbound.phone.BlockingPhoneImportRepositoryPort;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportStatuses;
import com.miniecommerce.inventory.domain.enums.PhoneDraftStatus;
import org.springframework.stereotype.Repository;

@Repository
public class BlockingPhoneImportRepositoryAdapter implements BlockingPhoneImportRepositoryPort {
    private final PhoneImportJpaRepository repository;

    public BlockingPhoneImportRepositoryAdapter(PhoneImportJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean writeStatusResult(Long id, boolean operationSucceeded, PhoneImportStatuses statuses, String error) {
        return repository.writeStatusResult(id, operationSucceeded, statuses.checkStatus().name(),
                statuses.successStatus().name(), statuses.failureStatus().name(), error) == 1;
    }

    @Override
    public boolean writeValidationResult(Long id, boolean operationSucceeded, PhoneDraftStatus validDraftStatus,
            PhoneImportStatuses statuses) {
        return repository.writeValidationResult(id, operationSucceeded, validDraftStatus.name(),
                statuses.checkStatus().name(), statuses.successStatus().name(), statuses.failureStatus().name()) == 1;
    }
}
