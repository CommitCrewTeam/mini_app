package com.miniecommerce.inventory.adapter.inbound.rest;

import java.util.UUID;
import com.miniecommerce.common.response.ApiResponse;
import com.miniecommerce.inventory.app.port.inbound.phone.PhoneImportUseCase;
import com.miniecommerce.inventory.domain.PhoneImportUpload;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import com.miniecommerce.inventory.domain.ImportMode;

@RestController
@RequestMapping("/api/phones/imports")
public class PhoneImportController {
    private final PhoneImportUseCase phoneImportUseCase;

    public PhoneImportController(PhoneImportUseCase phoneImportUseCase) {
        this.phoneImportUseCase = phoneImportUseCase;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<ApiResponse<UUID>> upload(@RequestPart("file") FilePart file,
                                                @RequestPart("mode") String mode) {
        ImportMode importMode;
        try {
            importMode = ImportMode.valueOf(mode);
        } catch (IllegalArgumentException e) {
            throw new com.miniecommerce.common.exception.AppException(
                    com.miniecommerce.common.exception.ErrorCode.BAD_REQUEST, "Mode must be APPEND, UPDATE or RESTOCK");
        }
        return phoneImportUseCase.importFile(new PhoneImportUpload(file.filename(), importMode, file::transferTo))
                .map(id -> ApiResponse.success(id, "Phone import submitted"));
    }

    @PostMapping("/{importId}/validate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<ApiResponse<Long>> validate(@PathVariable UUID importId) {
        return phoneImportUseCase.validate(importId)
                .map(id -> ApiResponse.success(id, "Phone validation submitted"));
    }

    @PostMapping("/{importId}/approve")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<ApiResponse<Long>> approve(@PathVariable UUID importId) {
        return phoneImportUseCase.approve(importId)
                .map(id -> ApiResponse.success(id, "Phone approval submitted"));
    }

}
