package com.miniecommerce.inventory.app.port.outbound.phone;

import com.miniecommerce.inventory.domain.PhoneDraft;
import java.util.List;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

public interface PhoneDraftRepositoryPort {

    /** True only when the import has rows and every row is VALID. */
    Mono<Boolean> allValid(Long importId);

    Mono<PhoneDraft> findById(Long importId, Long draftId);

    Flux<PhoneDraft> findByIds(Long importId, List<Long> draftIds);

    Flux<PhoneDraft> findPage(Long importId, int page, int size);

    Mono<java.util.Set<String>> findDuplicateKeys(Long importId, List<String> keys);

    Flux<PhoneDraft> findChunkAfterId(Long importId, long afterId, int size);

    Mono<Void> saveAll(Long importId, List<? extends PhoneDraft> drafts);

}
