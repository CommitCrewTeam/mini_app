//package com.miniecommerce.inventory.adapter.inbound.rest;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.tuple;
//
//import com.miniecommerce.inventory.adapter.outbound.persistence.phone.PhoneDraftEntity;
//import com.miniecommerce.inventory.adapter.outbound.persistence.phone.PhoneDraftSpringDataRepository;
//import com.miniecommerce.inventory.adapter.outbound.persistence.phone.PhoneImportEntity;
//import com.miniecommerce.inventory.adapter.outbound.persistence.phone.PhoneImportSpringDataRepository;
//import com.miniecommerce.inventory.domain.ImportMode;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.time.Duration;
//import java.util.List;
//import java.util.UUID;
//import java.util.concurrent.TimeUnit;
//import java.util.function.BooleanSupplier;
//import javax.sql.DataSource;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.core.io.Resource;
//import org.springframework.http.MediaType;
//import org.springframework.http.client.MultipartBodyBuilder;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.springframework.test.web.reactive.server.WebTestClient;
//import org.springframework.web.reactive.function.BodyInserters;
//import org.testcontainers.containers.PostgreSQLContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@AutoConfigureWebTestClient
//@Testcontainers
//class PhoneImportJobIT {
//
//    private static final Path IMPORT_DIR = createImportDirectory();
//
//    @Container
//    @ServiceConnection
//    static final PostgreSQLContainer<?> POSTGRES =
//            new PostgreSQLContainer<>("postgres:16");
//
//    @Autowired
//    private WebTestClient client;
//
//    @Autowired
//    private PhoneImportSpringDataRepository imports;
//
//    @Autowired
//    private PhoneDraftSpringDataRepository drafts;
//
//    @Autowired
//    @Qualifier("batchDataSource")
//    private DataSource batchDataSource;
//
//    @DynamicPropertySource
//    static void datasourceProperties(DynamicPropertyRegistry registry) {
//        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://%s:%d/%s".formatted(
//                POSTGRES.getHost(), POSTGRES.getMappedPort(5432),
//                POSTGRES.getDatabaseName()));
//        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
//        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
//
//        // Liquibase uses its own JDBC connection, not the R2DBC one.
//        registry.add("spring.liquibase.url", POSTGRES::getJdbcUrl);
//        registry.add("spring.liquibase.user", POSTGRES::getUsername);
//        registry.add("spring.liquibase.password", POSTGRES::getPassword);
//
//        // The application's @BatchDataSource is configured from app.* properties,
//        // so @ServiceConnection cannot wire it automatically.
//        registry.add("app.phone-import.datasource.jdbc-url", POSTGRES::getJdbcUrl);
//        registry.add("app.phone-import.datasource.username", POSTGRES::getUsername);
//        registry.add("app.phone-import.datasource.password", POSTGRES::getPassword);
//
//        registry.add("app.phone-import.directory", IMPORT_DIR::toString);
//        registry.add("app.phone-import.max-bytes", () -> 20 * 1024 * 1024);
//    }
//
//    @AfterEach
//    void removeUploadedFiles() throws IOException {
//        if (!Files.exists(IMPORT_DIR)) {
//            return;
//        }
//        try (var files = Files.list(IMPORT_DIR)) {
//            for (Path file : files.toList()) {
//                Files.deleteIfExists(file);
//            }
//        }
//    }
//
//    @Test
//    void uploadsCsvAndPersistsImportAndDraftsInPostgres() throws Exception {
//        Resource sample = new ClassPathResource("import/sample.csv");
//
//        MultipartBodyBuilder multipart = new MultipartBodyBuilder();
//        multipart.part("file", sample).filename("sample.csv");
//        multipart.part("mode", "APPEND");
//
//        client.post()
//                .uri("/api/phones/imports")
//                .contentType(MediaType.MULTIPART_FORM_DATA)
//                .body(BodyInserters.fromMultipartData(multipart.build()))
//                .exchange()
//                .expectStatus().isAccepted()
//                .expectBody()
//                .jsonPath("$.success").isEqualTo(true)
//                .jsonPath("$.message").isEqualTo("Phone import submitted");
//
//        PhoneImportEntity importRow = imports.findAll()
//                .single()
//                .block(Duration.ofSeconds(5));
//
//        assertThat(importRow).isNotNull();
//        assertThat(importRow.getId()).isNotNull();
//        assertThat(importRow.getStatus()).isIn("PENDING", "READY");
//        assertThat(importRow.getMode()).isEqualTo(ImportMode.APPEND);
//
//        UUID importId = importRow.getId();
//        List<PhoneDraftEntity> savedDrafts = awaitDrafts(importId);
//
//        assertThat(savedDrafts)
//                .extracting(PhoneDraftEntity::getRowNumber, PhoneDraftEntity::getKey,
//                        PhoneDraftEntity::getQuantity, PhoneDraftEntity::getStatus)
//                .containsExactlyInAnyOrder(
//                        tuple(2L, "iphone 15", 10, "VALID"),
//                        tuple(3L, "galaxy s24", 5, "VALID"),
//                        tuple(4L, "pixel 8", 0, "VALID")
//                );
//        assertThat(savedDrafts).allSatisfy(draft -> {
//            assertThat(draft.getId()).isNotNull();
//            assertThat(draft.getImportId()).isEqualTo(importId);
//        });
//
//        JdbcTemplate jdbc = new JdbcTemplate(batchDataSource);
//
//        // Cross-check through JDBC, proving the rows are really in PostgreSQL
//        // and visible to the batch datasource, not just in the R2DBC connection.
//        assertThat(jdbc.queryForObject(
//                "select count(*) from phone_draft where import_id = ?",
//                Integer.class, importId)).isEqualTo(3);
//
//        assertThat(jdbc.queryForObject(
//                "select detail ->> 'storage' from phone_draft where import_id = ? and row_number = 2",
//                String.class, importId)).isEqualTo("256GB");
//        assertThat(jdbc.queryForObject(
//                "select detail ->> 'color' from phone_draft where import_id = ? and row_number = 3",
//                String.class, importId)).isEqualTo("white");
//
//        await(Duration.ofSeconds(10), () -> jdbc.queryForObject(
//                "select count(*) from BATCH_JOB_EXECUTION where STATUS = 'COMPLETED'",
//                Integer.class) >= 1);
//
//        assertThat(jdbc.queryForObject(
//                "select count(*) from BATCH_JOB_EXECUTION where STATUS = 'COMPLETED'",
//                Integer.class)).isGreaterThanOrEqualTo(1);
//
//        await(Duration.ofSeconds(10), () -> "READY".equals(imports.findById(importId).block().getStatus()));
//        // A phone inserted after upload must be detected by the separate validation job.
//        jdbc.update("insert into phone (name, key, detail, active, stock, version) values ('iphone 15', 'iphone 15', '{}'::jsonb, true, 1, 0)");
//        client.post().uri("/api/phones/imports/{id}/validate", importId).exchange().expectStatus().isAccepted();
//        await(Duration.ofSeconds(10), () -> "READY".equals(imports.findById(importId).block().getStatus()));
//        assertThat(drafts.findAllByImportId(importId).collectList().block())
//                .anySatisfy(row -> {
//                    assertThat(row.getKey()).isEqualTo("iphone 15");
//                    assertThat(row.getStatus()).isEqualTo("INVALID");
//                });
//        assertThat(jdbc.queryForObject("select count(*) from BATCH_STEP_EXECUTION s join BATCH_JOB_EXECUTION e on s.JOB_EXECUTION_ID = e.JOB_EXECUTION_ID join BATCH_JOB_INSTANCE i on e.JOB_INSTANCE_ID = i.JOB_INSTANCE_ID where i.JOB_NAME = 'phoneValidateJob'", Integer.class)).isEqualTo(1);
//        client.post().uri("/api/phones/imports/{id}/approve", importId).exchange().expectStatus().isAccepted();
//        await(Duration.ofSeconds(10), () -> "READY".equals(imports.findById(importId).block().getStatus()));
//        assertThat(jdbc.queryForObject("select count(*) from phone", Integer.class)).isEqualTo(1);
//        assertThat(jdbc.queryForObject("select count(*) from phone_draft where status = 'APPLIED'", Integer.class)).isZero();
//        // Approval itself revalidates the previously INVALID draft after the conflict is removed.
//        jdbc.update("delete from phone where key = 'iphone 15'");
//        client.post().uri("/api/phones/imports/{id}/approve", importId).exchange().expectStatus().isAccepted();
//        await(Duration.ofSeconds(10), () -> "DONE".equals(imports.findById(importId).block().getStatus()));
//        assertThat(imports.findById(importId).block().getStatus()).isEqualTo("DONE");
//        assertThat(jdbc.queryForObject("select count(*) from phone", Integer.class)).isEqualTo(3);
//        assertThat(jdbc.queryForObject("select active from phone where key = 'pixel 8'", Boolean.class)).isFalse();
//        assertThat(drafts.findAllByImportId(importId).collectList().block()).allSatisfy(row -> assertThat(row.getStatus()).isEqualTo("APPLIED"));
//        client.post().uri("/api/phones/imports/{id}/approve", importId).exchange().expectStatus().isEqualTo(409);
//        client.get().uri("/api/phones/imports/{id}", importId).exchange().expectStatus().isOk()
//                .expectBody().jsonPath("$.data.phoneImport.status").isEqualTo("DONE");
//
//    }
//
//    private List<PhoneDraftEntity> awaitDrafts(UUID importId) throws InterruptedException {
//        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
//        List<PhoneDraftEntity> rows = List.of();
//        while (System.nanoTime() < deadline) {
//            rows = drafts.findAllByImportId(importId)
//                    .collectList()
//                    .block(Duration.ofSeconds(5));
//            if (rows != null && rows.size() == 3 && rows.stream().allMatch(row -> "VALID".equals(row.getStatus()))) {
//                return rows;
//            }
//            Thread.sleep(200);
//        }
//        return rows == null ? List.of() : rows;
//    }
//
//    private static void await(Duration timeout, BooleanSupplier condition)
//            throws InterruptedException {
//        long deadline = System.nanoTime() + timeout.toNanos();
//        while (System.nanoTime() < deadline) {
//            if (condition.getAsBoolean()) {
//                return;
//            }
//            Thread.sleep(100);
//        }
//    }
//
//    private static Path createImportDirectory() {
//        try {
//            return Files.createTempDirectory("inventory-phone-import-it-");
//        } catch (IOException e) {
//            throw new IllegalStateException("Cannot create import directory", e);
//        }
//    }
//}
