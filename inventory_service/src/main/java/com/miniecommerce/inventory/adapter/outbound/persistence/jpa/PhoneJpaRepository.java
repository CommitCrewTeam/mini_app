package com.miniecommerce.inventory.adapter.outbound.persistence.jpa;

import com.miniecommerce.inventory.adapter.outbound.persistence.jpa.entity.PhoneJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhoneJpaRepository extends JpaRepository<PhoneJpaEntity, Long> {
    List<PhoneJpaEntity> findByKeyIn(List<String> keys);
}
