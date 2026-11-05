package com.miniecommerce.inventory.app.job;

import com.miniecommerce.inventory.domain.PhoneImportStatus;
import com.miniecommerce.inventory.domain.PhoneDraftStatus;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneDraftRepositoryPort;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneImportRepositoryPort;
import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Lazy;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneRepositoryPort;
import com.miniecommerce.inventory.domain.ImportMode;
import com.miniecommerce.inventory.domain.PhoneDraft;
import com.miniecommerce.inventory.domain.PhoneImportMatch;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.SynchronizedItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

@Component
public class PhoneImportValidationJob {
    private final JobLauncher launcher;
    private final Job validationJob;
    private final PhoneImportRepositoryPort imports;
    private final PhoneDraftRepositoryPort drafts;
    private final PhoneRepositoryPort phones;
    // Validators run in order within a page; pages are processed concurrently.
    private final List<ChunkValidationStep> validationSteps = List.of(this::validatePhoneKeys);

    public PhoneImportValidationJob(
            @Qualifier("batchImportJobLauncher") JobLauncher launcher,
            @Lazy @Qualifier("phoneValidateJob") Job validationJob,
            PhoneImportRepositoryPort imports,
            PhoneDraftRepositoryPort drafts,
            PhoneRepositoryPort phones) {
        this.launcher = launcher;
        this.validationJob = validationJob;
        this.imports = imports;
        this.drafts = drafts;
        this.phones = phones;
    }

