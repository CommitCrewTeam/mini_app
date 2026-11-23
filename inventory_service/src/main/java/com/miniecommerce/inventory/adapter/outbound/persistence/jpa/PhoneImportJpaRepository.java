package com.miniecommerce.inventory.adapter.outbound.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.miniecommerce.inventory.adapter.outbound.persistence.jpa.entity.PhoneImportJpaEntity;
import java.util.Collection;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportCASStatuses;

public interface PhoneImportJpaRepository extends JpaRepository<PhoneImportJpaEntity, Long> {
    @Modifying
    @Transactional
    @Query("""
            UPDATE PhoneImportJpaEntity i
            SET i.status = CASE WHEN :operationSucceeded = true THEN :#{#statuses.successStatus().name()} ELSE :#{#statuses.failureStatus().name()} END,
                i.error = :error,
                i.version = i.version + 1,
                i.updatedAt = CURRENT_TIMESTAMP
            WHERE i.id = :id AND i.status = :#{#statuses.checkStatus().name()}
            """)
    int writeStatusResult(
            @Param("id") Long id,
            @Param("operationSucceeded") boolean operationSucceeded,
            @Param("statuses") PhoneImportCASStatuses statuses,
            @Param("error") String error);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE phone_import i
            SET status = CASE
                    WHEN :validationJobSucceeded
                         AND EXISTS (SELECT 1 FROM phone_draft d WHERE d.import_id = i.id)
                         AND NOT EXISTS (
                             SELECT 1 FROM phone_draft d
                             WHERE d.import_id = i.id
                               AND d.status NOT IN (:approvableDraftStatuses)
                         )
                    THEN :#{#statuses.successStatus().name()}
                    ELSE :#{#statuses.failureStatus().name()}
                END,
                version = version + 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE i.id = :id AND i.status = :#{#statuses.checkStatus().name()}
            """, nativeQuery = true)
    int writeValidationResult(
            @Param("id") Long id,
            @Param("validationJobSucceeded") boolean validationJobSucceeded,
            @Param("approvableDraftStatuses") Collection<String> approvableDraftStatuses,
            @Param("statuses") PhoneImportCASStatuses statuses);

}
