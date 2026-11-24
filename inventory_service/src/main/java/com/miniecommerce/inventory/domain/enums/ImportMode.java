package com.miniecommerce.inventory.domain.enums;

public enum ImportMode {
    CREATE,
    UPDATE,
    RESTOCK;

    public boolean requiresExistingPhone() {
        return this != CREATE;
    }

    /**
     * Only UPDATE rewrites a Phone from a snapshot, so only UPDATE needs a {@code Phone.version}
     * baseline. RESTOCK adds stock atomically in SQL and never compares versions.
     */
    public boolean tracksPhoneVersion() {
        return this == UPDATE;
    }
}
