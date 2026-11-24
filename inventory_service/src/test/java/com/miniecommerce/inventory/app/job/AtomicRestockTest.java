package com.miniecommerce.inventory.app.job;

import com.miniecommerce.inventory.app.port.outbound.phone.BlockingPhoneDraftRepositoryPort;
import com.miniecommerce.inventory.app.port.outbound.phone.BlockingPhoneRepositoryPort;
import com.miniecommerce.inventory.domain.PhoneDraft;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportMatch;
import com.miniecommerce.inventory.domain.enums.ImportMode;
import com.miniecommerce.inventory.domain.enums.PhoneDraftStatus;
import java.util.Map;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.item.Chunk;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AtomicRestockTest {
    private PhoneDraft draft() {
        var draft = new PhoneDraft(1L, "phone", 3, PhoneDraftStatus.VALID, null, 1L);
        draft.setDetail(Map.of());
        return draft;
    }

    @Test
    void restockClearsOldBaselineAndAcceptsChangingVersions() {
        var draft = draft();
        draft.validateForImport(ImportMode.RESTOCK, false, new PhoneImportMatch(1L, "phone", 2L));
        draft.validateForImport(ImportMode.RESTOCK, false, new PhoneImportMatch(1L, "phone", 3L));
        assertEquals(PhoneDraftStatus.VALID, draft.getStatus());
        assertNull(draft.getMatchedVersion());
        draft.validateForImport(ImportMode.RESTOCK, false, null);
        assertEquals(PhoneDraftStatus.INVALID, draft.getStatus());
        draft.setQuantity(-1);
        draft.validateForImport(ImportMode.RESTOCK, false, new PhoneImportMatch(1L, "phone", 3L));
        assertEquals(PhoneDraftStatus.INVALID, draft.getStatus());
    }

    @Test
    void updateStillWarnsOnChangedVersion() {
        var draft = draft();
        draft.validateForImport(ImportMode.UPDATE, false, new PhoneImportMatch(1L, "phone", 2L));
        assertEquals(PhoneDraftStatus.WARNING, draft.getStatus());
        assertEquals(1L, draft.getMatchedVersion());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void restockWriterDelegatesToAtomicApplyRegardlessOfWarningSkip(boolean warningSkip) throws Exception {
        var phones = mock(BlockingPhoneRepositoryPort.class);
        var drafts = mock(BlockingPhoneDraftRepositoryPort.class);
        var draft = draft();
        draft.setId(7L);
        draft.setMatchedVersion(null);
        var writer = new ApproveImportJob.ApprovalConfiguration()
                .phoneApprovalWriter(10L, "RESTOCK", warningSkip, drafts, phones);
        writer.write(new Chunk<>(draft));
        verify(drafts).applyRestock(10L, List.of(7L));
        verifyNoMoreInteractions(drafts);
        verifyNoInteractions(phones);
    }

    @Test
    void emptyRestockChunkDoesNotWrite() throws Exception {
        var phones = mock(BlockingPhoneRepositoryPort.class);
        var drafts = mock(BlockingPhoneDraftRepositoryPort.class);
        var writer = new ApproveImportJob.ApprovalConfiguration()
                .phoneApprovalWriter(10L, "RESTOCK", false, drafts, phones);
        writer.write(new Chunk<>());
        verifyNoInteractions(phones, drafts);
    }
}
