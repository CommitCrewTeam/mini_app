package com.miniecommerce.inventory.app.service;

import java.util.UUID;
import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import com.miniecommerce.inventory.app.port.inbound.phone.PhoneImportUseCase;
import com.miniecommerce.inventory.app.job.SpringBatchPhoneImportJob;
import com.miniecommerce.inventory.app.job.ApproveImportJob;
import com.miniecommerce.inventory.app.job.PhoneImportValidationJob;
import com.miniecommerce.inventory.domain.PhoneImportUpload;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Path;
import java.util.Locale;
import java.nio.file.Files;

@Service
public class PhoneImportApplicationService implements PhoneImportUseCase {
    private final SpringBatchPhoneImportJob importJob;
    private final ApproveImportJob approveJob;
    private final PhoneImportValidationJob validationJob;
    private final long maxBytes;

    public PhoneImportApplicationService(SpringBatchPhoneImportJob importJob,
            ApproveImportJob approveJob,
            PhoneImportValidationJob validationJob,
            @Value("${app.phone-import.max-bytes:20971520}") long maxBytes) {
        this.importJob = importJob;
        this.approveJob = approveJob;
        this.validationJob = validationJob;
        this.maxBytes = maxBytes;
    }

    @Override
    public Mono<Long> validate(UUID importId) {
        return Mono.fromCallable(() -> validationJob.launchValidation(importId))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Long> approve(UUID importId) {
        return Mono.fromCallable(() -> approveJob.launchApprove(importId))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UUID> importFile(PhoneImportUpload upload) {
        return saveFile(upload)
                .flatMap(path -> softValidate(upload.filename(), path))
                .flatMap(path -> Mono.fromCallable(() -> importJob.launchPhoneImportJob(path, upload.mode()))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /** Soft validation currently checks only filename and actual uploaded file size. */
    private Mono<Path> softValidate(String filename, Path file) {
        return Mono.fromCallable(() -> {
            if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Upload a .csv file");
            }
            long size = Files.size(file);
            if (size == 0 || size > maxBytes) {
                throw new AppException(ErrorCode.BAD_REQUEST,
                        "CSV must be nonempty and at most " + maxBytes + " bytes");
            }
            return file;
        }).subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(error -> delete(file).then(Mono.error(error)));
    }

    private Mono<Path> saveFile(PhoneImportUpload upload) {
        // Cleanup only covers transfer. A saved file is retained for the background job.
        return Mono.usingWhen(
                Mono.fromCallable(importJob::allocateImportFile).subscribeOn(Schedulers.boundedElastic()),
                path -> Mono.defer(() -> upload.transferTo().apply(path)).thenReturn(path),
                path -> Mono.empty(),
                (path, error) -> delete(path),
                this::delete);
    }

    private Mono<Void> delete(Path path) {
        return Mono.fromCallable(() -> Files.deleteIfExists(path))
                .subscribeOn(Schedulers.boundedElastic()).then();
    }
}
