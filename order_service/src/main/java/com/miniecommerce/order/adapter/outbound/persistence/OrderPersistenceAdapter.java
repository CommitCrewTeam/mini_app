package com.miniecommerce.order.adapter.outbound.persistence;

import com.miniecommerce.order.adapter.outbound.persistence.mapper.OrderPersistenceMapper;
import com.miniecommerce.order.app.port.outbound.LoadOrderPort;
import com.miniecommerce.order.app.port.outbound.SaveOrderPort;
import com.miniecommerce.order.domain.OrderAggregateRoot;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class OrderPersistenceAdapter implements SaveOrderPort, LoadOrderPort {

    private final OrderJpaRepository repository;
    private final OrderPersistenceMapper mapper;

    public OrderPersistenceAdapter(OrderJpaRepository repository, OrderPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public OrderAggregateRoot save(OrderAggregateRoot order) {
        return mapper.toDomain(repository.save(mapper.toEntity(order)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderAggregateRoot> findById(String orderId) {
        return repository.findById(orderId).map(mapper::toDomain);
    }
}