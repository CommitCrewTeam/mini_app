package com.miniecommerce.inventory.app.port.outbound.phone;

import com.miniecommerce.inventory.domain.Phone;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportMatch;
import java.util.List;

public interface BlockingPhoneRepositoryPort {
    List<Phone> findByKeys(List<String> keys);
    List<PhoneImportMatch> findImportMatchesByKeys(List<String> keys);
    void saveChanges(List<Phone> phones);
}
