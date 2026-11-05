package com.miniecommerce.inventory.app.job;

import com.miniecommerce.inventory.domain.PhoneImportStatus;
import com.miniecommerce.inventory.domain.PhoneDraftStatus;
import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneRepositoryPort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneDraftRepositoryPort;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneImportRepositoryPort;
import com.miniecommerce.inventory.domain.ImportMode;
import com.miniecommerce.inventory.domain.Phone;
import com.miniecommerce.inventory.domain.PhoneDraft;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.batch.item.support.SynchronizedItemReader;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Component
public class ApproveImportJob {
    public record ImportedPhone(Long draftId, Long matchedVersion, Phone phone) { }

    private final JobLauncher launcher;
    private final Job approvalJob;
    private final PhoneRepositoryPort phones;
    private final TransactionalOperator transactions;
    private final PhoneImportRepositoryPort imports;
    private final PhoneDraftRepositoryPort drafts;

    public ApproveImportJob(
            @Qualifier("phoneApprovalJobLauncher") JobLauncher launcher,
            @Lazy @Qualifier("phoneApproveJob") Job approvalJob,
            PhoneRepositoryPort phones,
            ConnectionFactory connectionFactory,
            PhoneImportRepositoryPort imports,
            PhoneDraftRepositoryPort drafts) {
        this.launcher = launcher;
        this.approvalJob = approvalJob;
        this.phones = phones;
        this.transactions = TransactionalOperator.create(new R2dbcTransactionManager(connectionFactory));
        this.imports = imports;
        this.drafts = drafts;
    }

