package com.miniecommerce.payment.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentMethodJpa extends JpaRepository<PaymentMethodEntity, Long> {

    List<PaymentMethodEntity> findByActiveTrue();
}
