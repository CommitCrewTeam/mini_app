package com.miniecommerce.inventory.app.port.outbound.phone;

import com.miniecommerce.inventory.domain.PhoneDraft;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneDraftApprovalSummary;
import java.util.List;
import java.util.Set;

public interface BlockingPhoneDraftRepositoryPort {
    List<PhoneDraft> findPage(Long importId, int page, int size);
    List<PhoneDraft> findChunkAfterId(Long importId, long afterId, int size);
    Set<String> findDuplicateKeys(Long importId, List<String> keys);
    PhoneDraftApprovalSummary summarizeForApproval(Long importId);
    void saveAll(Long importId, List<? extends PhoneDraft> drafts);
    /** Atomically applies persisted RESTOCK drafts and their stock increments in the chunk transaction. */
    void applyRestock(Long importId, List<Long> draftIds);
}
