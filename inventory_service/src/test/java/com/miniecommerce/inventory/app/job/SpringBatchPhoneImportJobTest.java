//package com.miniecommerce.inventory.app.job;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.Mockito.*;
//import com.miniecommerce.inventory.adapter.outbound.persistence.phone.PhoneImportBatchStore;
//import com.miniecommerce.inventory.app.port.outbound.phone.*;
//import com.miniecommerce.inventory.domain.*;
//import java.util.*;
//import org.junit.jupiter.api.Test;
//import org.springframework.batch.core.Job;
//import org.springframework.batch.core.launch.JobLauncher;
//import reactor.core.publisher.Flux;
//
//class SpringBatchPhoneImportJobTest {
//    private final PhoneRepositoryPort phones = mock(PhoneRepositoryPort.class);
//    private final PhoneImportBatchStore store = mock(PhoneImportBatchStore.class);
//    private final UUID importId = UUID.randomUUID();
//    private final SpringBatchPhoneImportJob config = new SpringBatchPhoneImportJob(
//            mock(JobLauncher.class), mock(Job.class), mock(Job.class), mock(Job.class), store,
//            mock(PhoneImportRepositoryPort.class), mock(PhoneDraftRepositoryPort.class), phones, "build/imports");
//
//    private PhoneDraft draft(String status) {
//        var draft = new PhoneDraft(2, "phone", 5, status, null, null, null);
//        draft.setDetail(Map.of());
//        draft.setActive(false);
//        return draft;
//    }
//
//    @Test
//    void revalidatesPreviouslyValidRowsAgainstCurrentPhones() throws Exception {
//        when(phones.findImportMatchesByKeys(anyList())).thenReturn(Flux.just(new PhoneImportMatch(1L, "phone", 3L)));
//        var draft = draft("VALID");
//        config.phoneValidationProcessor("APPEND", importId.toString()).process(List.of(draft));
//        assertThat(draft.getStatus()).isEqualTo("INVALID");
//        assertThat(draft.getErrorMessage()).contains("already exists");
//    }
//
//    @Test
//    void previouslyInvalidBusinessRuleCanBecomeValid() throws Exception {
//        when(phones.findImportMatchesByKeys(anyList())).thenReturn(Flux.empty());
//        var draft = draft("INVALID");
//        config.phoneValidationProcessor("APPEND", importId.toString()).process(List.of(draft));
//        assertThat(draft.getStatus()).isEqualTo("VALID");
//        assertThat(config.phoneApprovalProcessor().process(draft).phone().isActive()).isFalse();
//    }
//
//    @Test
//    void preservesParseFailures() throws Exception {
//        var draft = draft("INVALID");
//        draft.setQuantity(null);
//        draft.setErrorMessage("Cannot parse stock integer");
//        config.phoneValidationProcessor("APPEND", importId.toString()).process(List.of(draft));
//        assertThat(draft.getStatus()).isEqualTo("INVALID");
//        assertThat(draft.getErrorMessage()).isEqualTo("Cannot parse stock integer");
//        verifyNoInteractions(phones);
//    }
//
//    @Test
//    void restockCapturesVersionAndNegativeStockFails() throws Exception {
//        when(phones.findImportMatchesByKeys(anyList())).thenReturn(Flux.just(new PhoneImportMatch(1L, "phone", 7L)));
//        var draft = draft("VALID");
//        config.phoneValidationProcessor("RESTOCK", importId.toString()).process(List.of(draft));
//        assertThat(draft.getMatchedVersion()).isEqualTo(7L);
//        draft.setQuantity(-1);
//        config.phoneValidationProcessor("RESTOCK", importId.toString()).process(List.of(draft));
//        assertThat(draft.getStatus()).isEqualTo("INVALID");
//    }
//}
