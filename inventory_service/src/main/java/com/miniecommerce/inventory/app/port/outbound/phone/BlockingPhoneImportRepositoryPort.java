package com.miniecommerce.inventory.app.port.outbound.phone;

import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportStatuses;
import com.miniecommerce.inventory.domain.enums.PhoneDraftStatus;

public interface BlockingPhoneImportRepositoryPort {
    boolean writeStatusResult(Long id, boolean operationSucceeded, PhoneImportStatuses statuses, String error);
    boolean writeValidationResult(Long id, boolean operationSucceeded, PhoneDraftStatus validDraftStatus,
            PhoneImportStatuses statuses);
}
