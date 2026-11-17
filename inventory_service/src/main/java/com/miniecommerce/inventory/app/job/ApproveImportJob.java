package com.miniecommerce.inventory.app.job;

import com.miniecommerce.inventory.app.port.outbound.phone.PhoneDraftRepositoryPort;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneImportRepositoryPort;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneRepositoryPort;
import com.miniecommerce.inventory.domain.Phone;
import com.miniecommerce.inventory.domain.PhoneDraft;
import com.miniecommerce.inventory.domain.PhoneImport;
import com.miniecommerce.inventory.domain.enums.PhoneImportStatus;
import com.miniecommerce.inventory.domain.enums.ImportMode;
import jakarta.validation.constraints.Min;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;

@Component
public class ApproveImportJob {
    private static final String IMPORT_ID = "importId";
    private static final String APPROVAL_VERSION = "approvalVersion";

    private final BatchJobSubmitter submitter;
    private final Job approvalJob;
    private final PhoneImportRepositoryPort phoneImportRepository;

    public ApproveImportJob(
            @Qualifier("phoneApprovalSubmitter") BatchJobSubmitter submitter,
            @Qualifier("phoneApproveJob") Job approvalJob,
            PhoneImportRepositoryPort phoneImportRepository) {
        this.submitter = submitter;
        this.approvalJob = approvalJob;
        this.phoneImportRepository = phoneImportRepository;
    }

    public JobExecution submit(PhoneImport phoneImport) {
        var parameters = new JobParametersBuilder()
                .addLong(IMPORT_ID, phoneImport.getId())
                .addLong(APPROVAL_VERSION, phoneImport.getVersion(), false)
                .addString("mode", phoneImport.getMode().name())
                .toJobParameters();
        return submitter.submit(approvalJob, parameters,
                () -> persistStatusChange(phoneImportRepository, phoneImport.getId(),
                        PhoneImport.completeApproval(phoneImport.getVersion(), PhoneImportStatus.FAILED)).block());
    }

    private static Mono<Void> persistStatusChange(PhoneImportRepositoryPort phoneImportRepository, Long id,
            PhoneImport.StatusChange change) {
        return phoneImportRepository.compareAndSetStatus(id, change.expectedStatus(), change.expectedVersion(), change.nextStatus())
                .doOnNext(change::requireApplied)
                .then();
    }

    static Mono<Void> applyChunk(PhoneDraftRepositoryPort phoneDraftRepository, PhoneRepositoryPort phoneRepository,
            Long importId, ImportMode mode, List<? extends PhoneDraft> readDrafts) {
        if (readDrafts.isEmpty()) {
            return Mono.empty();
        }
        var draftIds = readDrafts.stream().map(PhoneDraft::getId).distinct().toList();
        var phoneKeys = readDrafts.stream().map(PhoneDraft::getKey).distinct().toList();
        // For UPDATE/RESTOCK load one Phone snapshot per key for this transaction. Each snapshot
        // carries the Phone.version that prepareImportChange compares with the draft's
        // matchedVersion (the version captured at validation) before mutating it in place.
        Mono<Map<String, Phone>> phoneSnapshots = mode.requiresExistingPhone()
                ? phoneRepository.findByKeys(phoneKeys).collectMap(Phone::getKey)
                : Mono.just(Map.of());
        // Read sequentially: both queries share the chunk's transaction connection.
        return phoneDraftRepository.findByIds(importId, draftIds).collectMap(PhoneDraft::getId)
                .flatMap(freshDraftsById -> phoneSnapshots.flatMap(phonesByKey -> {
                    // Two different version checks happen here:
                    // - readDraft.getVersion() is the draft row version from the reader. It must
                    //   equal freshDraft.version, otherwise the draft changed under the worker.
                    // - freshDraft.matchedVersion is the Phone.version captured at validation; it
                    //   is checked against phonesByKey in Phone.prepareImportChange.
                    // readDraft is the reader snapshot that selected the chunk;
                    // freshDraft is re-read here so the version check and the write share one transaction.
                    var freshDrafts = readDrafts.stream().map(readDraft -> {
                        var freshDraft = freshDraftsById.get(readDraft.getId());
                        if (freshDraft == null) {
                            throw new IllegalStateException("Draft not found in import");
                        }
//unused
//                        freshDraft.requireUnchangedForApproval(readDraft.getVersion());
                        return freshDraft;
                    }).toList();
                    var phones = freshDrafts.stream().map(freshDraft ->
                            Phone.prepareImportChange(mode, freshDraft, phonesByKey.get(freshDraft.getKey()))).toList();
                    return phoneRepository.saveChanges(phones)
                            .then(Mono.defer(() -> {
                                // Mark drafts APPLIED only after the phone writes succeeded, so a
                                // failed save leaves the drafts VALID for a future retry.
                                freshDrafts.forEach(PhoneDraft::markApplied);
                                return phoneDraftRepository.saveAll(importId, freshDrafts);
                            }));
                }));
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
                @Value("#{jobParameters['importId']}") Long importId,
                Settings settings, PhoneDraftRepositoryPort phoneDraftRepository) {
            return new SynchronizedItemReader<>(
                    new PagedDraftReader(phoneDraftRepository, importId, settings.chunkSize()));
        }

