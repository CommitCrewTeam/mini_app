package com.miniecommerce.inventory.app.port.inbound.phone;

import java.util.UUID;

import com.miniecommerce.inventory.domain.PhoneImportUpload;
import reactor.core.publisher.Mono;

public interface PhoneImportUseCase {
    //    async
    Mono<Long> validate(UUID importId);

    // async
    Mono<Long> approve(UUID importId);

    // async
    Mono<UUID> importFile(PhoneImportUpload upload);
}