    public Long launchApprove(UUID id) throws Exception {
        var phoneImport = imports.findById(id).block();
        if (phoneImport == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "Phone import not found");
        }
        if (phoneImport.getStatus() != PhoneImportStatus.READY) {
            throw new AppException(ErrorCode.CONFLICT, "Import is not ready");
        }
        phoneImport.transitionTo(PhoneImportStatus.APPROVING);
        // to handle it: concurrency.
        imports.save(phoneImport).block();
        var parameters = new JobParametersBuilder()
                .addString("importId", id.toString())
                .addString("mode", phoneImport.getMode().name())
                .addString("requestId", UUID.randomUUID().toString())
                .toJobParameters();
        try {
            return launcher.run(approvalJob, parameters).getId();
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

    @Bean
    public static SimpleAsyncTaskExecutor phoneApprovalExecutor(
            @Value("${app.phone-import.approval.workers:5}") int workers) {
        if (workers < 1) {
            throw new IllegalArgumentException("Approval workers must be positive");
        }
        var executor = new SimpleAsyncTaskExecutor("phone-approve-");
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(workers);
        return executor;
    }

    @Bean
    @StepScope
    public ItemReader<PhoneDraft> phoneApprovalReader(
            @Value("#{jobParameters['importId']}") String importId,
            @Value("${app.phone-import.approval.chunk-size:500}") int pageSize) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("Approval chunk size must be positive");
        }
        return new SynchronizedItemReader<>(new ItemReader<PhoneDraft>() {
            private int pageNumber;
            private boolean exhausted;
            private Iterator<PhoneDraft> page = List.<PhoneDraft>of().iterator();

            @Override
            public PhoneDraft read() {
                if (exhausted) {
                    return null;
                }
                if (!page.hasNext()) {
                    // Page the stable import rows; filtering by VALID here would skip rows
                    // as other workers mark earlier pages APPLIED.
                    var rows = Objects.requireNonNull(drafts.findPage(UUID.fromString(importId), pageNumber, pageSize)
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
        });
    }

    @Bean
    public ItemProcessor<PhoneDraft, ImportedPhone> phoneApprovalProcessor() {
        return draft -> {
            if (draft.getStatus() != PhoneDraftStatus.VALID) {
                throw new IllegalStateException("Draft is no longer valid after approval validation");
            }
            var phone = Phone.fromImport(draft.getKey(), draft.getDetail(), draft.getQuantity());
            return new ImportedPhone(draft.getId(), draft.getMatchedVersion(), phone);
        };
    }

    @Bean
    @StepScope
    public ItemWriter<ImportedPhone> phoneApprovalWriter(
            @Value("#{jobParameters['importId']}") String importId,
            @Value("#{jobParameters['mode']}") String mode) {
        return chunk -> applyChunk(UUID.fromString(importId), ImportMode.valueOf(mode),
                chunk.getItems()).block();
    }

    private Mono<Void> applyChunk(UUID importId, ImportMode mode, List<? extends ImportedPhone> rows) {
        // All phone writes and draft acknowledgements in this chunk commit or roll back together.
        return Flux.fromIterable(rows)
                .concatMap(row -> drafts.findById(importId, row.draftId())
                        .switchIfEmpty(Mono.error(new IllegalStateException("Draft not found in import")))
                        .flatMap(draft -> {
                            if (draft.getStatus() != PhoneDraftStatus.VALID) {
                                return Mono.error(new IllegalStateException("Draft changed after validation"));
                            }
                            return applyPhone(mode, row).flatMap(saved -> {
                                draft.transitionTo(PhoneDraftStatus.APPLIED);
                                return drafts.saveAll(importId, List.of(draft));
                            });
                        }))
                .then()
                .as(transactions::transactional);
    }

    private Mono<Phone> applyPhone(ImportMode mode, ImportedPhone row) {
        return switch (mode) {
            case APPEND -> phones.save(row.phone());
            case UPDATE -> updateExisting(row,
                    phone -> phone.updateFromImport(row.phone().getDetail()));
            case RESTOCK -> updateExisting(row, phone -> phone.restock(row.phone().getStock()));
        };
    }

    private Mono<Phone> updateExisting(ImportedPhone row, Consumer<Phone> update) {
        return phones.findVersionedByKey(row.phone().getName())
                .switchIfEmpty(Mono.error(new AppException(ErrorCode.NOT_FOUND, "Phone not found")))
                .flatMap(snapshot -> {
                    // to handle it: verify matchedVersion and concurrent phone changes.
                    Phone existing = snapshot.phone();
                    update.accept(existing);
                    return phones.save(existing, snapshot.version());
                });
    }

    @Bean
    public Step phoneApprovalStep(JobRepository repository,
            @Qualifier("batchTransactionManager") PlatformTransactionManager transactionManager,
            @Qualifier("phoneApprovalReader") ItemReader<PhoneDraft> reader,
            @Qualifier("phoneApprovalProcessor") ItemProcessor<PhoneDraft, ImportedPhone> processor,
            @Qualifier("phoneApprovalWriter") ItemWriter<ImportedPhone> writer,
            @Qualifier("phoneApprovalExecutor") SimpleAsyncTaskExecutor approvalExecutor,
            @Value("${app.phone-import.approval.chunk-size:500}") int chunkSize) {
        var transaction = new DefaultTransactionAttribute();
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
        return new StepBuilder("phoneApprovalStep", repository)
                .<PhoneDraft, ImportedPhone>chunk(chunkSize, transactionManager)
                .taskExecutor(approvalExecutor)
                .transactionAttribute(transaction)
                .reader(reader).processor(processor).writer(writer).build();
    }

    @Bean
    public Job phoneApproveJob(JobRepository repository,
            @Qualifier("phoneImportValidateStep") Step validateStep,
            @Qualifier("phoneApprovalStep") Step approvalStep) {
        // Validate every page before allowing any chunk to write to phone.
        JobExecutionDecider validationGate = (execution, stepExecution) -> {
            UUID id = UUID.fromString(execution.getJobParameters().getString("importId"));
            boolean allValid = Boolean.TRUE.equals(drafts.allValid(id).block());
            execution.getExecutionContext().putString("approvalValidation", allValid ? "PASSED" : "BLOCKED");
            return new FlowExecutionStatus(allValid ? "ALL_VALID" : "INVALID_ROWS");
        };
        return new JobBuilder("phoneApproveJob", repository)
                .preventRestart()
                .listener(new JobExecutionListener() {
                    @Override
                    public void afterJob(JobExecution execution) {
                        UUID id = UUID.fromString(execution.getJobParameters().getString("importId"));
                        PhoneImportStatus status = PhoneImportStatus.FAILED;
                        if (execution.getStatus() == BatchStatus.COMPLETED) {
                            status = "BLOCKED".equals(execution.getExecutionContext()
                                    .getString("approvalValidation", ""))
                                    ? PhoneImportStatus.READY : PhoneImportStatus.DONE;
                        }
                        updateImportStatus(id, status);
                    }
                })
                .start(validateStep)
                .next(validationGate).on("ALL_VALID").to(approvalStep)
                .from(validationGate).on("INVALID_ROWS").end()
                .end()
                .build();
    }
}
