package com.miniecommerce.inventory.app.port.inbound.phone;

import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportDetail;

import com.miniecommerce.inventory.app.command.ImportPhonesCommand;
import com.miniecommerce.inventory.app.command.ApprovePhoneImportCommand;
import com.miniecommerce.inventory.app.command.ListPhoneImportsCommand;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportPage;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportStat;

import reactor.core.publisher.Mono;

public interface PhoneImportUseCase {
    Mono<PhoneImportDetail> getDetail(Long importId, int page, int size);
    Mono<PhoneImportStat> getStat(Long importId);

    /** Command is built and validated by the inbound adapter. */
    Mono<PhoneImportPage> list(ListPhoneImportsCommand command);

    //    async
    Mono<Long> validate(Long importId);

    // async
    Mono<Long> approve(ApprovePhoneImportCommand command);

    // async; the command owns a temp file until the background job finishes
    Mono<Long> importFile(ImportPhonesCommand command);
}
