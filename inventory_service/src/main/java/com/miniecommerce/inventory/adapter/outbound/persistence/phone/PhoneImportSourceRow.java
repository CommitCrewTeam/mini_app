package com.miniecommerce.inventory.adapter.outbound.persistence.phone;

public record PhoneImportSourceRow(long rowNumber, String name, String detail, String active,
        String stock, String errorMessage) { }
