package com.miniecommerce.inventory.app.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import com.miniecommerce.inventory.adapter.outbound.persistence.phone.PhoneDraftPersistenceAdapter;
import com.miniecommerce.inventory.adapter.outbound.persistence.phone.PhonePersistenceAdapter;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneDraftRepositoryPort;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneRepositoryPort;
import com.miniecommerce.inventory.domain.PhoneDraft;
import com.miniecommerce.inventory.domain.enums.ImportMode;
import io.r2dbc.postgresql.codec.Json;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

/**
 * Proves the approval chunk is a single transaction. The phone write completes first,
 * then the draft write fails with an optimistic lock: both must be rolled back.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class ApprovalChunkRollbackIT {

    private static final long PHONE_ID = 1L;
    private static final long IMPORT_ID = 1L;
    private static final long DRAFT_ID = 1L;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://" + POSTGRES.getHost() + ":"
                + POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT) + "/" + POSTGRES.getDatabaseName());
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
        registry.add("spring.liquibase.url", POSTGRES::getJdbcUrl);
        registry.add("spring.liquibase.user", POSTGRES::getUsername);
        registry.add("spring.liquibase.password", POSTGRES::getPassword);
        registry.add("app.phone-import.datasource.jdbc-url", POSTGRES::getJdbcUrl);
        registry.add("app.phone-import.datasource.username", POSTGRES::getUsername);
        registry.add("app.phone-import.datasource.password", POSTGRES::getPassword);
        registry.add("app.phone-import.directory", () -> "build/test-phone-imports");
    }

    @Autowired
    DatabaseClient client;

    @Autowired
    PhoneRepositoryPort phoneRepository;

    @Autowired
    PhoneDraftRepositoryPort draftRepository;

    @Autowired
    @Qualifier("inventoryTransactions")
    TransactionalOperator transactions;

    @Test
    void rollsBackPhoneWriteWhenDraftSaveFails() {
        seedPhoneImportAndDraft();

        var phoneWriteCompleted = new AtomicBoolean();
        // @Repository beans are wrapped in a persistence-exception proxy, so spy the target.
        PhonePersistenceAdapter phoneAdapter = AopTestUtils.getUltimateTargetObject(phoneRepository);
        var phones = spy(phoneAdapter);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Mono<Void> write = (Mono<Void>) invocation.callRealMethod();
            return write.doOnSuccess(ignored -> phoneWriteCompleted.set(true));
        }).when(phones).saveChanges(anyList());

        // Simulate a concurrent edit: by the time the drafts are saved, the rows carry a
        // newer version than the one read inside this transaction, so the UPDATE hits 0 rows.
        PhoneDraftPersistenceAdapter draftAdapter = AopTestUtils.getUltimateTargetObject(draftRepository);
        var drafts = spy(draftAdapter);
        doAnswer(invocation -> {
            List<PhoneDraft> chunk = invocation.<List<PhoneDraft>>getArgument(1);
            chunk.forEach(draft -> draft.setVersion(draft.getVersion() + 1));
            return draftRepository.saveAll(invocation.getArgument(0), chunk);
        }).when(drafts).saveAll(any(), anyList());

        var rows = List.of(draftRepository.findById(IMPORT_ID, DRAFT_ID).block());

        assertThatThrownBy(() -> ApproveImportJob
                .applyChunk(drafts, phones, IMPORT_ID, ImportMode.RESTOCK, rows)
                .as(transactions::transactional)
                .block())
                .isInstanceOfSatisfying(AppException.class, error -> {
                    assertThat(error.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                    // The draft adapter's conflict, so the phone write had already run.
                    assertThat(error.getMessage()).contains("Draft");
                });

        assertThat(phoneWriteCompleted).isTrue();
        // Stock would be 15 and version 1 if the phone write had been committed.
        assertThat(phoneSnapshot()).isEqualTo(tuple(10, 0L));
        assertThat(draftSnapshot()).isEqualTo(tuple("VALID", 0L));
    }

    private void seedPhoneImportAndDraft() {
        client.sql("""
                INSERT INTO phone (id, name, key, detail, active, stock, version)
                VALUES (:id, :name, :key, :detail, true, 10, 0)
                """)
                .bind("id", PHONE_ID)
                .bind("name", "iphone")
                .bind("key", "iphone")
                .bind("detail", Json.of("{\"brand\":\"apple\"}"))
                .then().block();

        // DONE is outside uq_one_active_import, so it can coexist with a running import.
        client.sql("""
                INSERT INTO phone_import (id, status, mode, version, created_at, updated_at)
                VALUES (:id, 'DONE', 'RESTOCK', 0, now(), now())
                """)
                .bind("id", IMPORT_ID)
                .then().block();

        client.sql("""
                INSERT INTO phone_draft
                    (id, version, import_id, row_number, key, detail, quantity, status, matched_version)
                VALUES (:id, 0, :importId, 1, :key, :detail, 5, 'VALID', 0)
                """)
                .bind("id", DRAFT_ID)
                .bind("importId", IMPORT_ID)
                .bind("key", "iphone")
                .bind("detail", Json.of("{\"brand\":\"apple\"}"))
                .then().block();
    }

    private Tuple phoneSnapshot() {
        return client.sql("SELECT stock, version FROM phone WHERE id = :id")
                .bind("id", PHONE_ID)
                .map((row, metadata) -> tuple(row.get("stock", Integer.class), row.get("version", Long.class)))
                .one().block();
    }

    private Tuple draftSnapshot() {
        return client.sql("SELECT status, version FROM phone_draft WHERE id = :id")
                .bind("id", DRAFT_ID)
                .map((row, metadata) -> tuple(row.get("status", String.class), row.get("version", Long.class)))
                .one().block();
    }
}