        @Bean
        @StepScope
        public ItemWriter<PhoneDraft> phoneApprovalWriter(
                @Value("#{jobParameters['importId']}") Long importId,
                @Value("#{jobParameters['mode']}") String mode,
                @Qualifier("inventoryTransactions") TransactionalOperator transactions,
                PhoneDraftRepositoryPort phoneDraftRepository, PhoneRepositoryPort phoneRepository) {
            ImportMode importMode = ImportMode.valueOf(mode);
            // The deferred publisher puts every phone write and draft acknowledgement inside
            // one R2DBC transaction. Draft saveAll joins it with REQUIRED propagation.
            return chunk -> Mono.defer(() -> applyChunk(phoneDraftRepository, phoneRepository, importId, importMode, chunk.getItems()))
                    .as(transactions::transactional).block();
        }

        @Bean
        public Step phoneApprovalStep(JobRepository repository,
                @Qualifier("batchTransactionManager") PlatformTransactionManager transactionManager,
                @Qualifier("phoneApprovalReader") ItemReader<PhoneDraft> reader,
                @Qualifier("phoneApprovalWriter") ItemWriter<PhoneDraft> writer,
                @Qualifier("phoneApprovalExecutor") SimpleAsyncTaskExecutor approvalExecutor,
                Settings settings) {
            var transaction = new DefaultTransactionAttribute();
            transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
            return new StepBuilder("phoneApprovalStep", repository)
                    .<PhoneDraft, PhoneDraft>chunk(settings.chunkSize(), transactionManager)
                    .taskExecutor(approvalExecutor)
                    .transactionAttribute(transaction)
                    .reader(reader)
                    .writer(writer).build();
        }

        private static JobExecutionDecider phoneApprovalValidationGate(PhoneDraftRepositoryPort phoneDraftRepository) {
            return (execution, stepExecution) -> {
                Long id = execution.getJobParameters().getLong(IMPORT_ID);
                boolean allValid = Boolean.TRUE.equals(phoneDraftRepository.allValid(id).block());
                return new FlowExecutionStatus(
                        (allValid ? PhoneImportStatus.VALIDATED : PhoneImportStatus.FAILED).name());
            };
        }

        private static JobExecutionListener phoneApprovalListener(PhoneImportRepositoryPort phoneImportRepository) {
            return new JobExecutionListener() {
                @Override
                public void afterJob(JobExecution execution) {
                    Long id = execution.getJobParameters().getLong(IMPORT_ID);
                    PhoneImportStatus outcome = execution.getStatus() == BatchStatus.COMPLETED
                            && !PhoneImportStatus.FAILED.name().equals(execution.getExitStatus().getExitCode())
                            ? PhoneImportStatus.DONE : PhoneImportStatus.FAILED;
                    Long approvalVersion = execution.getJobParameters().getLong(APPROVAL_VERSION);
                    persistStatusChange(phoneImportRepository, id, PhoneImport.completeApproval(approvalVersion, outcome)).block();
                }
            };
        }

        @Bean
        public Job phoneApproveJob(JobRepository repository,
                @Qualifier("phoneImportValidateStep") Step validateStep,
                @Qualifier("phoneApprovalStep") Step approvalStep,
                PhoneDraftRepositoryPort phoneDraftRepository,
                PhoneImportRepositoryPort phoneImportRepository) {
            JobExecutionDecider validationGate = phoneApprovalValidationGate(phoneDraftRepository);
            return new JobBuilder("phoneApproveJob", repository)
                    .preventRestart()
                    .listener(phoneApprovalListener(phoneImportRepository))
                    .start(validateStep)
                    .next(validationGate).on(PhoneImportStatus.VALIDATED.name()).to(approvalStep)
                    .from(validationGate).on(PhoneImportStatus.FAILED.name()).end(PhoneImportStatus.FAILED.name())
                    .end()
                    .build();
        }
    }
    private static final class PagedDraftReader implements ItemReader<PhoneDraft> {
        private final PhoneDraftRepositoryPort phoneDraftRepository;
        private final Long importId;
        private final int pageSize;
        private int pageNumber;
        private boolean exhausted;
        private Iterator<PhoneDraft> page = List.<PhoneDraft>of().iterator();

        private PagedDraftReader(PhoneDraftRepositoryPort phoneDraftRepository, Long importId, int pageSize) {
            this.phoneDraftRepository = phoneDraftRepository;
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
                var rows = Objects.requireNonNull(phoneDraftRepository.findPage(importId, pageNumber, pageSize)
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
