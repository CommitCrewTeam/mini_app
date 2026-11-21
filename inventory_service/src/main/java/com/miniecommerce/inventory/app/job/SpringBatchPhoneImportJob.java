package com.miniecommerce.inventory.app.job;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import com.miniecommerce.inventory.adapter.outbound.mapper.PhoneDraftMapper;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneDraftRepositoryPort;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneImportRepositoryPort;
import com.miniecommerce.inventory.domain.PhoneDraft;
import com.miniecommerce.inventory.domain.PhoneImport;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportSourceRow;
import java.nio.file.Path;
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
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Component
public class SpringBatchPhoneImportJob {

    private final BatchJobSubmitter submitter;
    private final Job job;
    private final PhoneImportRepositoryPort phoneImportRepository;
    private final PhoneDraftRepositoryPort phoneDraftRepository;
    private final DelimitedLineTokenizer csvTokenizer =
            new DelimitedLineTokenizer();

    public SpringBatchPhoneImportJob(
            @Qualifier("batchImportSubmitter") BatchJobSubmitter submitter,
            @Lazy @Qualifier("phoneImportJob") Job job,
            PhoneImportRepositoryPort phoneImportRepository,
            PhoneDraftRepositoryPort phoneDraftRepository
    ) {
        this.submitter = submitter;
        this.job = job;
        this.phoneImportRepository = phoneImportRepository;
        this.phoneDraftRepository = phoneDraftRepository;
        this.csvTokenizer.setNames("key", "detail", "quantity");
    }

    public JobExecution submit(PhoneImport phoneImport, Path input) {
        return submitter.submit(job, new JobParametersBuilder()
                .addLong("importId", phoneImport.getId())
                .addString("mode", phoneImport.getMode().name())
                .addString("inputFile", input.toAbsolutePath().toString())
                .toJobParameters(), () -> {
                    phoneImport.submissionFailed();
                    phoneImportRepository.save(phoneImport).block();
                });
    }

    private void completeImport(Long id, boolean completed) {
        var phoneImport = phoneImportRepository.findById(id).block();
        if (phoneImport == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "Phone import not found");
        }
        phoneImport.completeImport(completed);
        phoneImportRepository.save(phoneImport).block();
    }

    private JobExecutionListener statusListener() {
        return new JobExecutionListener() {
            @Override
            public void afterJob(JobExecution execution) {
                Long id = execution.getJobParameters().getLong("importId");
                completeImport(id, execution.getStatus() == BatchStatus.COMPLETED);
            }
        };
    }

    @Bean
    @StepScope
    public FlatFileItemReader<PhoneImportSourceRow> phoneCsvReader(
            @Value("#{jobParameters['inputFile']}") String inputFile
    ) {
        return new FlatFileItemReaderBuilder<PhoneImportSourceRow>()
                .name("phoneCsvReader")
                .resource(new FileSystemResource(inputFile))
                .linesToSkip(1)
                .comments(new String[0])
                .lineMapper(this::mapCsvLine)
                .build();
    }

    private PhoneImportSourceRow mapCsvLine(String line, int lineNumber) {
        try {
            var f = csvTokenizer.tokenize(line);
            return new PhoneImportSourceRow(lineNumber,
                    f.readString("key"), f.readString("detail"),
                    f.readString("quantity"), null);
        } catch (Exception e) {
            return new PhoneImportSourceRow(
                    lineNumber, null, null, null, "Cannot parse CSV columns");
        }
    }

    @Bean
    public ItemProcessor<PhoneImportSourceRow, PhoneDraft>
    phoneDraftProcessor(PhoneDraftMapper mapper) {
        return mapper::toDraft;
    }

    @Bean
    @StepScope
    public ItemWriter<PhoneDraft> phoneImportWriter(
            @Value("#{jobParameters['importId']}") Long importId
    ) {
        return chunk -> phoneDraftRepository.saveAll(importId, chunk.getItems()).block();
    }

    @Bean
    public Job phoneImportJob(
            JobRepository repository,
            @Qualifier("batchTransactionManager")
            PlatformTransactionManager transactionManager,
            @Qualifier("phoneCsvReader")
            FlatFileItemReader<PhoneImportSourceRow> reader,
            @Qualifier("phoneImportWriter")
            ItemWriter<PhoneDraft> writer,
            @Qualifier("phoneDraftProcessor")
            ItemProcessor<PhoneImportSourceRow, PhoneDraft> processor,
            @Qualifier("phoneImportValidateStep")
            Step validateStep
    ) {
        Step ingest = new StepBuilder(
                "phoneImportIngestStep", repository
        )
                .<PhoneImportSourceRow, PhoneDraft>chunk(
                        500, transactionManager
                )
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();

        return new JobBuilder("phoneImportJob", repository)
                .preventRestart()
                .listener(statusListener())
                .start(ingest)
                .next(validateStep)
                .build();
    }



}
