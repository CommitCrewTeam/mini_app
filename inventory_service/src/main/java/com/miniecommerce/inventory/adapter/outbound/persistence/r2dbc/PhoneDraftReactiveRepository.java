package com.miniecommerce.inventory.adapter.outbound.persistence.r2dbc;

import com.miniecommerce.inventory.adapter.outbound.persistence.r2dbc.entity.PhoneDraftEntity;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface PhoneDraftReactiveRepository
        extends ReactiveCrudRepository<PhoneDraftEntity, Long> {


}
