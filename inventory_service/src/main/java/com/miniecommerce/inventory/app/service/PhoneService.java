package com.miniecommerce.inventory.app.service;

import com.miniecommerce.inventory.app.port.inbound.phone.PhoneUseCase;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneRepositoryPort;
import com.miniecommerce.inventory.domain.Phone;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class PhoneService implements PhoneUseCase {

    private final PhoneRepositoryPort phoneRepository;

    public PhoneService(PhoneRepositoryPort phoneRepository) {
        this.phoneRepository = phoneRepository;
    }

    @Override
    public Flux<Phone> getAllPhones() {
        return phoneRepository.findAll();
    }

    @Override
    public Mono<Phone> createPhone(Phone phone) {
        phone.validate();
        return phoneRepository.save(phone);
    }

    @Override
    public Mono<Integer> getStock(Long id) {
        return phoneRepository.getStock(id);
    }

    @Override
    public Flux<Phone> getItemsForOrderPreview(List<Long> ids) {
        return phoneRepository.findByIds(ids);
    }
}
