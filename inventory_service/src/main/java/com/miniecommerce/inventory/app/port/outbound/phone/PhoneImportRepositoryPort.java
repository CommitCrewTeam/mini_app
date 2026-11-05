package com.miniecommerce.inventory.app.port.outbound.phone;

import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportStat;

import com.miniecommerce.inventory.domain.PhoneImport;
import com.miniecommerce.inventory.domain.enums.PhoneImportStatus;
import reactor.core.publisher.Mono;

public interface PhoneImportRepositoryPort {

    Mono<PhoneImportStat> getStat(Long importId);

    Mono<PhoneImport> save(PhoneImport phoneImport);

    /** Atomically changes status only while the caller still owns the expected version. */
    Mono<Boolean> compareAndSetStatus(Long id, PhoneImportStatus expectedStatus,
            Long expectedVersion, PhoneImportStatus nextStatus);

    Mono<PhoneImport> findById(Long id);
}
