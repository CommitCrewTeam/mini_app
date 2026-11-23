package com.miniecommerce.inventory.app.job;

import com.miniecommerce.inventory.app.port.outbound.phone.BlockingPhoneDraftRepositoryPort;
import com.miniecommerce.inventory.app.port.outbound.phone.BlockingPhoneImportRepositoryPort;
import com.miniecommerce.inventory.app.port.outbound.phone.BlockingPhoneRepositoryPort;
import com.miniecommerce.inventory.domain.PhoneDraft;
import com.miniecommerce.inventory.domain.PhoneImport;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportValidationContext;
import com.miniecommerce.inventory.domain.enums.ImportMode;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportMatch;
import java.util.List;
import java.util.Objects;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Component
public class PhoneImportValidationJob {
    private final BatchJobSubmitter submitter;
    private final Job validationJob;
    private final BlockingPhoneImportRepositoryPort phoneImportRepository;

    public PhoneImportValidationJob(
            @Qualifier("batchImportSubmitter") BatchJobSubmitter submitter,
            @Lazy @Qualifier(PhoneImportUtils.VALIDATE_JOB) Job validationJob,
            BlockingPhoneImportRepositoryPort phoneImportRepository) {
        this.submitter = submitter;
        this.validationJob = validationJob;
        this.phoneImportRepository = phoneImportRepository;
    }

    public Long submitValidation(PhoneImportValidationContext context) {
        Long id = context.importId();
        return submitter.submit(validationJob, new JobParametersBuilder()
                .addLong("importId", id)
                .addString("mode", context.mode().name())
                .toJobParameters(), () -> finishValidation(id, false)).getId();
    }

    private void finishValidation(Long id, boolean validationJobSucceeded) {
        phoneImportRepository.writeValidationResult(id, validationJobSucceeded,
                PhoneDraft.approvableStatuses(), PhoneImport.validationResultStatuses());
    }

    private JobExecutionListener statusListener() {
        return new JobExecutionListener() {
            @Override
            public void afterJob(JobExecution execution) {
                Long id = execution.getJobParameters().getLong("importId");
                finishValidation(id, PhoneImportUtils.jobCompleted(execution));
            }
        };
    }

    @Bean(PhoneImportUtils.VALIDATE_JOB)
    public Job phoneValidateJob(JobRepository repository,
            @Qualifier(PhoneImportUtils.VALIDATE_STEP) Step validateStep) {
        return new JobBuilder(PhoneImportUtils.VALIDATE_JOB, repository).preventRestart()
                .listener(statusListener()).start(validateStep).build();
    }

    @Bean
    @StepScope
    public ItemReader<List<PhoneDraft>> phonePendingDraftReader(
            @Value("#{jobParameters['importId']}") Long importId,
            @Value("${app.phone-import.validation.chunk-size:500}") int chunkSize,
            BlockingPhoneDraftRepositoryPort phoneDraftRepository
    ) {
        if (chunkSize < 1) {
            throw new IllegalArgumentException("Validation chunk size must be positive");
        }
        return new SynchronizedItemReader<>(
                new PendingDraftPageReader(importId, chunkSize, phoneDraftRepository));
    }

    @Bean
    @StepScope
    public ItemProcessor<List<PhoneDraft>, List<PhoneDraft>> phoneValidationProcessor(
            @Value("#{jobParameters['mode']}") String mode,
            @Value("#{jobParameters['importId']}") Long importId,
            BlockingPhoneDraftRepositoryPort phoneDraftRepository,
            BlockingPhoneRepositoryPort phoneRepository
    ) {
        ImportMode importMode = ImportMode.valueOf(mode);
        return rows -> {
            var keys = rows.stream().map(PhoneDraft::getKey)
                    .filter(Objects::nonNull).distinct().toList();
            var duplicates = phoneDraftRepository.findDuplicateKeys(importId, keys);
            var matches = phoneRepository.findImportMatchesByKeys(keys).stream()
                    .collect(java.util.stream.Collectors.toMap(PhoneImportMatch::key, match -> match));
            // The first valid match captures a Phone.version baseline. Revalidation marks a
            // changed Phone as WARNING and keeps that original baseline for approval.
            rows.forEach(row -> row.validateForImport(importMode,
                    duplicates.contains(row.getKey()), matches.get(row.getKey())));
            return rows;
        };
    }

    @Bean
    @StepScope
    public ItemWriter<List<PhoneDraft>> phoneValidationWriter(
            @Value("#{jobParameters['importId']}") Long importId,
            BlockingPhoneDraftRepositoryPort phoneDraftRepository
    ) {
        return chunk -> {
            for (var page : chunk.getItems()) {
                phoneDraftRepository.saveAll(importId, page);
            }
        };
    }

    // SynchronizedItemReader serializes read and cursor advancement across workers.
    private final class PendingDraftPageReader implements ItemReader<List<PhoneDraft>> {
        private final Long importId;
        private final int chunkSize;
        private final BlockingPhoneDraftRepositoryPort phoneDraftRepository;
        private long afterId;
        private boolean exhausted;

        private PendingDraftPageReader(Long importId, int chunkSize,
                BlockingPhoneDraftRepositoryPort phoneDraftRepository) {
            this.importId = importId;
            this.chunkSize = chunkSize;
            this.phoneDraftRepository = phoneDraftRepository;
        }

        @Override
        public List<PhoneDraft> read() {
            if (exhausted) {
                return null;
            }
            var rows = phoneDraftRepository.findChunkAfterId(importId, afterId, chunkSize);
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
    @Bean(PhoneImportUtils.VALIDATE_STEP)
    public Step phoneImportValidateStep(
            JobRepository repository,
            @Qualifier("jpaTransactionManager")
            PlatformTransactionManager transactionManager,
            @Qualifier("phonePendingDraftReader")
            ItemReader<List<PhoneDraft>> validationReader,
            @Qualifier("phoneValidationProcessor")
            ItemProcessor<List<PhoneDraft>, List<PhoneDraft>> validationProcessor,
            @Qualifier("phoneValidationWriter")
            ItemWriter<List<PhoneDraft>> validationWriter
    ) {
        return new StepBuilder(
                PhoneImportUtils.VALIDATE_STEP, repository
        )
                // One Spring Batch item is one keyset page, so commit one page at a time.
                .<List<PhoneDraft>, List<PhoneDraft>>chunk(1, transactionManager)
                .reader(validationReader)
                .processor(validationProcessor)
                .writer(validationWriter)
                .build();
    }

}
