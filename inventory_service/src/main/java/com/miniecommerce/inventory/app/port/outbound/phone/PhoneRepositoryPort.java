package com.miniecommerce.inventory.app.port.outbound.phone;

import com.miniecommerce.inventory.domain.Phone;
import com.miniecommerce.inventory.domain.PhoneImportMatch;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PhoneRepositoryPort {

    record VersionedPhone(Phone phone, Long version) { }

    Mono<VersionedPhone> findVersionedByKey(String key);

    Mono<Phone> save(Phone phone, Long expectedVersion);

    Flux<Phone> findAll();

    Mono<Phone> save(Phone phone);

    Mono<Integer> getStock(Long id);

    Flux<Phone> findByIds(List<Long> ids);

    Flux<PhoneImportMatch> findImportMatchesByKeys(List<String> keys);
}
