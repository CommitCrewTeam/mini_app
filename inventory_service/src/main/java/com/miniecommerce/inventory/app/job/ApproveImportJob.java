package com.miniecommerce.inventory.app.job;

import com.miniecommerce.inventory.app.port.outbound.phone.BlockingPhoneDraftRepositoryPort;
import com.miniecommerce.inventory.app.port.outbound.phone.BlockingPhoneImportRepositoryPort;
import com.miniecommerce.inventory.app.port.outbound.phone.BlockingPhoneRepositoryPort;
import com.miniecommerce.inventory.domain.Phone;
import com.miniecommerce.inventory.domain.PhoneDraft;
import com.miniecommerce.inventory.domain.PhoneImport;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportApprovalContext;
import com.miniecommerce.inventory.domain.enums.ImportMode;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    private enum Gate { APPROVABLE, VALIDATION_INVALID, WARNINGS_UNCONFIRMED }

    private final BatchJobSubmitter submitter;
    private final Job approvalJob;
    private final BlockingPhoneImportRepositoryPort phoneImportRepository;

    public ApproveImportJob(
            @Qualifier("batchImportSubmitter") BatchJobSubmitter submitter,
            @Qualifier(PhoneImportUtils.APPROVE_JOB) Job approvalJob,
            BlockingPhoneImportRepositoryPort phoneImportRepository) {
        this.submitter = submitter;
        this.approvalJob = approvalJob;
        this.phoneImportRepository = phoneImportRepository;
    }

    public JobExecution submit(PhoneImportApprovalContext context) {
        Long importId = context.importId();
        return submitter.submit(approvalJob, new JobParametersBuilder()
                .addLong(IMPORT_ID, importId)
                .addString("mode", context.mode().name())
                .addString("warningSkip", Boolean.toString(context.warningSkip()))
                .addString("attempt", UUID.randomUUID().toString())
                .toJobParameters(),
                () -> writeApprovalResult(phoneImportRepository, importId, false, "system exception"));
    }

    private static void writeApprovalResult(BlockingPhoneImportRepositoryPort repository, Long id,
            boolean succeeded, String error) {
        repository.writeStatusResult(id, succeeded, PhoneImport.approvalResultStatuses(), error);
    }

    private static void applyBlockingChunk(BlockingPhoneDraftRepositoryPort drafts,
            BlockingPhoneRepositoryPort phones, Long importId, ImportMode mode, boolean warningSkip,
            List<? extends PhoneDraft> readDrafts) {
        if (readDrafts.isEmpty()) return;
        var phoneKeys = readDrafts.stream().map(PhoneDraft::getKey).distinct().toList();
        Map<String, Phone> phonesByKey = mode.requiresExistingPhone()
                ? phones.findByKeys(phoneKeys).stream().collect(java.util.stream.Collectors.toMap(Phone::getKey, p -> p))
                : Map.of();
        var changed = readDrafts.stream().map(draft ->
                Phone.prepareImportChange(mode, draft, phonesByKey.get(draft.getKey()), warningSkip)).toList();
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
                @Value("#{jobParameters['warningSkip']}") boolean warningSkip,
                BlockingPhoneDraftRepositoryPort phoneDraftRepository,
                BlockingPhoneRepositoryPort phoneRepository) {
            ImportMode importMode = ImportMode.valueOf(mode);
            // ItemWriter is the chunk strategy; select once for this step execution.
            return switch (importMode) {
                case RESTOCK -> chunk -> {
                    if (chunk.isEmpty()) return;
                    phoneDraftRepository.applyRestock(importId,
                            chunk.getItems().stream().map(PhoneDraft::getId).toList());
                };
                case CREATE, UPDATE -> chunk -> applyBlockingChunk(phoneDraftRepository, phoneRepository,
                        importId, importMode, warningSkip, chunk.getItems());
            };
        }

        @Bean(PhoneImportUtils.APPROVE_STEP)
        public Step phoneApprovalStep(JobRepository repository,
                @Qualifier("jpaTransactionManager") PlatformTransactionManager transactionManager,
                @Qualifier("phoneApprovalReader") ItemReader<PhoneDraft> reader,
                @Qualifier("phoneApprovalWriter") ItemWriter<PhoneDraft> writer) {
            return new StepBuilder(PhoneImportUtils.APPROVE_STEP, repository)
                    .<PhoneDraft, PhoneDraft>chunk(APPROVAL_CHUNK_SIZE, transactionManager)
                    .reader(reader)
                    .writer(writer).build();
        }

        private JobExecutionDecider approvalGate(BlockingPhoneDraftRepositoryPort phoneDraftRepository) {
            return (execution, stepExecution) -> {
                Long id = execution.getJobParameters().getLong(IMPORT_ID);
                var summary = phoneDraftRepository.summarizeForApproval(id);
                Gate outcome;
                if (!PhoneImport.isValidationValid(summary)) {
                    outcome = Gate.VALIDATION_INVALID;
                } else if (PhoneImport.isRequiresWarningConfirmation(summary) && !isWarningSkip(execution)) {
                    outcome = Gate.WARNINGS_UNCONFIRMED;
                } else {
                    outcome = Gate.APPROVABLE;
                }
                return new FlowExecutionStatus(outcome.name());
            };
        }

        private JobExecutionListener approvalListener(BlockingPhoneImportRepositoryPort phoneImportRepository) {
            return new JobExecutionListener() {
                @Override
                public void afterJob(JobExecution execution) {
                    Long id = execution.getJobParameters().getLong(IMPORT_ID);
                    String exitCode = execution.getExitStatus().getExitCode();
                    if (Gate.WARNINGS_UNCONFIRMED.name().equals(exitCode)) {
                        phoneImportRepository.writeStatusResult(id, false,
                                PhoneImport.approvalWarningConfirmationStatuses(),
                                "Set warningSkip=true to approve warning drafts");
                    } else if (Gate.VALIDATION_INVALID.name().equals(exitCode)) {
                        phoneImportRepository.writeStatusResult(id, false,
                                PhoneImport.approvalValidationFailedStatuses(), "validate failed");
                    } else {
                        phoneImportRepository.writeStatusResult(id, PhoneImportUtils.jobCompleted(execution),
                                PhoneImport.approvalResultStatuses(), null);
                    }
                }
            };
        }

        private static boolean isWarningSkip(JobExecution execution) {
            return Boolean.parseBoolean(execution.getJobParameters().getString("warningSkip", "false"));
        }

        @Bean(PhoneImportUtils.APPROVE_JOB)
        public Job phoneApproveJob(JobRepository repository,
                @Qualifier(PhoneImportUtils.VALIDATE_STEP) Step validateStep,
                @Qualifier(PhoneImportUtils.APPROVE_STEP) Step approvalStep,
                BlockingPhoneDraftRepositoryPort phoneDraftRepository,
                BlockingPhoneImportRepositoryPort phoneImportRepository) {
            JobExecutionDecider gate = approvalGate(phoneDraftRepository);
            return new JobBuilder(PhoneImportUtils.APPROVE_JOB, repository)
                    .preventRestart()
                    .listener(approvalListener(phoneImportRepository))
                    .start(validateStep)
                    .next(gate).on(Gate.APPROVABLE.name()).to(approvalStep)
                    .from(gate).on(Gate.VALIDATION_INVALID.name()).end(Gate.VALIDATION_INVALID.name())
                    .from(gate).on(Gate.WARNINGS_UNCONFIRMED.name()).end(Gate.WARNINGS_UNCONFIRMED.name())
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
