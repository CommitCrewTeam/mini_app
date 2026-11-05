package com.miniecommerce.inventory.adapter.outbound.mapper;

import com.miniecommerce.inventory.domain.PhoneDraftStatus;
import com.miniecommerce.inventory.adapter.outbound.persistence.phone.PhoneImportSourceRow;
import com.miniecommerce.inventory.domain.PhoneDraft;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.mapstruct.AfterMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public abstract class PhoneDraftMapper {

    @Autowired
    protected ObjectMapper objectMapper;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "key", source = "name")
    @Mapping(target = "detail", ignore = true)
    @Mapping(target = "quantity", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(target = "updateKey", ignore = true)
    @Mapping(target = "matchedVersion", ignore = true)
    public abstract PhoneDraft toDraft(PhoneImportSourceRow row);

    @AfterMapping
    public void applyParseResult(
            PhoneImportSourceRow row,
            @MappingTarget PhoneDraft draft
    ) {
        if (row.errorMessage() != null) {
            draft.setKey(null);
            draft.setStatus(PhoneDraftStatus.INVALID);
            draft.setErrorMessage(row.errorMessage());
            return;
        }

        try {
            draft.setQuantity(Integer.valueOf(row.stock()));
            draft.setStatus(PhoneDraftStatus.PENDING);
        } catch (NumberFormatException e) {
            draft.setStatus(PhoneDraftStatus.INVALID);
            draft.setErrorMessage("Cannot parse stock integer");
            return;
        }

        try {
            Map<String, Object> detail = objectMapper.readValue(
                    row.detail(), new TypeReference<Map<String, Object>>() { });
            if (detail == null) {
                throw new IllegalArgumentException("Detail must be a JSON object");
            }
            draft.setDetail(detail);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            draft.setStatus(PhoneDraftStatus.INVALID);
            draft.setErrorMessage("Cannot parse detail JSON object");
        }
    }
}
