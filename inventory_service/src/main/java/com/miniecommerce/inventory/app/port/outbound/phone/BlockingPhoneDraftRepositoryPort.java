package com.miniecommerce.inventory.app.port.outbound.phone;

import com.miniecommerce.inventory.domain.PhoneDraft;
import java.util.List;
import java.util.Set;

public interface BlockingPhoneDraftRepositoryPort {
    List<PhoneDraft> findPage(Long importId, int page, int size);
    List<PhoneDraft> findChunkAfterId(Long importId, long afterId, int size);
    Set<String> findDuplicateKeys(Long importId, List<String> keys);
    boolean allValid(Long importId);
    void saveAll(Long importId, List<? extends PhoneDraft> drafts);
}
