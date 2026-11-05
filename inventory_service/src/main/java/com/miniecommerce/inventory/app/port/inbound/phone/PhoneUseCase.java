package com.miniecommerce.inventory.app.port.inbound.phone;

import com.miniecommerce.inventory.domain.Phone;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.List;

public interface PhoneUseCase {
    Mono<Phone> createPhone(Phone phone);

    Flux<Phone> getAllPhones();

    Flux<Phone> getItemsForOrderPreview(List<Long> ids);

    Mono<Integer> getStock(Long id);
}
