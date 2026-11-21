package com.miniecommerce.inventory.adapter.outbound.persistence.r2dbc;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import com.miniecommerce.inventory.adapter.outbound.persistence.r2dbc.entity.PhoneEntity;

import com.miniecommerce.inventory.adapter.outbound.persistence.r2dbc.mapper.PhonePersistenceMapper;
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
public class PhoneRepositoryAdapter implements PhoneRepositoryPort {

    private final PhoneReactiveRepository repository;
    private final PhonePersistenceMapper mapper;
    private final TransactionalOperator transactions;

    public PhoneRepositoryAdapter(
            PhoneReactiveRepository repository,
            PhonePersistenceMapper mapper,
            @Qualifier("inventoryTransactions") TransactionalOperator transactions
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.transactions = transactions;
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
    public Flux<Phone> findAll() {
        return repository.findAll()
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
