package com.miniecommerce.inventory.app.job;

import com.miniecommerce.inventory.app.port.outbound.phone.BlockingPhoneImportRepositoryPort;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportValidationContext;
import com.miniecommerce.inventory.domain.enums.ImportMode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ValidationAttemptsTest {
    @Test
    void revalidationUsesDistinctIdentifyingAttempts() throws Exception {
        JobLauncher launcher = mock(JobLauncher.class);
        Job job = mock(Job.class);
        when(launcher.run(eq(job), any())).thenReturn(new JobExecution(1L));
        var validation = new PhoneImportValidationJob(new BatchJobSubmitter(launcher), job,
                mock(BlockingPhoneImportRepositoryPort.class));
        var context = new PhoneImportValidationContext(42L, ImportMode.CREATE);
        validation.submitValidation(context);
        validation.submitValidation(context);
        var captor = ArgumentCaptor.forClass(JobParameters.class);
        verify(launcher, times(2)).run(eq(job), captor.capture());
        assertNotEquals(captor.getAllValues().getFirst().getString("attempt"), captor.getValue().getString("attempt"));
        assertTrue(captor.getValue().getParameter("attempt").isIdentifying());
    }
}