    public Long launchValidation(UUID id) throws Exception {
        var phoneImport = imports.findById(id).block();
        if (phoneImport == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "Phone import not found");
        }
        if (phoneImport.getStatus() != PhoneImportStatus.READY) {
            throw new AppException(ErrorCode.CONFLICT, "Import is not ready");
        }
        phoneImport.transitionTo(PhoneImportStatus.VALIDATING);
        // to handle it: concurrency.
        imports.save(phoneImport).block();
        try {
            var execution = launcher.run(validationJob, new JobParametersBuilder()
                    .addString("importId", id.toString())
                    .addString("mode", phoneImport.getMode().name())
                    .addString("requestId", UUID.randomUUID().toString())
                    .toJobParameters());
            return execution.getId();
        } catch (Exception e) {
            updateImportStatus(id, PhoneImportStatus.READY);
            throw e;
        }
    }

    private void updateImportStatus(UUID id, PhoneImportStatus status) {
        var phoneImport = imports.findById(id).block();
        if (phoneImport == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "Phone import not found");
        }
        phoneImport.transitionTo(status);
        imports.save(phoneImport).block();
    }

    private JobExecutionListener statusListener() {
        return new JobExecutionListener() {
            @Override
            public void afterJob(JobExecution execution) {
                UUID id = UUID.fromString(execution.getJobParameters().getString("importId"));
                updateImportStatus(id, execution.getStatus() == BatchStatus.COMPLETED ? PhoneImportStatus.READY : PhoneImportStatus.FAILED);
            }
        };
    }

    @Bean
    public Job phoneValidateJob(JobRepository repository,
            @Qualifier("phoneImportValidateStep") Step validateStep) {
        return new JobBuilder("phoneValidateJob", repository).preventRestart()
                .listener(statusListener()).start(validateStep).build();
    }

    @Bean
    @StepScope
    public ItemReader<List<PhoneDraft>> phonePendingDraftReader(
            @Value("#{jobParameters['importId']}") String importId,
            @Value("${app.phone-import.validation.chunk-size:500}") int chunkSize
    ) {
        if (chunkSize < 1) {
            throw new IllegalArgumentException("Validation chunk size must be positive");
        }
        return new SynchronizedItemReader<>(
                new PendingDraftPageReader(UUID.fromString(importId), chunkSize));
    }

    @Bean
    @StepScope
    public ItemProcessor<List<PhoneDraft>, List<PhoneDraft>> phoneValidationProcessor(
            @Value("#{jobParameters['mode']}") String mode,
            @Value("#{jobParameters['importId']}") String importId
    ) {
        ImportMode importMode = ImportMode.valueOf(mode);
        return rows -> {
            var duplicates = drafts.findDuplicateKeys(UUID.fromString(importId), rows.stream()
                    .map(PhoneDraft::getKey).filter(Objects::nonNull).distinct().toList()).block();
            for (var row : rows) {
                if (row.getStatus() == PhoneDraftStatus.APPLIED) {
                    continue;
                }
                // Missing parsed fields retain their original parse error (including legacy drafts).
                if (row.getQuantity() != null && row.getDetail() != null) {
                    row.transitionTo(PhoneDraftStatus.PENDING);
                    row.setErrorMessage(null);
                } else if (row.getStatus() != PhoneDraftStatus.INVALID) {
                    row.transitionTo(PhoneDraftStatus.INVALID);
                    row.setErrorMessage("Missing parsed fields; upload the CSV again");
                }
                if (row.getStatus() == PhoneDraftStatus.PENDING && duplicates.contains(row.getKey())) {
                    row.transitionTo(PhoneDraftStatus.INVALID);
                    row.setErrorMessage("Duplicate key in import");
                }
            }
            for (var validationStep : validationSteps) {
                var pending = rows.stream()
                        .filter(row -> row.getStatus() == PhoneDraftStatus.PENDING)
                        .toList();
                if (pending.isEmpty()) {
                    break;
                }
                validationStep.validate(importMode, pending);
            }
            rows.stream().filter(row -> row.getStatus() == PhoneDraftStatus.PENDING)
                    .forEach(row -> row.transitionTo(PhoneDraftStatus.VALID));
            return rows;
        };
    }

    @Bean
    @StepScope
    public ItemWriter<List<PhoneDraft>> phoneValidationWriter(
            @Value("#{jobParameters['importId']}") String importId
    ) {
        UUID id = UUID.fromString(importId);
        // Each page is persisted in its own R2DBC transaction.
        return chunk -> {
            for (var page : chunk.getItems()) {
                drafts.saveAll(id, page).block();
            }
        };
    }

    @Bean
    public SimpleAsyncTaskExecutor phoneValidationExecutor(
            @Value("${app.phone-import.validation.workers:5}") int workers
    ) {
        if (workers < 1) {
            throw new IllegalArgumentException("Validation workers must be positive");
        }
        var executor = new SimpleAsyncTaskExecutor("phone-validate-");
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(workers);
        return executor;
    }

    @FunctionalInterface
    private interface ChunkValidationStep {
        void validate(ImportMode mode, List<PhoneDraft> rows) throws Exception;
    }

    private void validatePhoneKeys(ImportMode mode, List<PhoneDraft> rows) {
        var keys = rows.stream()
                .map(PhoneDraft::getKey)
                .filter(key -> key != null && !key.isBlank())
                .distinct()
                .toList();
        var matches = Objects.requireNonNull(phones.findImportMatchesByKeys(keys)
                .collectMap(PhoneImportMatch::key)
                .block());
        rows.forEach(row -> row.validateAgainstPhone(mode, matches.get(row.getKey())));
    }

    // SynchronizedItemReader serializes read and cursor advancement across workers.
    private final class PendingDraftPageReader implements ItemReader<List<PhoneDraft>> {
        private final UUID importId;
        private final int chunkSize;
        private long afterId;
        private boolean exhausted;

        private PendingDraftPageReader(UUID importId, int chunkSize) {
            this.importId = importId;
            this.chunkSize = chunkSize;
        }

        @Override
        public List<PhoneDraft> read() {
            if (exhausted) {
                return null;
            }
            var rows = Objects.requireNonNull(drafts.findChunkAfterId(importId, afterId, chunkSize)
                    .collectList().block());
            if (rows.isEmpty()) {
                exhausted = true;
                return null;
            }
            afterId = rows.getLast().getId();
            return rows;
        }
    }

    /**
     * Reusable validate step. Any job can use it via .next(validateStep), provided it is launched
     * with the same job parameters (importId, mode) that the step-scoped beans read.
     */
    @Bean
    public Step phoneImportValidateStep(
            JobRepository repository,
            @Qualifier("batchTransactionManager")
            PlatformTransactionManager transactionManager,
            @Qualifier("phonePendingDraftReader")
            ItemReader<List<PhoneDraft>> validationReader,
            @Qualifier("phoneValidationProcessor")
            ItemProcessor<List<PhoneDraft>, List<PhoneDraft>> validationProcessor,
            @Qualifier("phoneValidationWriter")
            ItemWriter<List<PhoneDraft>> validationWriter,
            @Qualifier("phoneValidationExecutor")
            SimpleAsyncTaskExecutor validationExecutor
    ) {
        // Do not hold a JDBC transaction while workers perform DB/external validation.
        var validationTransaction = new DefaultTransactionAttribute();
        validationTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
        return new StepBuilder(
                "phoneImportValidateStep", repository
        )
                // One Spring Batch item is one keyset page, so commit one page at a time.
                .<List<PhoneDraft>, List<PhoneDraft>>chunk(1, transactionManager)
                .reader(validationReader)
                .processor(validationProcessor)
                .writer(validationWriter)
                .taskExecutor(validationExecutor)
                .transactionAttribute(validationTransaction)
                .build();
    }

}
