package com.miniecommerce.inventory.adapter.outbound.persistence.jpa;

import com.miniecommerce.inventory.adapter.outbound.persistence.jpa.entity.PhoneDraftJpaEntity;
import com.miniecommerce.inventory.adapter.outbound.persistence.jpa.mapper.PhoneJpaMapper;
import com.miniecommerce.common.exception.AppException;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.liquibase.enabled=false"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = AtomicRestockIT.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
class AtomicRestockIT {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", postgres::getJdbcUrl);
        properties.add("spring.datasource.username", postgres::getUsername);
        properties.add("spring.datasource.password", postgres::getPassword);
    }

    @Configuration
    @EntityScan(basePackageClasses = PhoneDraftJpaEntity.class)
    @EnableJpaRepositories(basePackageClasses = PhoneDraftJpaRepository.class)
    @Import(BlockingPhoneDraftRepositoryAdapter.class)
    static class Config {
        @Bean(name = {"transactionManager", "jpaTransactionManager"})
        JpaTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }

        @Bean
        PhoneJpaMapper mapper() { return mock(PhoneJpaMapper.class); }
    }

    @Autowired BlockingPhoneDraftRepositoryAdapter drafts;
    @Autowired JdbcTemplate jdbc;
    @Autowired JpaTransactionManager transactionManager;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM phone_draft");
        jdbc.update("DELETE FROM phone");
        jdbc.update("INSERT INTO phone (key, detail, active, stock, version) VALUES ('phone', '{}', false, 10, 0)");
        jdbc.update("INSERT INTO phone_draft (id, import_id, row_number, key, quantity, status, version) "
                + "VALUES (1, 10, 1, 'phone', 3, 'VALID', 0)");
    }

    private void apply(List<Long> ids) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> drafts.applyRestock(10L, ids));
    }

    @Test
    void appliesBothTablesAndDoesNotIncrementAgainOnRetry() {
        apply(List.of(1L));
        assertEquals(13, jdbc.queryForObject("SELECT stock FROM phone", Integer.class));
        assertEquals(1L, jdbc.queryForObject("SELECT version FROM phone", Long.class));
        assertEquals("APPLIED", jdbc.queryForObject("SELECT status FROM phone_draft", String.class));
        assertEquals(1L, jdbc.queryForObject("SELECT version FROM phone_draft", Long.class));
        assertThrows(AppException.class, () -> apply(List.of(1L)));
        assertEquals(13, jdbc.queryForObject("SELECT stock FROM phone", Integer.class));
    }

    @Test
    void missingPhoneRollsBackEntireChunk() {
        jdbc.update("INSERT INTO phone_draft (id, import_id, row_number, key, quantity, status, version) "
                + "VALUES (2, 10, 2, 'missing', 5, 'VALID', 0)");
        assertThrows(AppException.class, () -> apply(List.of(1L, 2L)));
        assertEquals(10, jdbc.queryForObject("SELECT stock FROM phone", Integer.class));
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM phone_draft WHERE status = 'VALID' AND version = 0", Integer.class));
    }

    @Test
    void duplicateKeysRollBackEntireChunk() {
        jdbc.update("INSERT INTO phone_draft (id, import_id, row_number, key, quantity, status, version) "
                + "VALUES (2, 10, 2, 'phone', 5, 'VALID', 0)");
        assertThrows(AppException.class, () -> apply(List.of(1L, 2L)));
        assertEquals(10, jdbc.queryForObject("SELECT stock FROM phone", Integer.class));
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM phone_draft WHERE status = 'VALID'", Integer.class));
    }
}
