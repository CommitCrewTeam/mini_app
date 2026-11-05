package com.miniecommerce.inventory.adapter.inbound.rest;

import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportStat;
import com.miniecommerce.inventory.domain.PhoneImportPojo.JobSubmission;

import com.miniecommerce.common.response.ApiResponse;
import com.miniecommerce.inventory.app.port.inbound.phone.PhoneImportUseCase;
import com.miniecommerce.inventory.domain.enums.ImportMode;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportUpload;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/phones/imports")
public class PhoneImportController {
    private final PhoneImportUseCase phoneImportUseCase;

    public PhoneImportController(PhoneImportUseCase phoneImportUseCase) {
        this.phoneImportUseCase = phoneImportUseCase;
    }

    @GetMapping("/{importId}/stat")
    public Mono<ApiResponse<PhoneImportStat>> getStat(@PathVariable Long importId) {
        return phoneImportUseCase.getStat(importId)
                .map(stat -> ApiResponse.success(stat, "Phone import stat retrieved"));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<ApiResponse<JobSubmission>> upload(@RequestPart("file") FilePart file,
                                                @RequestPart("mode") String mode) {
        ImportMode importMode;
        try {
            importMode = ImportMode.valueOf(mode);
        } catch (IllegalArgumentException e) {
            throw new com.miniecommerce.common.exception.AppException(
                    com.miniecommerce.common.exception.ErrorCode.BAD_REQUEST, "Mode must be CREATE, UPDATE or RESTOCK");
        }
        return phoneImportUseCase.importFile(new PhoneImportUpload(file.filename(), importMode, file::transferTo))
                .map(id -> ApiResponse.success(id, "Phone import submitted"));
    }

    @PostMapping("/{importId}/validate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<ApiResponse<Long>> validate(@PathVariable Long importId) {
        return phoneImportUseCase.validate(importId)
                .map(id -> ApiResponse.success(id, "Phone validation submitted"));
    }

    @PostMapping("/{importId}/approve")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<ApiResponse<JobSubmission>> approve(@PathVariable Long importId) {
        return phoneImportUseCase.approve(importId)
                .map(id -> ApiResponse.success(id, "Phone approval submitted"));
    }

}
