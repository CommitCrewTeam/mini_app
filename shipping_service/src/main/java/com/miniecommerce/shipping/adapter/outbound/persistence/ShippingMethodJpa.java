package com.miniecommerce.shipping.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShippingMethodJpa extends JpaRepository<ShippingMethodEntity, Long> {

    List<ShippingMethodEntity> findByActiveTrue();
}