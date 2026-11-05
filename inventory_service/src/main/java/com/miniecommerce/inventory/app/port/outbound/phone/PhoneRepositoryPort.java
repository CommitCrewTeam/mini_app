package com.miniecommerce.inventory.app.port.outbound.phone;

import com.miniecommerce.inventory.domain.Phone;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportMatch;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PhoneRepositoryPort {

    Flux<Phone> findByKeys(List<String> keys);

    /** Insert when {@code id} is null; otherwise update only if the row still has this version. */
    Mono<Phone> save(Phone phone);

    /** Save a whole chunk sequentially; the first conflicting row stops it. */
    Mono<Void> saveChanges(List<Phone> phones);

    Flux<Phone> findAll();

    Mono<Integer> getStock(Long id);

    Flux<Phone> findByIds(List<Long> ids);

    Flux<PhoneImportMatch> findImportMatchesByKeys(List<String> keys);
}
