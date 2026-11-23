package com.miniecommerce.inventory.adapter.outbound.persistence.jpa;

import com.miniecommerce.inventory.adapter.outbound.persistence.jpa.entity.PhoneDraftJpaEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhoneDraftJpaRepository extends JpaRepository<PhoneDraftJpaEntity, Long> {
    List<PhoneDraftJpaEntity> findByImportIdOrderByIdAsc(Long importId, Pageable pageable);
    List<PhoneDraftJpaEntity> findByImportIdAndIdGreaterThanOrderByIdAsc(Long importId, long afterId, Pageable pageable);
    List<PhoneDraftJpaEntity> findByImportIdAndKeyIn(Long importId, List<String> keys);

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
