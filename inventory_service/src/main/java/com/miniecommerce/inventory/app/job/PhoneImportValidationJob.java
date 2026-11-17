package com.miniecommerce.inventory.app.job;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneDraftRepositoryPort;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneImportRepositoryPort;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneRepositoryPort;
import com.miniecommerce.inventory.domain.PhoneDraft;
import com.miniecommerce.inventory.domain.enums.ImportMode;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportMatch;
import java.util.List;
import java.util.Objects;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.SynchronizedItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

@Component
public class PhoneImportValidationJob {
    private final BatchJobSubmitter submitter;
    private final Job validationJob;
    private final PhoneImportRepositoryPort phoneImportRepository;
    private final PhoneDraftRepositoryPort phoneDraftRepository;
    private final PhoneRepositoryPort phoneRepository;

    public PhoneImportValidationJob(
            @Qualifier("batchImportSubmitter") BatchJobSubmitter submitter,
            @Lazy @Qualifier("phoneValidateJob") Job validationJob,
            PhoneImportRepositoryPort phoneImportRepository,
            PhoneDraftRepositoryPort phoneDraftRepository,
            PhoneRepositoryPort phoneRepository) {
        this.submitter = submitter;
        this.validationJob = validationJob;
        this.phoneImportRepository = phoneImportRepository;
        this.phoneDraftRepository = phoneDraftRepository;
        this.phoneRepository = phoneRepository;
    }

    public Long launchValidation(Long id) {
        var phoneImport = phoneImportRepository.findById(id).block();
        if (phoneImport == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "Phone import not found");
        }
        phoneImport.startValidation();
        var savedImport = phoneImportRepository.save(phoneImport).block();
        return submitter.submit(validationJob, new JobParametersBuilder()
                .addLong("importId", id)
                .addString("mode", phoneImport.getMode().name())
                .toJobParameters(), () -> {
                    savedImport.submissionFailed();
                    phoneImportRepository.save(savedImport).block();
                }).getId();
    }

    private void completeValidation(Long id, boolean completed, boolean allValid) {
        var phoneImport = phoneImportRepository.findById(id).block();
        if (phoneImport == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "Phone import not found");
        }
        phoneImport.completeValidation(completed, allValid);
        phoneImportRepository.save(phoneImport).block();
    }

    private JobExecutionListener statusListener() {
        return new JobExecutionListener() {
            @Override
            public void afterJob(JobExecution execution) {
                Long id = execution.getJobParameters().getLong("importId");
                boolean completed = execution.getStatus() == BatchStatus.COMPLETED;
                boolean allValid = completed && Boolean.TRUE.equals(phoneDraftRepository.allValid(id).block());
                completeValidation(id, completed, allValid);
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
            @Value("#{jobParameters['importId']}") Long importId,
            @Value("${app.phone-import.validation.chunk-size:500}") int chunkSize
    ) {
        if (chunkSize < 1) {
            throw new IllegalArgumentException("Validation chunk size must be positive");
        }
        return new SynchronizedItemReader<>(
                new PendingDraftPageReader(importId, chunkSize));
    }

    @Bean
    @StepScope
    public ItemProcessor<List<PhoneDraft>, List<PhoneDraft>> phoneValidationProcessor(
            @Value("#{jobParameters['mode']}") String mode,
            @Value("#{jobParameters['importId']}") Long importId
    ) {
        ImportMode importMode = ImportMode.valueOf(mode);
        return rows -> {
            var keys = rows.stream().map(PhoneDraft::getKey)
                    .filter(Objects::nonNull).distinct().toList();
            var duplicates = Objects.requireNonNull(phoneDraftRepository.findDuplicateKeys(importId, keys).block());
            var matches = Objects.requireNonNull(phoneRepository.findImportMatchesByKeys(keys)
                    .collectMap(PhoneImportMatch::key).block());
            // Validation stamps each valid draft with the Phone.version it matched here. Approval
            // later compares that snapshot with a freshly read Phone before writing.
            rows.forEach(row -> row.validateForImport(importMode,
                    duplicates.contains(row.getKey()), matches.get(row.getKey())));
            return rows;
        };
    }

    @Bean
    @StepScope
    public ItemWriter<List<PhoneDraft>> phoneValidationWriter(
            @Value("#{jobParameters['importId']}") Long importId
    ) {
        // Each page is persisted in its own R2DBC transaction.
        return chunk -> {
            for (var page : chunk.getItems()) {
                phoneDraftRepository.saveAll(importId, page).block();
            }
        };
    }

    @Bean
    public SimpleAsyncTaskExecutor phoneValidationExecutor(
            @Value("${app.phone-import.validation.workers:50}") int workers
    ) {
        if (workers < 1) {
            throw new IllegalArgumentException("Validation workers must be positive");
        }
        var executor = new SimpleAsyncTaskExecutor("phone-validate-");
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(workers);
        return executor;
    }

    // SynchronizedItemReader serializes read and cursor advancement across workers.
    private final class PendingDraftPageReader implements ItemReader<List<PhoneDraft>> {
        private final Long importId;
        private final int chunkSize;
        private long afterId;
        private boolean exhausted;

        private PendingDraftPageReader(Long importId, int chunkSize) {
            this.importId = importId;
            this.chunkSize = chunkSize;
        }

        @Override
        public List<PhoneDraft> read() {
            if (exhausted) {
                return null;
            }
            var rows = Objects.requireNonNull(phoneDraftRepository.findChunkAfterId(importId, afterId, chunkSize)
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
