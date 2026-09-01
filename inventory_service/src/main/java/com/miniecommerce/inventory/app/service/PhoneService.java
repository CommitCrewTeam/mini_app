package com.miniecommerce.inventory.app.service;

import com.miniecommerce.inventory.app.port.inbound.CreatePhoneUseCase;
import com.miniecommerce.inventory.app.port.inbound.GetAllPhonesUseCase;
import com.miniecommerce.inventory.app.port.inbound.GetItemsForOrderPreviewUseCase;
import com.miniecommerce.inventory.app.port.inbound.GetPhoneStockUseCase;
import com.miniecommerce.inventory.app.port.outbound.PhoneRepository;
import com.miniecommerce.inventory.domain.Phone;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class PhoneService implements GetAllPhonesUseCase, CreatePhoneUseCase, GetPhoneStockUseCase, GetItemsForOrderPreviewUseCase {

    private final PhoneRepository phoneRepository;

    public PhoneService(PhoneRepository phoneRepository) {
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
