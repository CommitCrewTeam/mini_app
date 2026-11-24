package com.miniecommerce.inventory.adapter.outbound.persistence.jpa;

import com.miniecommerce.inventory.adapter.outbound.persistence.jpa.mapper.PhoneJpaMapper;
import com.miniecommerce.inventory.app.port.outbound.phone.BlockingPhoneRepositoryPort;
import com.miniecommerce.inventory.domain.Phone;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportMatch;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class BlockingPhoneRepositoryAdapter implements BlockingPhoneRepositoryPort {
    private final PhoneJpaRepository repository;
    private final PhoneJpaMapper mapper;

    public BlockingPhoneRepositoryAdapter(PhoneJpaRepository repository, PhoneJpaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Phone> findByKeys(List<String> keys) {
        if (keys.isEmpty()) return List.of();
        return repository.findByKeyIn(keys).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<PhoneImportMatch> findImportMatchesByKeys(List<String> keys) {
        if (keys.isEmpty()) return List.of();
        return repository.findByKeyIn(keys).stream()
                .map(row -> new PhoneImportMatch(row.id, row.key, row.version)).toList();
    }

    @Override
    public void saveChanges(List<Phone> phones) {
        if (phones.isEmpty()) return;
        repository.saveAll(phones.stream().map(mapper::toEntity).toList());
    }
}
