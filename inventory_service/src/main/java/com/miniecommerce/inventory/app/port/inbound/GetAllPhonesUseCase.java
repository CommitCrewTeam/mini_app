package com.miniecommerce.inventory.app.port.inbound;

import com.miniecommerce.inventory.domain.Phone;
import reactor.core.publisher.Flux;

public interface GetAllPhonesUseCase {
    Flux<Phone> getAllPhones();
}
