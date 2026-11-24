package com.miniecommerce.inventory.app.service;

import com.miniecommerce.inventory.app.port.outbound.phone.PhoneDraftRepositoryPort;

import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportDetail;

import com.miniecommerce.inventory.app.command.ApprovePhoneImportCommand;
import com.miniecommerce.inventory.app.command.ImportPhonesCommand;
import com.miniecommerce.inventory.app.command.ListPhoneImportsCommand;
import com.miniecommerce.inventory.app.mapper.PhoneImportListMapper;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportApprovalContext;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportIngestionContext;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportPage;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportStat;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportValidationContext;
import com.miniecommerce.inventory.domain.PhoneImport;
import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import com.miniecommerce.common.storage.FileUtil;
import com.miniecommerce.inventory.app.job.ApproveImportJob;
import com.miniecommerce.inventory.app.job.PhoneImportValidationJob;
import com.miniecommerce.inventory.app.job.SpringBatchPhoneImportJob;
import com.miniecommerce.inventory.app.port.inbound.phone.PhoneImportUseCase;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneImportRepositoryPort;
import com.miniecommerce.inventory.utils.BlockingTasks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PhoneImportApplicationService implements PhoneImportUseCase {
    private static final Logger log = LoggerFactory.getLogger(PhoneImportApplicationService.class);

    private final SpringBatchPhoneImportJob importJob;
    private final ApproveImportJob approveJob;
    private final PhoneImportValidationJob validationJob;
    private final PhoneImportRepositoryPort phoneImportRepository;
    private final PhoneImportListMapper phoneImportListMapper;
    private final FileUtil fileUtil;
    private final BlockingTasks blockingTasks;
    private final PhoneDraftRepositoryPort drafts;

    public PhoneImportApplicationService(SpringBatchPhoneImportJob importJob,
            ApproveImportJob approveJob,
            PhoneImportValidationJob validationJob,
            PhoneImportRepositoryPort phoneImportRepository,
            PhoneImportListMapper phoneImportListMapper,
            FileUtil fileUtil,
            BlockingTasks blockingTasks,
            PhoneDraftRepositoryPort drafts) {
        this.drafts = drafts;
        this.importJob = importJob;
        this.approveJob = approveJob;
        this.validationJob = validationJob;
        this.phoneImportRepository = phoneImportRepository;
        this.phoneImportListMapper = phoneImportListMapper;
        this.fileUtil = fileUtil;
        this.blockingTasks = blockingTasks;
    }

    @Override
    public Mono<PhoneImportDetail> getDetail(Long importId, int page, int size) {
        if (importId == null || importId <= 0 || page < 0 || size < 1 || size > 100) {
            return Mono.error(new AppException(ErrorCode.BAD_REQUEST, "Require positive importId, page >= 0 and size between 1 and 100"));
        }
        return phoneImportRepository.findById(importId)
                .switchIfEmpty(Mono.error(new AppException(ErrorCode.NOT_FOUND, "Phone import not found")))
                .flatMap(header -> drafts.findPage(importId, page, size).collectList()
                        .zipWith(drafts.countByImportId(importId))
                        .map(result -> new PhoneImportDetail(
                                header, result.getT1(), page, size, result.getT2(), (result.getT2() + size - 1) / size)));
    }

    @Override
    public Mono<PhoneImportStat> getStat(Long importId) {
        return phoneImportRepository.getStat(importId)
                .switchIfEmpty(Mono.error(new AppException(ErrorCode.NOT_FOUND, "Phone import not found")));
    }

    @Override
    public Mono<PhoneImportPage> list(ListPhoneImportsCommand command) {
        return phoneImportRepository.findPage(command.filters(), command.page(), command.size())
                .map(phoneImportListMapper::toListItem)
                .collectList()
                .zipWith(phoneImportRepository.count(command.filters()))
                .map(tuple -> PhoneImportPage.of(tuple.getT1(), command.page(), command.size(), tuple.getT2()));
    }

    @Override
    public Mono<Long> validate(Long importId) {
        var transition = PhoneImport.validationStartTransition();
        return phoneImportRepository.writeStatusIfStatusIn(importId,
                        transition.allowedStatuses(), transition.nextStatus())
                .flatMap(transitioned -> {
                    if (!transitioned) {
                        return Mono.error(new AppException(ErrorCode.CONFLICT, "Import is not ready"));
                    }
                    return phoneImportRepository.findById(importId)
                            .switchIfEmpty(Mono.error(new AppException(
                                    ErrorCode.NOT_FOUND, "Phone import not found")));
                })
                .flatMap(phoneImport -> Mono.just(importId)
                        .doAfterTerminate(() -> blockingTasks.runInBackground(
                                () -> validationJob.submitValidation(
                                        new PhoneImportValidationContext(importId, phoneImport.getMode())),
                                error -> log.error("Cannot submit validation for import {}", importId, error))));
    }

    @Override
    public Mono<Long> approve(ApprovePhoneImportCommand command) {
        Long importId = command.importId();
        var transition = PhoneImport.approvalStartTransition();
        return phoneImportRepository.writeStatusIfStatusIn(importId,
                        transition.allowedStatuses(), transition.nextStatus())
                .flatMap(transitioned -> {
                    if (!transitioned) {
                        return Mono.error(new AppException(ErrorCode.CONFLICT, "Import cannot be approved"));
                    }
                    return phoneImportRepository.findById(importId)
                            .switchIfEmpty(Mono.error(new AppException(
                                    ErrorCode.NOT_FOUND, "Phone import not found")));
                })
                .flatMap(phoneImport -> Mono.just(importId)
                        .doAfterTerminate(() -> blockingTasks.runInBackground(
                                () -> approveJob.submit(new PhoneImportApprovalContext(
                                        importId, phoneImport.getMode(), command.warningSkip())),
                                error -> log.error("Cannot submit approval for import {}", importId, error))));
    }

    @Override
    public Mono<Long> importFile(ImportPhonesCommand command) {
        return phoneImportRepository.save(PhoneImport.uploading(command.mode()))
                .flatMap(saved -> Mono.just(saved.getId())
                        // Emit the ID and completion before handing off job submission.
                        .doAfterTerminate(() -> blockingTasks.runInBackground(
                                () -> importJob.submit(new PhoneImportIngestionContext(saved, command.file())),
                                error ->
                                        log.error("Cannot submit import {}", saved.getId(), error))))
                .doOnError(error -> fileUtil.deleteQuietly(command.file()));
    }

}
