package com.miniecommerce.inventory.domain;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import com.miniecommerce.inventory.domain.enums.ImportMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Phone {

    private Long id;
    /**
     * Optimistic-lock version of the persisted Phone aggregate, populated when the row is read.
     */
    private Long version;
    private String key;
    private Map<String, Object> detail;
    private boolean active;
    private int stock;

    public Phone() {
    }

    public Phone(Long id, String key, Map<String, Object> detail, boolean active, int stock) {
        this.id = id;
        this.key = key;
        this.detail = detail;
        this.active = active;
        this.stock = stock;
    }

    /** Imported phones start inactive until explicitly activated. */
    public static Phone fromImport(String key, Map<String, Object> detail, int quantity) {
        Phone phone = new Phone(null, key, detail, false, quantity);
        phone.validate();
        return phone;
    }

    /**
     * Builds the Phone row to persist for one approved draft.
     *
     * <p>For UPDATE/RESTOCK {@code snapshot} is the Phone read inside the approval transaction,
     * and the draft's {@code matchedVersion} (captured during validation) must still equal the
     * snapshot's {@link #version}; otherwise the phone changed since validation and the approval
     * is rejected with a conflict. The change is applied to the snapshot in place so the
     * optimistic-lock version that was read is the same one the save checks and increments.</p>
     *
     * <p>For CREATE no snapshot exists and a brand-new inactive Phone is built from the draft.</p>
     */
    public static Phone prepareImportChange(ImportMode mode, PhoneDraft draft, Phone snapshot) {
        if (!mode.requiresExistingPhone()) {
            return fromImport(draft.getKey(), draft.getDetail(), draft.getQuantity());
        }
        if (snapshot == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "Phone not found");
        }
        if (draft.getMatchedVersion() == null
                || !draft.getMatchedVersion().equals(snapshot.getVersion())) {
            throw new AppException(ErrorCode.CONFLICT, "Phone changed after approval validation");
        }
        // The snapshot was read for this transaction only, so change it in place.
        // Keeping the same object also keeps the version that was read for the save to check.
        switch (mode) {
            case UPDATE -> snapshot.updateFromImport(draft.getDetail());
            case RESTOCK -> snapshot.restock(draft.getQuantity());
            case CREATE -> throw new IllegalStateException("CREATE was handled before loading a snapshot");
        }
        return snapshot;
    }

    /** Replace only the imported details. */
    public void updateFromImport(Map<String, Object> detail) {
        this.detail = detail;
    }

    /** Add stock without changing the phone's other information. */
    public void restock(int quantity) {
        requireNonNegativeQuantity(quantity);
        this.stock = Math.addExact(this.stock, quantity);
    }

    private static void requireNonNegativeQuantity(int quantity) {
        if (quantity < 0) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Stock quantity must not be negative");
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Map<String, Object> getDetail() {
        return detail;
    }

    public void setDetail(Map<String, Object> detail) {
        this.detail = detail;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public boolean isAvailable() {
        return active && stock > 0;
    }

    public void validate() {
        List<String> violations = new ArrayList<>();
        if (key == null || key.isBlank()) {
            violations.add("key must not be blank");
        }
        if (stock < 0) {
            violations.add("stock must not be negative");
        }
        if (!violations.isEmpty()) {
            throw new AppException(
                    ErrorCode.BAD_REQUEST,
                    "Invalid Phone: " + String.join("; ", violations));
        }
    }
}
