package com.miniecommerce.inventory.app.job;

import com.miniecommerce.inventory.domain.PhoneImportStatus;
import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import com.miniecommerce.inventory.adapter.outbound.mapper.PhoneDraftMapper;
import com.miniecommerce.inventory.adapter.outbound.persistence.phone.PhoneImportSourceRow;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneDraftRepositoryPort;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneImportRepositoryPort;
import com.miniecommerce.inventory.domain.ImportMode;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import com.miniecommerce.inventory.domain.PhoneDraft;
import com.miniecommerce.inventory.domain.PhoneImport;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
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

    private final JobLauncher launcher;
    private final Job job;
    private final Path directory;
    private final PhoneImportRepositoryPort imports;
    private final PhoneDraftRepositoryPort drafts;
    private final DelimitedLineTokenizer csvTokenizer =
            new DelimitedLineTokenizer();

    public SpringBatchPhoneImportJob(
            @Qualifier("batchImportJobLauncher") JobLauncher launcher,
            @Lazy @Qualifier("phoneImportJob") Job job,
            PhoneImportRepositoryPort imports,
            PhoneDraftRepositoryPort drafts,
            @Value("${app.phone-import.directory}") String directory
    ) {
        this.launcher = launcher;
        this.job = job;
        this.imports = imports;
        this.drafts = drafts;
        this.directory = Path.of(directory).toAbsolutePath().normalize();
        this.csvTokenizer.setNames("name", "detail", "active", "stock");
    }

    public Path allocateImportFile() throws IOException {
        Files.createDirectories(directory);
        return Files.createTempFile(directory, "phones-", ".csv");
    }

    public UUID launchPhoneImportJob(Path input, ImportMode mode)
            throws Exception {
        UUID id = UUID.randomUUID();
        // to handle it: concurrent uploads.
        imports.save(PhoneImport.pending(id, mode)).block();

        launcher.run(job, new JobParametersBuilder()
                .addString("importId", id.toString())
                .addString("mode", mode.name())
                .addString("inputFile", input.toAbsolutePath().toString())
                .toJobParameters());
        return id;
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
                    f.readString("name"), f.readString("detail"),
                    f.readString("active"), f.readString("stock"), null);
        } catch (Exception e) {
            return new PhoneImportSourceRow(
                    lineNumber, null, null, null, null, "Cannot parse CSV columns");
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
            @Value("#{jobParameters['importId']}") String importId
    ) {
        UUID id = UUID.fromString(importId);
        return chunk -> drafts.saveAll(id, chunk.getItems()).block();
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