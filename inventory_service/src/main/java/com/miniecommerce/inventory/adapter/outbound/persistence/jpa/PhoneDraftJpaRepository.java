package com.miniecommerce.inventory.adapter.outbound.persistence.jpa;

import com.miniecommerce.inventory.adapter.outbound.persistence.jpa.entity.PhoneDraftJpaEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhoneDraftJpaRepository extends JpaRepository<PhoneDraftJpaEntity, Long> {
    List<PhoneDraftJpaEntity> findByImportIdOrderByIdAsc(Long importId, Pageable pageable);
    List<PhoneDraftJpaEntity> findByImportIdAndIdGreaterThanOrderByIdAsc(Long importId, long afterId, Pageable pageable);
    List<PhoneDraftJpaEntity> findByImportIdAndKeyIn(Long importId, List<String> keys);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            WITH applied AS (
                UPDATE phone_draft d
                SET status = 'APPLIED', version = d.version + 1
                WHERE d.import_id = :importId
                  AND d.id IN (:draftIds)
                  AND d.status = 'VALID'
                  AND d.quantity >= 0
                RETURNING d.key, d.quantity
            )
            UPDATE phone p
            SET stock = p.stock + a.quantity, version = p.version + 1
            FROM applied a
            WHERE p.key = a.key
            """, nativeQuery = true)
    int applyRestock(@Param("importId") Long importId, @Param("draftIds") List<Long> draftIds);

    @Query("""
            SELECT d.status AS status, COUNT(d) AS rowCount
            FROM PhoneDraftJpaEntity d
            WHERE d.importId = :importId
            GROUP BY d.status
            """)
    List<StatusCount> summarizeStatusesByImportId(@Param("importId") Long importId);

    interface StatusCount {
        String getStatus();
        long getRowCount();
    }
}
