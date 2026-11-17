package com.miniecommerce.inventory.domain.enums;

public enum ImportMode {
    CREATE,
    UPDATE,
    RESTOCK;

    public boolean requiresExistingPhone() {
        return this != CREATE;
    }
}
