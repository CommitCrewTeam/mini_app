package com.miniecommerce.inventory.app.service;

import com.miniecommerce.inventory.app.port.outbound.phone.PhoneDraftRepositoryPort;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneImportRepositoryPort;
import com.miniecommerce.inventory.domain.PhoneDraft;
import com.miniecommerce.inventory.domain.PhoneImport;
import com.miniecommerce.common.exception.AppException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PhoneImportDetailTest {
    private final PhoneImportRepositoryPort imports = mock(PhoneImportRepositoryPort.class);
    private final PhoneDraftRepositoryPort drafts = mock(PhoneDraftRepositoryPort.class);
    private final PhoneImportApplicationService service = new PhoneImportApplicationService(
            null, null, null, imports, null, null, null, drafts);

    @Test
    void readsOnlyRequestedImportPageAndIncludesDraftFields() {
        var header = new PhoneImport();
        header.setId(42L);
        var draft = new PhoneDraft();
        draft.setId(5L);
        draft.setQuantity(0);
        draft.setErrorMessage("Phone changed");
        when(imports.findById(42L)).thenReturn(Mono.just(header));
        when(drafts.findPage(42L, 1, 20)).thenReturn(Flux.just(draft));
        when(drafts.countByImportId(42L)).thenReturn(Mono.just(21L));
        StepVerifier.create(service.getDetail(42L, 1, 20)).assertNext(result -> {
            assertEquals(42L, result.header().getId());
            assertEquals(2, result.totalPages());
            assertEquals(1, result.page());
            assertEquals(0, result.content().getFirst().getQuantity());
            assertEquals("Phone changed", result.content().getFirst().getErrorMessage());
        }).verifyComplete();
    }

    @Test
    void missingImportDoesNotQueryDrafts() {
        when(imports.findById(42L)).thenReturn(Mono.empty());
        StepVerifier.create(service.getDetail(42L, 0, 20)).expectError(AppException.class).verify();
        verifyNoInteractions(drafts);
    }

    @Test
    void rejectsInvalidPaginationBeforeDatabaseAccess() {
        StepVerifier.create(service.getDetail(42L, -1, 20)).expectError(AppException.class).verify();
        StepVerifier.create(service.getDetail(42L, 0, 101)).expectError(AppException.class).verify();
        StepVerifier.create(service.getDetail(42L, 0, 0)).expectError(AppException.class).verify();
        verifyNoInteractions(imports, drafts);
    }
}
