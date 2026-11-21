package com.miniecommerce.inventory.adapter.outbound.persistence.jpa;

import com.miniecommerce.inventory.adapter.outbound.persistence.jpa.entity.PhoneDraftJpaEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhoneDraftJpaRepository extends JpaRepository<PhoneDraftJpaEntity, Long> {
    List<PhoneDraftJpaEntity> findByImportIdOrderByIdAsc(Long importId, Pageable pageable);
    List<PhoneDraftJpaEntity> findByImportIdAndIdGreaterThanOrderByIdAsc(Long importId, long afterId, Pageable pageable);
    List<PhoneDraftJpaEntity> findByImportIdAndKeyIn(Long importId, List<String> keys);
    long countByImportId(Long importId);
    long countByImportIdAndStatusNot(Long importId, String status);
}
