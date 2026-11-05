package com.miniecommerce.inventory.app.port.outbound.phone;

import com.miniecommerce.inventory.domain.PhoneImport;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface PhoneImportRepositoryPort {

    Mono<PhoneImport> save(PhoneImport phoneImport);

    Mono<PhoneImport> findById(UUID id);
}
