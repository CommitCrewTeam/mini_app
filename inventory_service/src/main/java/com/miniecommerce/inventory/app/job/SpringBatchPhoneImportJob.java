package com.miniecommerce.inventory.app.job;

import com.miniecommerce.common.storage.FileUtil;
import com.miniecommerce.inventory.adapter.outbound.mapper.PhoneDraftMapper;
import com.miniecommerce.inventory.app.port.outbound.phone.BlockingPhoneDraftRepositoryPort;
import com.miniecommerce.inventory.app.port.outbound.phone.BlockingPhoneImportRepositoryPort;
import com.miniecommerce.inventory.domain.PhoneDraft;
import com.miniecommerce.inventory.domain.PhoneImport;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportSourceRow;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportIngestionContext;

import java.nio.file.Path;

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
    private final BlockingPhoneImportRepositoryPort phoneImportRepository;
    private final FileUtil fileUtil;
    private final DelimitedLineTokenizer csvTokenizer =
            new DelimitedLineTokenizer();

    public SpringBatchPhoneImportJob(
            @Qualifier("batchImportSubmitter") BatchJobSubmitter submitter,
            @Lazy @Qualifier(PhoneImportUtils.UPLOAD_JOB) Job job,
            BlockingPhoneImportRepositoryPort phoneImportRepository,
            FileUtil fileUtil
    ) {
        this.submitter = submitter;
        this.job = job;
        this.phoneImportRepository = phoneImportRepository;
        this.fileUtil = fileUtil;
        this.csvTokenizer.setNames("key", "detail", "quantity");
    }

    public JobExecution submit(PhoneImportIngestionContext context) {
        PhoneImport phoneImport = context.phoneImport();
        Path input = context.inputFile();
        return submitter.submit(job, new JobParametersBuilder()
                .addLong("importId", phoneImport.getId())
                .addString("mode", phoneImport.getMode().name())
                .addString("inputFile", input.toAbsolutePath().toString())
                .toJobParameters(), () -> {
            try {
                writeImportResult(phoneImport.getId(), false);
            } finally {
                fileUtil.deleteQuietly(input);
            }
        });
    }

    private void writeImportResult(Long id, boolean importJobSucceeded) {
        phoneImportRepository.writeStatusResult(id, importJobSucceeded,
                PhoneImport.uploadResultStatuses(), null);
    }

    private JobExecutionListener statusListener() {
        return new JobExecutionListener() {
            @Override
            public void afterJob(JobExecution execution) {
                Long id = execution.getJobParameters().getLong("importId");
                try {
                    boolean uploadSucceeded = PhoneImportUtils.stepCompleted(
                            execution, PhoneImportUtils.UPLOAD_STEP);
                    if (!uploadSucceeded) {
                        writeImportResult(id, false);
                        return;
                    }
                    phoneImportRepository.writeValidationResult(id,
                            PhoneImportUtils.jobCompleted(execution),
                            PhoneDraft.approvableStatuses(), PhoneImport.uploadValidationResultStatuses());
                } finally {
                    // The job runs in the background, so it is the only owner of the file from here on.
                    fileUtil.deleteQuietly(execution.getJobParameters().getString("inputFile"));
                }
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
            @Value("#{jobParameters['importId']}") Long importId,
            BlockingPhoneDraftRepositoryPort phoneDraftRepository
    ) {
        return chunk -> phoneDraftRepository.saveAll(importId, chunk.getItems());
    }

    @Bean(PhoneImportUtils.UPLOAD_JOB)
    public Job phoneUploadJob(
            JobRepository repository,
            @Qualifier("jpaTransactionManager")
            PlatformTransactionManager transactionManager,
            @Qualifier("phoneCsvReader")
            FlatFileItemReader<PhoneImportSourceRow> reader,
            @Qualifier("phoneImportWriter")
            ItemWriter<PhoneDraft> writer,
            @Qualifier("phoneDraftProcessor")
            ItemProcessor<PhoneImportSourceRow, PhoneDraft> processor,
            @Qualifier(PhoneImportUtils.VALIDATE_STEP)
            Step validateStep
    ) {
        Step upload = new StepBuilder(
                PhoneImportUtils.UPLOAD_STEP, repository
        )
                .<PhoneImportSourceRow, PhoneDraft>chunk(
                        500, transactionManager
                )
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();

        return new JobBuilder(PhoneImportUtils.UPLOAD_JOB, repository)
                .preventRestart()
                .listener(statusListener())
                .start(upload)
                .next(validateStep)
                .build();
    }


}
