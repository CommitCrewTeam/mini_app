package com.miniecommerce.inventory.app.port.outbound.phone;

import com.miniecommerce.inventory.domain.PhoneDraft;
import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

public interface PhoneDraftRepositoryPort {

    /** True only when the import has rows and every row is VALID. */
    Mono<Boolean> allValid(UUID importId);

    Mono<PhoneDraft> findById(UUID importId, Long draftId);

    Mono<Boolean> hasApplied(UUID importId);

    Flux<PhoneDraft> findPage(UUID importId, int page, int size);

    Mono<java.util.Set<String>> findDuplicateKeys(UUID importId, List<String> keys);

    Flux<PhoneDraft> findChunkAfterId(UUID importId, long afterId, int size);

    Mono<Void> saveAll(UUID importId, List<? extends PhoneDraft> drafts);

    Flux<PhoneDraft> findPendingChunkAfterId(UUID importId, long afterId, int size);
}
