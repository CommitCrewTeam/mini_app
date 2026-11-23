package com.miniecommerce.inventory.adapter.inbound.rest;

import com.miniecommerce.common.response.ApiResponse;
import com.miniecommerce.inventory.adapter.inbound.rest.dto.PhoneImportFilterRequest;
import com.miniecommerce.inventory.adapter.inbound.rest.dto.ApprovePhoneImportRequest;
import com.miniecommerce.inventory.adapter.inbound.rest.utils.PhoneImportQueryParser;
import com.miniecommerce.common.storage.UploadTempFileStagerReactor;
import com.miniecommerce.inventory.app.command.ImportPhonesCommand;
import com.miniecommerce.inventory.app.command.ApprovePhoneImportCommand;
import com.miniecommerce.inventory.app.command.ListPhoneImportsCommand;
import com.miniecommerce.inventory.app.port.inbound.phone.PhoneImportUseCase;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportPage;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportStat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/phones/imports")
public class PhoneImportController {
    private final PhoneImportUseCase phoneImportUseCase;
    private final PhoneImportQueryParser queryParser;
    private final UploadTempFileStagerReactor stager;

    public PhoneImportController(PhoneImportUseCase phoneImportUseCase,
            PhoneImportQueryParser queryParser,
            UploadTempFileStagerReactor stager) {
        this.phoneImportUseCase = phoneImportUseCase;
        this.queryParser = queryParser;
        this.stager = stager;
    }

    @PostMapping
    public Mono<ApiResponse<PhoneImportPage>> list(
            @RequestBody PhoneImportFilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ListPhoneImportsCommand command = new ListPhoneImportsCommand(queryParser.parse(request.filters()), page, size);
        return phoneImportUseCase.list(command)
                .map(result -> ApiResponse.success(result, "Phone imports retrieved"));
    }

    @GetMapping("/{importId}/stat")
    public Mono<ApiResponse<PhoneImportStat>> getStat(@PathVariable Long importId) {
        return phoneImportUseCase.getStat(importId)
                .map(stat -> ApiResponse.success(stat, "Phone import stat retrieved"));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<ApiResponse<Long>> upload(@RequestPart("file") FilePart file,
                                                 @RequestPart("mode") String mode) {
        // Web adapter only translates transport types (FilePart -> temp Path, raw values -> command).
        // It owns the random temp file until the command is handed to the use case.
        // After that the service cleans up on failure and the background job cleans up on completion.
        return stager.stage(file, "phones-", ".csv", f -> ImportPhonesCommand.from(f, mode))
                .flatMap(phoneImportUseCase::importFile)
                .map(submission -> ApiResponse.success(submission, "Phone import submitted"));
    }

    @PostMapping("/{importId}/validate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<ApiResponse<Long>> validate(@PathVariable Long importId) {
        return phoneImportUseCase.validate(importId)
                .map(id -> ApiResponse.success(id, "Phone validation submitted"));
    }

    @PostMapping("/{importId}/approve")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<ApiResponse<Long>> approve(@PathVariable Long importId,
            @RequestBody(required = false) ApprovePhoneImportRequest request) {
        boolean warningSkip = request != null && request.warningSkip();
        return phoneImportUseCase.approve(new ApprovePhoneImportCommand(importId, warningSkip))
                .map(id -> ApiResponse.success(id, "Phone approval submitted"));
    }
}
