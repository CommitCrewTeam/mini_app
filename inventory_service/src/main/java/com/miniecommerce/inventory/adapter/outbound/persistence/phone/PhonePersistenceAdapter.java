package com.miniecommerce.inventory.adapter.outbound.persistence.phone;

import com.miniecommerce.inventory.adapter.outbound.mapper.PhonePersistenceMapper;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneRepositoryPort;
import com.miniecommerce.inventory.domain.Phone;
import com.miniecommerce.inventory.domain.PhoneImportMatch;
import java.util.List;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class PhonePersistenceAdapter implements PhoneRepositoryPort {

    private final PhoneSpringDataRepository repository;
    private final PhonePersistenceMapper mapper;
    public PhonePersistenceAdapter(PhoneSpringDataRepository repository, PhonePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<VersionedPhone> findVersionedByKey(String key) {
        return repository.findByKey(key)
                .map(entity -> new VersionedPhone(mapper.toDomain(entity), entity.getVersion()));
    }

    @Override
    public Mono<Phone> save(Phone phone, Long expectedVersion) {
        var entity = mapper.toEntity(phone);
        entity.setVersion(expectedVersion);
        return repository.save(entity).map(mapper::toDomain);
    }

    @Override
    public Flux<Phone> findAll() {
        return repository.findAll()
                .map(mapper::toDomain);
    }

    @Override
    public Flux<PhoneImportMatch> findImportMatchesByKeys(List<String> keys) {
        if (keys.isEmpty()) {
            return Flux.empty();
        }
        return repository.findAllByKeyIn(keys)
                .map(match -> new PhoneImportMatch(match.getId(), match.getKey(), match.getVersion()));
    }

    @Override
    public Mono<Phone> save(Phone phone) {
        return repository.save(mapper.toEntity(phone))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Integer> getStock(Long id) {
        return repository.findById(id)
                .map(PhoneEntity::getStock);
    }

    @Override
    public Flux<Phone> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Flux.empty();
        }
        return repository.findAllById(ids)
                .map(mapper::toDomain);
    }
}
