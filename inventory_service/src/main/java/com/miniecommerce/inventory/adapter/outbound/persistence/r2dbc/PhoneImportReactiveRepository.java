package com.miniecommerce.inventory.adapter.outbound.persistence.r2dbc;

import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportStat;

import com.miniecommerce.inventory.adapter.outbound.persistence.r2dbc.entity.PhoneImportEntity;
import java.util.Collection;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Mono;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface PhoneImportReactiveRepository
        extends ReactiveCrudRepository<PhoneImportEntity, Long> {

    @Query("""
            SELECT COUNT(d.id) AS total_count,
                   COUNT(CASE WHEN d.status = 'INVALID' THEN 1 END) AS fail_count,
                   COUNT(CASE WHEN d.status IN ('VALID') THEN 1 END) AS valid_count,
                   i.mode, i.status
            FROM phone_import i
            LEFT JOIN phone_draft d ON d.import_id = i.id
            WHERE i.id = :importId
            GROUP BY i.id, i.mode, i.status
            """)
    Mono<PhoneImportStat> getStat(@Param("importId") Long importId);

    @Modifying
    @Query("""
            UPDATE phone_import
            SET status = :nextStatus, version = version + 1, updated_at = CURRENT_TIMESTAMP
            WHERE id = :id AND status IN (:statuses)
            """)
    Mono<Integer> writeStatusIfStatusIn(@Param("id") Long id,
            @Param("statuses") Collection<String> statuses,
            @Param("nextStatus") String nextStatus);


}
