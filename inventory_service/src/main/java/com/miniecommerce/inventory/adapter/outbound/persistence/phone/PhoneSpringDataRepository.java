package com.miniecommerce.inventory.adapter.outbound.persistence.phone;

import com.miniecommerce.inventory.adapter.outbound.persistence.phone.entity.PhoneEntity;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportMatch;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import java.util.Collection;
import reactor.core.publisher.Flux;

public interface PhoneSpringDataRepository
        extends ReactiveCrudRepository<PhoneEntity, Long> {


    Flux<PhoneImportMatch> findAllByKeyIn(Collection<String> keys);

    @org.springframework.data.r2dbc.repository.Query("SELECT * FROM phone WHERE key IN (:keys)")
    Flux<PhoneEntity> findByKeyIn(Collection<String> keys);
}
