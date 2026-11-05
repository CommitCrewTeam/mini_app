package com.miniecommerce.inventory.adapter.outbound.persistence.phone;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import com.miniecommerce.inventory.adapter.outbound.persistence.phone.entity.PhoneEntity;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportMatch;

import com.miniecommerce.inventory.adapter.outbound.mapper.PhonePersistenceMapper;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneRepositoryPort;
import com.miniecommerce.inventory.domain.Phone;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class PhonePersistenceAdapter implements PhoneRepositoryPort {

    private final PhoneSpringDataRepository repository;
    private final PhonePersistenceMapper mapper;
    private final TransactionalOperator transactions;

    public PhonePersistenceAdapter(
            PhoneSpringDataRepository repository,
            PhonePersistenceMapper mapper,
            @Qualifier("inventoryTransactions") TransactionalOperator transactions
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.transactions = transactions;
    }

    @Override
    public Flux<Phone> findByKeys(List<String> keys) {
        if (keys.isEmpty()) {
            return Flux.empty();
        }
        return repository.findByKeyIn(keys)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Phone> save(Phone phone) {
        return repository.save(mapper.toEntity(phone))
                .onErrorMap(OptimisticLockingFailureException.class,
                        error -> new AppException(ErrorCode.CONFLICT,
                                "Phone changed after it was read"))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Void> saveChanges(List<Phone> phones) {
        var entities = phones.stream()
                .map(mapper::toEntity)
                .toList();
        // saveAll saves sequentially, so @Version checks each UPDATE against the
        // version that was read.
        return repository.saveAll(entities)
                .then()
                .as(transactions::transactional)
                .onErrorMap(OptimisticLockingFailureException.class,
                        error -> new AppException(ErrorCode.CONFLICT,
                                "Phone changed after it was read"));
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
        return repository.findAllByKeyIn(keys);
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
