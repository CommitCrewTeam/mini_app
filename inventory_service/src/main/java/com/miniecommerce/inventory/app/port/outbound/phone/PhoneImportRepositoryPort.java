package com.miniecommerce.inventory.app.port.outbound.phone;

import com.miniecommerce.common.query.Filter;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportStat;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportStatuses;

import com.miniecommerce.inventory.domain.PhoneImport;
import com.miniecommerce.inventory.domain.enums.PhoneDraftStatus;
import com.miniecommerce.inventory.domain.enums.PhoneImportStatus;
import java.util.List;
import java.util.Collection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PhoneImportRepositoryPort {

    Mono<PhoneImportStat> getStat(Long importId);

    Mono<PhoneImport> save(PhoneImport phoneImport);

    Mono<Boolean> writeStatusIfStatusIn(Long id, Collection<PhoneImportStatus> statuses,
            PhoneImportStatus nextStatus);
    
    Mono<PhoneImport> findById(Long id);

    Flux<PhoneImport> findPage(List<Filter> filters, int page, int size);

    Mono<Long> count(List<Filter> filters);
}
