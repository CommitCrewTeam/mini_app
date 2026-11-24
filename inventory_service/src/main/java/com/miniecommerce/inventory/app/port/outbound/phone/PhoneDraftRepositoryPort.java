package com.miniecommerce.inventory.app.port.outbound.phone;

import reactor.core.publisher.Flux;

import com.miniecommerce.inventory.domain.PhoneDraft;
import java.util.List;
import reactor.core.publisher.Mono;

public interface PhoneDraftRepositoryPort {

    Flux<PhoneDraft> findPage(Long importId, int page, int size);
    Mono<Long> countByImportId(Long importId);

}
