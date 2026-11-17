package com.miniecommerce.inventory.app.port.inbound.phone;

import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportStat;
import com.miniecommerce.inventory.domain.PhoneImportPojo.JobSubmission;

import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportUpload;

import reactor.core.publisher.Mono;

public interface PhoneImportUseCase {
    Mono<PhoneImportStat> getStat(Long importId);

    //    async
    Mono<Long> validate(Long importId);

    // async
    Mono<JobSubmission> approve(Long importId);

    // async
    Mono<JobSubmission> importFile(PhoneImportUpload upload);
}
