package com.miniecommerce.inventory.app.port.inbound;

import com.miniecommerce.inventory.domain.Phone;
import reactor.core.publisher.Mono;

public interface CreatePhoneUseCase {
    Mono<Phone> createPhone(Phone phone);
}
