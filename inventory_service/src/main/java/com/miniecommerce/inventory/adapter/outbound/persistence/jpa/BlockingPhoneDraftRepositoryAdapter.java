package com.miniecommerce.inventory.adapter.outbound.persistence.jpa;

import com.miniecommerce.inventory.adapter.outbound.persistence.jpa.mapper.PhoneJpaMapper;
import com.miniecommerce.inventory.app.port.outbound.phone.BlockingPhoneDraftRepositoryPort;
import com.miniecommerce.inventory.domain.PhoneDraft;
import com.miniecommerce.inventory.domain.enums.PhoneDraftStatus;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

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
    public boolean allValid(Long importId) {
        return repository.countByImportId(importId) > 0
                && repository.countByImportIdAndStatusNot(importId, PhoneDraftStatus.VALID.name()) == 0;
    }

    @Override
    public void saveAll(Long importId, List<? extends PhoneDraft> drafts) {
        if (drafts.isEmpty()) return;
        repository.saveAll(drafts.stream().map(draft -> mapper.toEntity(importId, draft)).toList());
    }
}
