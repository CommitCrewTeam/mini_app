package com.miniecommerce.inventory.adapter.outbound.persistence.r2dbc;

import com.miniecommerce.inventory.adapter.outbound.persistence.r2dbc.entity.PhoneEntity;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportMatch;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import java.util.Collection;
import reactor.core.publisher.Flux;

public interface PhoneReactiveRepository
        extends ReactiveCrudRepository<PhoneEntity, Long> {



}
