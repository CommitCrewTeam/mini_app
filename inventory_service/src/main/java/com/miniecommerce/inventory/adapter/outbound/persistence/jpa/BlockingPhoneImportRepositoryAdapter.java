package com.miniecommerce.inventory.adapter.outbound.persistence.jpa;

import com.miniecommerce.inventory.app.port.outbound.phone.BlockingPhoneImportRepositoryPort;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportCASStatuses;
import com.miniecommerce.inventory.domain.enums.PhoneDraftStatus;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class BlockingPhoneImportRepositoryAdapter implements BlockingPhoneImportRepositoryPort {
    private final PhoneImportJpaRepository repository;

    public BlockingPhoneImportRepositoryAdapter(PhoneImportJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean writeStatusResult(Long id, boolean operationSucceeded, PhoneImportCASStatuses statuses, String error) {
        return repository.writeStatusResult(id, operationSucceeded, statuses, error) == 1;
    }

    @Override
    public boolean writeValidationResult(Long id, boolean operationSucceeded,
            List<PhoneDraftStatus> approvableDraftStatuses,
            PhoneImportCASStatuses statuses) {
        return repository.writeValidationResult(id, operationSucceeded,
                approvableDraftStatuses.stream().map(Enum::name).toList(),
                statuses) == 1;
    }
}
