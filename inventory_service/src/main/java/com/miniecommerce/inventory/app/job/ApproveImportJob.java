package com.miniecommerce.inventory.app.job;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneDraftRepositoryPort;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneImportRepositoryPort;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneRepositoryPort;
import com.miniecommerce.inventory.domain.ImportMode;
import com.miniecommerce.inventory.domain.Phone;
import com.miniecommerce.inventory.domain.PhoneDraft;
import com.miniecommerce.inventory.domain.PhoneDraftStatus;
import com.miniecommerce.inventory.domain.PhoneImport;
import com.miniecommerce.inventory.domain.PhoneImportStatus;
import jakarta.validation.constraints.Min;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.SynchronizedItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ApproveImportJob {
    private static final String IMPORT_ID = "importId";
    private static final String APPROVAL_VERSION = "approvalVersion";
    private static final String BLOCKED = "BLOCKED";
    private static final String ALL_VALID = "ALL_VALID";
    private static final String INVALID_ROWS = "INVALID_ROWS";

    public record ImportedPhone(Long draftId, Long matchedVersion, Phone phone) { }

    private final JobLauncher launcher;
    private final Job approvalJob;
    private final PhoneImportRepositoryPort imports;

    public ApproveImportJob(
            @Qualifier("phoneApprovalJobLauncher") JobLauncher launcher,
            @Qualifier("phoneApproveJob") Job approvalJob,
            PhoneImportRepositoryPort imports) {
        this.launcher = launcher;
        this.approvalJob = approvalJob;
        this.imports = imports;
    }

    public Long launchApprove(UUID id) throws Exception {
        var phoneImport = markImportAsApproving(imports, id).block();
        var parameters = new JobParametersBuilder()
                .addString(IMPORT_ID, id.toString())
                .addLong(APPROVAL_VERSION, phoneImport.getVersion())
                .addString("mode", phoneImport.getMode().name())
                .addString("requestId", UUID.randomUUID().toString())
                .toJobParameters();
        try {
            return launcher.run(approvalJob, parameters).getId();
        } catch (Exception e) {
            try {
                release(imports, id, phoneImport.getVersion()).block();
            } catch (Exception cleanupFailure) {
                e.addSuppressed(cleanupFailure);
            }
            throw e;
        }
    }

    private enum ApprovalOutcome { BLOCKED, DONE, FAILED }

    private static PhoneImportStatus statusAfter(ApprovalOutcome outcome) {
        return switch (outcome) {
            case BLOCKED -> PhoneImportStatus.READY;
            case DONE -> PhoneImportStatus.DONE;
            case FAILED -> PhoneImportStatus.FAILED;
        };
    }

    private static Mono<PhoneImport> markImportAsApproving(PhoneImportRepositoryPort imports, UUID id) {
        return imports.findById(id)
                .switchIfEmpty(Mono.error(new AppException(ErrorCode.NOT_FOUND, "Phone import not found")))
                .flatMap(phoneImport -> {
                    if (phoneImport.getStatus() != PhoneImportStatus.READY) {
                        return Mono.error(new AppException(ErrorCode.CONFLICT, "Import is not ready"));
                    }
                    phoneImport.transitionTo(PhoneImportStatus.APPROVING);
                    Long expectedVersion = Objects.requireNonNull(phoneImport.getVersion(), "Import version is required");
                    return imports.compareAndSetStatus(id, PhoneImportStatus.READY,
                                    expectedVersion, PhoneImportStatus.APPROVING)
                            .flatMap(statusUpdated -> {
                                if (!statusUpdated) {
                                    return Mono.error(new AppException(ErrorCode.CONFLICT,
                                            "Import changed or approval already started"));
                                }
                                phoneImport.setVersion(expectedVersion + 1);
                                return Mono.just(phoneImport);
                            });
                });
    }

    private static Mono<Void> release(PhoneImportRepositoryPort imports, UUID id, Long approvalVersion) {
        return transitionClaimedImport(imports, id, approvalVersion, PhoneImportStatus.READY);
    }

    private static Mono<Void> complete(PhoneImportRepositoryPort imports, UUID id,
            Long approvalVersion, ApprovalOutcome outcome) {
        return transitionClaimedImport(imports, id, approvalVersion, statusAfter(outcome));
    }

    private static Mono<Void> transitionClaimedImport(PhoneImportRepositoryPort imports, UUID id,
            Long approvalVersion, PhoneImportStatus status) {
        PhoneImportStatus.APPROVING.transitionTo(status);
        return imports.compareAndSetStatus(id, PhoneImportStatus.APPROVING, approvalVersion, status)
                .flatMap(updated -> updated ? Mono.<Void>empty()
                        : Mono.error(new AppException(ErrorCode.CONFLICT, "Approval ownership changed")));
    }

    private static ImportedPhone prepare(PhoneDraft draft) {
        if (draft.getStatus() != PhoneDraftStatus.VALID) {
            throw new IllegalStateException("Draft is no longer valid after approval validation");
        }
        var phone = Phone.fromImport(draft.getKey(), draft.getDetail(), draft.getQuantity());
        return new ImportedPhone(draft.getId(), draft.getMatchedVersion(), phone);
    }

    private static Mono<Void> applyChunk(PhoneDraftRepositoryPort drafts, PhoneRepositoryPort phones,
            UUID importId, ImportMode mode, List<? extends ImportedPhone> rows) {
        return Flux.fromIterable(rows)
                .concatMap(row -> drafts.findById(importId, row.draftId())
                        .switchIfEmpty(Mono.error(new IllegalStateException("Draft not found in import")))
                        .flatMap(draft -> {
                            if (draft.getStatus() != PhoneDraftStatus.VALID) {
                                return Mono.error(new IllegalStateException("Draft changed after validation"));
                            }
                            return loadChange(phones, mode, row)
                                    .flatMap(change -> executeChange(phones, change))
                                    .flatMap(saved -> {
                                        draft.transitionTo(PhoneDraftStatus.APPLIED);
                                        return drafts.saveAll(importId, List.of(draft));
                                    });
                        }))
                .then();
    }

    private record PhoneChange(Phone phone, Long expectedVersion) { }

    /** Decides the change from supplied data; no repository or Batch dependency. */
    private static PhoneChange selectChange(ImportMode mode, ImportedPhone row,
            Phone current, Long currentVersion) {
        if (mode == ImportMode.APPEND) {
            return new PhoneChange(row.phone(), null);
        }
        if (current == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "Phone not found");
        }
        if (row.matchedVersion() == null || !row.matchedVersion().equals(currentVersion)) {
            throw new AppException(ErrorCode.CONFLICT, "Phone changed after approval validation");
        }
        // Prepare a new value without mutating the supplied snapshot.
        Phone changed = new Phone(current.getId(), current.getName(), current.getDetail(),
                current.isActive(), current.getStock());
        switch (mode) {
            case UPDATE -> changed.updateFromImport(row.phone().getDetail());
            case RESTOCK -> changed.restock(row.phone().getStock());
            case APPEND -> throw new IllegalStateException("APPEND was handled before loading a snapshot");
        }
        return new PhoneChange(changed, row.matchedVersion());
    }

    private static Mono<PhoneChange> loadChange(PhoneRepositoryPort phones, ImportMode mode, ImportedPhone row) {
        if (mode == ImportMode.APPEND) {
            return Mono.fromSupplier(() -> selectChange(mode, row, null, null));
        }
        return phones.findVersionedByKey(row.phone().getName())
                .map(snapshot -> selectChange(mode, row, snapshot.phone(), snapshot.version()))
                .switchIfEmpty(Mono.error(new AppException(ErrorCode.NOT_FOUND, "Phone not found")));
    }

    private static Mono<Phone> executeChange(PhoneRepositoryPort phones, PhoneChange change) {
        return change.expectedVersion() == null
                ? phones.save(change.phone())
                : phones.save(change.phone(), change.expectedVersion());
    }

    @Validated
    @ConfigurationProperties("app.phone-import.approval")
    public record Settings(@DefaultValue("5") @Min(1) int workers,
                           @DefaultValue("500") @Min(1) int chunkSize) { }

    /** Batch wiring kept separate from the approval operations in this file. */
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(Settings.class)
    public static class ApprovalConfiguration {
        @Bean
        public SimpleAsyncTaskExecutor phoneApprovalExecutor(Settings settings) {
            var executor = new SimpleAsyncTaskExecutor("phone-approve-");
            executor.setVirtualThreads(true);
            executor.setConcurrencyLimit(settings.workers());
            return executor;
        }

        @Bean
        @StepScope
        public ItemReader<PhoneDraft> phoneApprovalReader(
                @Value("#{jobParameters['importId']}") String importId,
                Settings settings, PhoneDraftRepositoryPort drafts) {
            return new SynchronizedItemReader<>(
                    new PagedDraftReader(drafts, UUID.fromString(importId), settings.chunkSize()));
        }

        @Bean
        @StepScope
        public ItemWriter<ImportedPhone> phoneApprovalWriter(
                @Value("#{jobParameters['importId']}") String importId,
                @Value("#{jobParameters['mode']}") String mode,
                @Qualifier("inventoryTransactions") TransactionalOperator transactions,
                PhoneDraftRepositoryPort drafts, PhoneRepositoryPort phones) {
            UUID id = UUID.fromString(importId);
            ImportMode importMode = ImportMode.valueOf(mode);
            // The deferred publisher puts every phone write and draft acknowledgement inside
            // one R2DBC transaction. Draft saveAll joins it with REQUIRED propagation.
            return chunk -> Mono.defer(() -> applyChunk(drafts, phones, id, importMode, chunk.getItems()))
                    .as(transactions::transactional).block();
        }

        @Bean
        public Step phoneApprovalStep(JobRepository repository,
                @Qualifier("batchTransactionManager") PlatformTransactionManager transactionManager,
                @Qualifier("phoneApprovalReader") ItemReader<PhoneDraft> reader,
                @Qualifier("phoneApprovalWriter") ItemWriter<ImportedPhone> writer,
                @Qualifier("phoneApprovalExecutor") SimpleAsyncTaskExecutor approvalExecutor,
                Settings settings) {
            var transaction = new DefaultTransactionAttribute();
            transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
            return new StepBuilder("phoneApprovalStep", repository)
                    .<PhoneDraft, ImportedPhone>chunk(settings.chunkSize(), transactionManager)
                    .taskExecutor(approvalExecutor)
                    .transactionAttribute(transaction)
                    .reader(reader).processor(ApproveImportJob::prepare).writer(writer).build();
        }

        private static JobExecutionDecider phoneApprovalValidationGate(PhoneDraftRepositoryPort drafts) {
            return (execution, stepExecution) -> {
                UUID id = UUID.fromString(execution.getJobParameters().getString(IMPORT_ID));
                boolean allValid = Boolean.TRUE.equals(drafts.allValid(id).block());
                return new FlowExecutionStatus(allValid ? ALL_VALID : INVALID_ROWS);
            };
        }

        private static JobExecutionListener phoneApprovalListener(PhoneImportRepositoryPort imports) {
            return new JobExecutionListener() {
                @Override
                public void afterJob(JobExecution execution) {
                    UUID id = UUID.fromString(execution.getJobParameters().getString(IMPORT_ID));
                    ApprovalOutcome outcome = execution.getStatus() != BatchStatus.COMPLETED
                            ? ApprovalOutcome.FAILED
                            : BLOCKED.equals(execution.getExitStatus().getExitCode())
                                    ? ApprovalOutcome.BLOCKED : ApprovalOutcome.DONE;
                    Long approvalVersion = Objects.requireNonNull(
                            execution.getJobParameters().getLong(APPROVAL_VERSION), "Approval version is required");
                    complete(imports, id, approvalVersion, outcome).block();
                }
            };
        }

        @Bean
        public Job phoneApproveJob(JobRepository repository,
                @Qualifier("phoneImportValidateStep") Step validateStep,
                @Qualifier("phoneApprovalStep") Step approvalStep,
                PhoneDraftRepositoryPort drafts,
                PhoneImportRepositoryPort imports) {
            JobExecutionDecider validationGate = phoneApprovalValidationGate(drafts);
            return new JobBuilder("phoneApproveJob", repository)
                    .preventRestart()
                    .listener(phoneApprovalListener(imports))
                    .start(validateStep)
                    .next(validationGate).on(ALL_VALID).to(approvalStep)
                    .from(validationGate).on(INVALID_ROWS).end(BLOCKED)
                    .end()
                    .build();
        }
    }
    private static final class PagedDraftReader implements ItemReader<PhoneDraft> {
        private final PhoneDraftRepositoryPort drafts;
        private final UUID importId;
        private final int pageSize;
        private int pageNumber;
        private boolean exhausted;
        private Iterator<PhoneDraft> page = List.<PhoneDraft>of().iterator();

        private PagedDraftReader(PhoneDraftRepositoryPort drafts, UUID importId, int pageSize) {
            this.drafts = drafts;
            this.importId = importId;
            this.pageSize = pageSize;
        }

        @Override
        public PhoneDraft read() {
            if (exhausted) {
                return null;
            }
            if (!page.hasNext()) {
                // Page all stable import rows. Filtering VALID would skip rows as workers
                // mark earlier pages APPLIED and change the offsets of remaining rows.
                var rows = Objects.requireNonNull(drafts.findPage(importId, pageNumber, pageSize)
                        .collectList().block());
                if (rows.isEmpty()) {
                    exhausted = true;
                    return null;
                }
                pageNumber++;
                page = rows.iterator();
            }
            return page.next();
        }
    }
}
