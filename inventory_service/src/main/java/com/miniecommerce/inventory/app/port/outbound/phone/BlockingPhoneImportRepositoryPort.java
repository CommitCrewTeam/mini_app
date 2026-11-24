package com.miniecommerce.inventory.app.port.outbound.phone;

import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportCASStatuses;
import com.miniecommerce.inventory.domain.enums.PhoneDraftStatus;
import java.util.List;

public interface BlockingPhoneImportRepositoryPort {
    boolean writeStatusResult(Long id, boolean operationSucceeded, PhoneImportCASStatuses statuses, String error);
    boolean writeValidationResult(Long id, boolean operationSucceeded, List<PhoneDraftStatus> approvableDraftStatuses,
            PhoneImportCASStatuses statuses);
}
