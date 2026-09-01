package com.miniecommerce.inventory.adapter.outbound.persistence;

import com.miniecommerce.inventory.adapter.outbound.persistence.mapper.PhonePersistenceMapper;
import com.miniecommerce.inventory.app.port.outbound.PhoneRepository;
import com.miniecommerce.inventory.domain.Phone;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public class PhoneRepositoryAdapter implements PhoneRepository {

    private final PhoneR2dbcRepository repository;
    private final PhonePersistenceMapper mapper;

    public PhoneRepositoryAdapter(PhoneR2dbcRepository repository, PhonePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Flux<Phone> findAll() {
        return repository.findAll()
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Phone> save(Phone phone) {
        return repository.save(mapper.toEntity(phone))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Integer> getStock(Long id) {
        return repository.findStockById(id);
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
