package com.miniecommerce.inventory.adapter.outbound.persistence.jpa;

import com.miniecommerce.inventory.adapter.outbound.persistence.jpa.mapper.PhoneJpaMapper;
import com.miniecommerce.inventory.app.port.outbound.phone.BlockingPhoneDraftRepositoryPort;
import com.miniecommerce.inventory.domain.PhoneDraft;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneDraftApprovalSummary;
import com.miniecommerce.inventory.domain.enums.PhoneDraftStatus;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;

@Repository
public class BlockingPhoneDraftRepositoryAdapter implements BlockingPhoneDraftRepositoryPort {
    private final PhoneDraftJpaRepository repository;
    private final PhoneJpaMapper mapper;

    public BlockingPhoneDraftRepositoryAdapter(PhoneDraftJpaRepository repository, PhoneJpaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<PhoneDraft> findPage(Long importId, int page, int size) {
        return repository.findByImportIdOrderByIdAsc(importId, PageRequest.of(page, size)).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<PhoneDraft> findChunkAfterId(Long importId, long afterId, int size) {
        return repository.findByImportIdAndIdGreaterThanOrderByIdAsc(importId, afterId, PageRequest.of(0, size))
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public Set<String> findDuplicateKeys(Long importId, List<String> keys) {
        if (keys.isEmpty()) return Set.of();
        return repository.findByImportIdAndKeyIn(importId, keys).stream()
                .collect(Collectors.groupingBy(row -> row.key, Collectors.counting()))
                .entrySet().stream().filter(entry -> entry.getValue() > 1)
                .map(java.util.Map.Entry::getKey).collect(Collectors.toSet());
    }

    @Override
    public PhoneDraftApprovalSummary summarizeForApproval(Long importId) {
        var counts = repository.summarizeStatusesByImportId(importId);
        long total = counts.stream().mapToLong(PhoneDraftJpaRepository.StatusCount::getRowCount).sum();
        long valid = counts.stream()
                .filter(row -> PhoneDraftStatus.VALID.name().equals(row.getStatus()))
                .mapToLong(PhoneDraftJpaRepository.StatusCount::getRowCount).sum();
        long warnings = counts.stream()
                .filter(row -> PhoneDraftStatus.WARNING.name().equals(row.getStatus()))
                .mapToLong(PhoneDraftJpaRepository.StatusCount::getRowCount).sum();
        long invalid = counts.stream()
                .filter(row -> PhoneDraftStatus.INVALID.name().equals(row.getStatus()))
                .mapToLong(PhoneDraftJpaRepository.StatusCount::getRowCount).sum();
        return new PhoneDraftApprovalSummary(total, valid, warnings, invalid);
    }

    @Override
    public void saveAll(Long importId, List<? extends PhoneDraft> drafts) {
        if (drafts.isEmpty()) return;
        repository.saveAll(drafts.stream().map(draft -> mapper.toEntity(importId, draft)).toList());
    }

    @Override
    @Transactional(transactionManager = "jpaTransactionManager", propagation = Propagation.MANDATORY)
    public void applyRestock(Long importId, List<Long> draftIds) {
        if (draftIds.isEmpty()) return;
        // One phone per draft is required by import validation. This also rejects missing
        // phones, duplicate keys and ineligible drafts, rolling back BOTH CTE updates.
        if (repository.applyRestock(importId, draftIds) != draftIds.size()) {
            throw new AppException(ErrorCode.CONFLICT, "Restock chunk could not apply every draft");
        }
    }
}
