package com.miniecommerce.inventory.adapter.outbound.persistence;

import com.miniecommerce.inventory.domain.PhoneEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface PhoneR2dbcRepository extends ReactiveCrudRepository<PhoneEntity, Long> {
}
