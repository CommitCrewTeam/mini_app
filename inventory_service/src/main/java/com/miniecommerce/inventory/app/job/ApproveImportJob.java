package com.miniecommerce.inventory.app.job;

import com.miniecommerce.inventory.app.port.outbound.phone.BlockingPhoneDraftRepositoryPort;
import com.miniecommerce.inventory.app.port.outbound.phone.BlockingPhoneRepositoryPort;
import com.miniecommerce.inventory.app.port.outbound.phone.BlockingPhoneImportRepositoryPort;
import com.miniecommerce.inventory.domain.PhoneImport;
import com.miniecommerce.inventory.domain.Phone;
import com.miniecommerce.inventory.domain.PhoneDraft;
import com.miniecommerce.inventory.domain.enums.PhoneImportStatus;
import com.miniecommerce.inventory.domain.enums.ImportMode;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Component
public class ApproveImportJob {
    private static final String IMPORT_ID = "importId";
    private static final int APPROVAL_CHUNK_SIZE = 500;

    private final BatchJobSubmitter submitter;
    private final Job approvalJob;
    private final BlockingPhoneImportRepositoryPort phoneImportRepository;

    public ApproveImportJob(
            @Qualifier("batchImportSubmitter") BatchJobSubmitter submitter,
            @Qualifier("phoneApproveJob") Job approvalJob,
            BlockingPhoneImportRepositoryPort phoneImportRepository) {
        this.submitter = submitter;
        this.approvalJob = approvalJob;
        this.phoneImportRepository = phoneImportRepository;
    }

    public JobExecution submit(Long importId, ImportMode mode) {
        return submitter.submit(approvalJob, new JobParametersBuilder()
                .addLong(IMPORT_ID, importId)
                .addString("mode", mode.name())
                .toJobParameters(),
                () -> writeApprovalResult(phoneImportRepository, importId, false, "system exception"));
    }

    private static void writeApprovalResult(BlockingPhoneImportRepositoryPort repository, Long id,
            boolean succeeded, String error) {
        repository.writeStatusResult(id, succeeded, PhoneImport.approvalResultStatuses(), error);
    }

    private static void applyBlockingChunk(BlockingPhoneDraftRepositoryPort drafts,
            BlockingPhoneRepositoryPort phones, Long importId, ImportMode mode,
            List<? extends PhoneDraft> readDrafts) {
        if (readDrafts.isEmpty()) return;
        var phoneKeys = readDrafts.stream().map(PhoneDraft::getKey).distinct().toList();
        Map<String, Phone> phonesByKey = mode.requiresExistingPhone()
                ? phones.findByKeys(phoneKeys).stream().collect(java.util.stream.Collectors.toMap(Phone::getKey, p -> p))
                : Map.of();
        var changed = readDrafts.stream().map(draft ->
                Phone.prepareImportChange(mode, draft, phonesByKey.get(draft.getKey()))).toList();
        phones.saveChanges(changed);
        readDrafts.forEach(PhoneDraft::markApplied);
        drafts.saveAll(importId, readDrafts);
    }

    /** Batch wiring kept separate from the approval operations in this file. */
    @Configuration(proxyBeanMethods = false)
    public static class ApprovalConfiguration {
        @Bean
        @StepScope
        public ItemReader<PhoneDraft> phoneApprovalReader(
                @Value("#{jobParameters['importId']}") Long importId,
                BlockingPhoneDraftRepositoryPort phoneDraftRepository) {
            return new SynchronizedItemReader<>(
                    new PagedDraftReader(phoneDraftRepository, importId, APPROVAL_CHUNK_SIZE));
        }

        @Bean
        @StepScope
        public ItemWriter<PhoneDraft> phoneApprovalWriter(
                @Value("#{jobParameters['importId']}") Long importId,
                @Value("#{jobParameters['mode']}") String mode,
                BlockingPhoneDraftRepositoryPort phoneDraftRepository,
                BlockingPhoneRepositoryPort phoneRepository) {
            ImportMode importMode = ImportMode.valueOf(mode);
            return chunk -> applyBlockingChunk(phoneDraftRepository, phoneRepository, importId, importMode,
                    chunk.getItems());
        }

        @Bean
        public Step phoneApprovalStep(JobRepository repository,
                @Qualifier("jpaTransactionManager") PlatformTransactionManager transactionManager,
                @Qualifier("phoneApprovalReader") ItemReader<PhoneDraft> reader,
                @Qualifier("phoneApprovalWriter") ItemWriter<PhoneDraft> writer) {
            return new StepBuilder("phoneApprovalStep", repository)
                    .<PhoneDraft, PhoneDraft>chunk(APPROVAL_CHUNK_SIZE, transactionManager)
                    .reader(reader)
                    .writer(writer).build();
        }

        private JobExecutionDecider phoneApprovalValidationGate(BlockingPhoneDraftRepositoryPort phoneDraftRepository) {
            return (execution, stepExecution) -> {
                Long id = execution.getJobParameters().getLong(IMPORT_ID);
                boolean allDraftsValid = phoneDraftRepository.allValid(id);
                return new FlowExecutionStatus(
                        (allDraftsValid ? PhoneImportStatus.VALIDATED : PhoneImportStatus.VALIDATION_FAILED).name());
            };
        }

        private JobExecutionListener phoneApprovalListener(BlockingPhoneImportRepositoryPort phoneImportRepository) {
            return new JobExecutionListener() {
                @Override
                public void afterJob(JobExecution execution) {
                    Long id = execution.getJobParameters().getLong(IMPORT_ID);
                    boolean validationFailed = PhoneImportStatus.APPROVAL_VALIDATED_FAILED.name()
                            .equals(execution.getExitStatus().getExitCode());
                    boolean succeeded = execution.getStatus() == BatchStatus.COMPLETED && !validationFailed;
                    var statuses = validationFailed
                            ? PhoneImport.approvalValidationFailedStatuses()
                            : PhoneImport.approvalResultStatuses();
                    phoneImportRepository.writeStatusResult(id, succeeded, statuses,
                            validationFailed ? "validate failed" : null);
                }
            };
        }

        @Bean
        public Job phoneApproveJob(JobRepository repository,
                @Qualifier("phoneImportValidateStep") Step validateStep,
                @Qualifier("phoneApprovalStep") Step approvalStep,
                BlockingPhoneDraftRepositoryPort phoneDraftRepository,
                BlockingPhoneImportRepositoryPort phoneImportRepository) {
            JobExecutionDecider validationGate = phoneApprovalValidationGate(phoneDraftRepository);
            return new JobBuilder("phoneApproveJob", repository)
                    .preventRestart()
                    .listener(phoneApprovalListener(phoneImportRepository))
                    .start(validateStep)
                    .next(validationGate).on(PhoneImportStatus.VALIDATED.name()).to(approvalStep)
                    .from(validationGate).on(PhoneImportStatus.VALIDATION_FAILED.name())
                    .end(PhoneImportStatus.APPROVAL_VALIDATED_FAILED.name())
                    .end()
                    .build();
        }
    }
    private static final class PagedDraftReader implements ItemReader<PhoneDraft> {
        private final BlockingPhoneDraftRepositoryPort phoneDraftRepository;
        private final Long importId;
        private final int pageSize;
        private int pageNumber;
        private boolean exhausted;
        private Iterator<PhoneDraft> page = List.<PhoneDraft>of().iterator();

        private PagedDraftReader(BlockingPhoneDraftRepositoryPort phoneDraftRepository, Long importId, int pageSize) {
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
                var rows = phoneDraftRepository.findPage(importId, pageNumber, pageSize);
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
