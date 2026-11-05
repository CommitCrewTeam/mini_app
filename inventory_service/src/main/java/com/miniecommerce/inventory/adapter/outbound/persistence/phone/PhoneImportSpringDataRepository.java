package com.miniecommerce.inventory.adapter.outbound.persistence.phone;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface PhoneImportSpringDataRepository
        extends ReactiveCrudRepository<PhoneImportEntity, UUID> { }
