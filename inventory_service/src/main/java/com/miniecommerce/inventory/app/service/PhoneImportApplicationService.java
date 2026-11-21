package com.miniecommerce.inventory.app.service;

import com.miniecommerce.inventory.app.command.ImportPhonesCommand;
import com.miniecommerce.inventory.app.command.ListPhoneImportsCommand;
import com.miniecommerce.inventory.app.mapper.PhoneImportListMapper;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportPage;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportStat;
import com.miniecommerce.inventory.domain.PhoneImportPojo.JobSubmission;
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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PhoneImportApplicationService implements PhoneImportUseCase {

    private final SpringBatchPhoneImportJob importJob;
    private final ApproveImportJob approveJob;
    private final PhoneImportValidationJob validationJob;
    private final PhoneImportRepositoryPort phoneImportRepository;
    private final PhoneImportListMapper phoneImportListMapper;
    private final FileUtil fileUtil;
    private final BlockingTasks blockingTasks;

    public PhoneImportApplicationService(SpringBatchPhoneImportJob importJob,
            ApproveImportJob approveJob,
            PhoneImportValidationJob validationJob,
            PhoneImportRepositoryPort phoneImportRepository,
            PhoneImportListMapper phoneImportListMapper,
            FileUtil fileUtil,
            BlockingTasks blockingTasks) {
        this.importJob = importJob;
        this.approveJob = approveJob;
        this.validationJob = validationJob;
        this.phoneImportRepository = phoneImportRepository;
        this.phoneImportListMapper = phoneImportListMapper;
        this.fileUtil = fileUtil;
        this.blockingTasks = blockingTasks;
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
                .transform(blockingTasks::continueOnBlockingThread)
                .map(phoneImport -> validationJob.submitValidation(importId, phoneImport.getMode()));
    }

    @Override
    public Mono<Long> approve(Long importId) {
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
                .transform(blockingTasks::continueOnBlockingThread)
                .map(phoneImport -> approveJob.submit(importId, phoneImport.getMode()).getId());
    }

    @Override
    public Mono<JobSubmission> importFile(ImportPhonesCommand command) {
        return phoneImportRepository.save(PhoneImport.pending(command.mode()))
                .transform(blockingTasks::continueOnBlockingThread)
                .map(saved -> new JobSubmission(saved.getId(),
                        importJob.submit(saved, command.file()).getId()))
                .doOnError(error -> fileUtil.deleteQuietly(command.file()));
    }
}
