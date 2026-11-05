package com.miniecommerce.inventory.app.service;

import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportStat;
import com.miniecommerce.inventory.domain.PhoneImportPojo.JobSubmission;
import com.miniecommerce.inventory.domain.PhoneImport;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import com.miniecommerce.inventory.app.job.ApproveImportJob;
import com.miniecommerce.inventory.app.job.PhoneImportValidationJob;
import com.miniecommerce.inventory.app.job.SpringBatchPhoneImportJob;
import com.miniecommerce.inventory.app.port.inbound.phone.PhoneImportUseCase;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneImportRepositoryPort;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportUpload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Service
public class PhoneImportApplicationService implements PhoneImportUseCase {
    private final SpringBatchPhoneImportJob importJob;
    private final ApproveImportJob approveJob;
    private final PhoneImportValidationJob validationJob;
    private final long maxBytes;
    private final PhoneImportFileStorage fileStorage;
    private final PhoneImportRepositoryPort phoneImportRepository;

    public PhoneImportApplicationService(SpringBatchPhoneImportJob importJob,
            ApproveImportJob approveJob,
            PhoneImportValidationJob validationJob,
            PhoneImportRepositoryPort phoneImportRepository,
            PhoneImportFileStorage fileStorage,
            @Value("${app.phone-import.max-bytes:20971520}") long maxBytes) {
        this.importJob = importJob;
        this.approveJob = approveJob;
        this.validationJob = validationJob;
        this.maxBytes = maxBytes;
        this.fileStorage = fileStorage;
        this.phoneImportRepository = phoneImportRepository;
    }

    @Override
    public Mono<PhoneImportStat> getStat(Long importId) {
        return phoneImportRepository.getStat(importId)
                .switchIfEmpty(Mono.error(new AppException(ErrorCode.NOT_FOUND, "Phone import not found")));
    }

    @Override
    public Mono<Long> validate(Long importId) {
        return Mono.fromCallable(() -> validationJob.launchValidation(importId))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<JobSubmission> approve(Long importId) {
        return phoneImportRepository.findById(importId)
                .switchIfEmpty(Mono.error(new AppException(ErrorCode.NOT_FOUND, "Phone import not found")))
                .flatMap(phoneImport -> {
                    phoneImport.startApproval();
                    return phoneImportRepository.save(phoneImport);
                })
                .flatMap(savedImport -> Mono.fromCallable(() -> approveJob.submit(savedImport))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(execution -> new JobSubmission(savedImport.getId(), execution.getId())));
    }

    @Override
    public Mono<JobSubmission> importFile(PhoneImportUpload upload) {
        return saveFile(upload)
                .flatMap(path -> softValidate(upload.filename(), path)
                        .flatMap(validPath -> phoneImportRepository.save(PhoneImport.pending(upload.mode()))
                                .flatMap(savedImport -> Mono.fromCallable(() -> importJob.submit(savedImport, validPath))
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .map(execution -> new JobSubmission(savedImport.getId(), execution.getId()))))
                        .onErrorResume(error -> delete(path)
                                .onErrorResume(cleanupError -> {
                                    error.addSuppressed(cleanupError);
                                    return Mono.empty();
                                })
                                .then(Mono.error(error))));
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
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Path> saveFile(PhoneImportUpload upload) {
        // Cleanup only covers transfer. A saved file is retained for the background job.
        return Mono.usingWhen(
                Mono.fromCallable(fileStorage::allocate).subscribeOn(Schedulers.boundedElastic()),
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
