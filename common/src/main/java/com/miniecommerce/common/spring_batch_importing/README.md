# Shared Spring Batch file importing

Reusable infrastructure for services that import a staged file with Spring Batch 5.

## Components

- `BatchImportInfrastructure`: JDBC transaction manager, executor and an asynchronous launcher named `batchImportJobLauncher`. Opt in using `@Import`. Spring Boot auto-configuration supplies the Batch repository, explorer and scopes; this class does not use `@EnableBatchProcessing`.

## Integrating another service

1. Add `spring-boot-starter-batch` to the service. Common declares Batch/JDBC as compile-only so unrelated consumers do not inherit a Batch runtime.
2. Provide a JDBC DataSource named `batchDataSource`, annotated with `@BatchDataSource`; explicitly import `BatchImportInfrastructure` from the service's configuration.
3. Set `spring.batch.jdbc.initialize-schema=always` to let Spring Boot initialize the Batch metadata tables using Spring Batch's database-specific SQL script. Set `spring.batch.job.enabled=false` for jobs triggered by requests.
4. Define the Job and its steps in the service's outbound adapter. Each service owns its readers, processors, writers, validation steps and execution flow. A file reader may bind `jobParameters['inputFile']` in step scope.
5. Inject `@Qualifier("batchImportJobLauncher") JobLauncher` and `JobExplorer` into the service's adapter to launch jobs asynchronously and query executions. The service owns job parameters, job-name checks and application status mapping.
6. Invoke blocking launch/query from a worker scheduler when using WebFlux.

Settings: `app.batch-import.worker-count` (2), `queue-capacity` (20), `shutdown-wait-seconds` (30). One imported infrastructure per application; multiple jobs share it. Services own datasource settings and pool sizing.

The shared package does not register jobs, stage uploads, validate business data, choose SQL/chunk size, migrate databases or supply domain use cases. Those remain service responsibilities, including restart and deduplication policies.
