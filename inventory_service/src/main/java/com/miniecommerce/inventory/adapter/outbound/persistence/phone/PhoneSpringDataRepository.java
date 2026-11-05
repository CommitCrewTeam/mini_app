package com.miniecommerce.inventory.adapter.outbound.persistence.phone;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import java.util.Collection;
import reactor.core.publisher.Flux;

public interface PhoneSpringDataRepository
        extends ReactiveCrudRepository<PhoneEntity, Long> {

    reactor.core.publisher.Mono<PhoneEntity> findByKey(String key);

    Flux<ImportMatch> findAllByKeyIn(Collection<String> keys);

    interface ImportMatch {
        Long getId();
        String getKey();
        Long getVersion();
    }
}
