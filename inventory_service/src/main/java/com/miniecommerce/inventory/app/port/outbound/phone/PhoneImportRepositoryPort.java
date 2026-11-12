package com.miniecommerce.inventory.app.port.outbound.phone;

import com.miniecommerce.inventory.domain.PhoneImport;
import com.miniecommerce.inventory.domain.PhoneImportStatus;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface PhoneImportRepositoryPort {

    Mono<PhoneImport> save(PhoneImport phoneImport);

    /** Atomically changes status only while the caller still owns the expected version. */
    Mono<Boolean> compareAndSetStatus(UUID id, PhoneImportStatus expectedStatus,
            Long expectedVersion, PhoneImportStatus nextStatus);

    Mono<PhoneImport> findById(UUID id);
}
