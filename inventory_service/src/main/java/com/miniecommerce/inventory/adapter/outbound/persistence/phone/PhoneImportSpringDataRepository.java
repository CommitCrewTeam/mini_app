package com.miniecommerce.inventory.adapter.outbound.persistence.phone;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Mono;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface PhoneImportSpringDataRepository
        extends ReactiveCrudRepository<PhoneImportEntity, UUID> {

    @Modifying
    @Query("""
            UPDATE phone_import
            SET status = :nextStatus, version = version + 1, updated_at = CURRENT_TIMESTAMP
            WHERE id = :id AND status = :expectedStatus AND version = :expectedVersion
            """)
    Mono<Integer> compareAndSetStatus(@Param("id") UUID id,
            @Param("expectedStatus") String expectedStatus,
            @Param("expectedVersion") Long expectedVersion,
            @Param("nextStatus") String nextStatus);
}
