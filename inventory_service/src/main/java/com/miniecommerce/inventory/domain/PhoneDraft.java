package com.miniecommerce.inventory.domain;

import com.miniecommerce.inventory.domain.enums.ImportMode;
import com.miniecommerce.inventory.domain.enums.PhoneDraftStatus;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportMatch;
import java.util.Locale;
import java.util.Map;

public class PhoneDraft {

    private Long id;
    /**
     * Optimistic-lock version of the phone_draft row itself, used to detect the draft being
     * modified between the approval reader and the approval writer. This is NOT the version of
     * the Phone aggregate; that baseline lives in {@link #matchedVersion}.
     */
    private Long version;
    private long rowNumber;
    private String key;
    private Map<String, Object> detail;
    private Integer quantity;
    private PhoneDraftStatus status;
    private String errorMessage;
    /**
     * Phone.version observed when validation last matched this draft against the phone table.
     * It is a point-in-time baseline, not a live value: approval re-reads the Phone snapshot and
     * rejects the change if the phone has moved past this version in the meantime.
     */
    private Long matchedVersion;

    public PhoneDraft() {
    }

    public PhoneDraft(long rowNumber, String key, Integer quantity, PhoneDraftStatus status,
            String errorMessage, Long matchedVersion) {
        this(null, rowNumber, key, quantity, status, errorMessage, matchedVersion);
    }

    public PhoneDraft(Long id, long rowNumber, String key, Integer quantity, PhoneDraftStatus status,
            String errorMessage, Long matchedVersion) {
        this.id = id;
        this.rowNumber = rowNumber;
        this.key = normalize(key);
        this.quantity = quantity;
        this.status = status;
        this.errorMessage = errorMessage;
        this.matchedVersion = matchedVersion;
    }





    public void validateForImport(ImportMode mode, boolean duplicate, PhoneImportMatch match) {
        if (status == PhoneDraftStatus.APPLIED) {
            return;
        }
        // Preserve parse errors; these rows need a new CSV upload.
        if (quantity == null || detail == null) {
            if (status != PhoneDraftStatus.INVALID) {
                setStatus(PhoneDraftStatus.INVALID);
                errorMessage = "Missing parsed fields; upload the CSV again";
            }
            return;
        }
        // Re-validation always recomputes the Phone baseline; a value left over from a previous
        // validation run must never survive into approval.
        matchedVersion = null;
        errorMessage = null;
        if (duplicate) {
            errorMessage = "Duplicate key in import";
        } else if (key == null || key.isBlank()) {
            errorMessage = "Key is required";
        } else if (quantity < 0) {
            errorMessage = "Stock must not be negative";
        } else if (mode == ImportMode.CREATE && match != null) {
            errorMessage = "Key already exists in phone";
        } else if (mode != ImportMode.CREATE && match == null) {
            errorMessage = "Key does not exist in phone";
        } else if (match != null) {
            // Capture the current Phone.version so approval can detect updates that happened
            // after this validation and fail with a conflict instead of overwriting them.
            matchedVersion = match.version();
        }
        setStatus(errorMessage == null ? PhoneDraftStatus.VALID : PhoneDraftStatus.INVALID);
    }

    private void requireValidForApproval() {
        if (status != PhoneDraftStatus.VALID) {
            throw new IllegalStateException("Draft is no longer valid after approval validation");
        }
    }

    public void markApplied() {
        requireValidForApproval();
        setStatus(PhoneDraftStatus.APPLIED);
    }

    private static String normalize(String key) {
        return key == null ? null : key.trim().toLowerCase(Locale.ROOT);
    }

    public Long getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(long rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = normalize(key);
    }

    public Map<String, Object> getDetail() {
        return detail;
    }

    public void setDetail(Map<String, Object> detail) {
        this.detail = detail;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public PhoneDraftStatus getStatus() {
        return status;
    }

    // Direct state assignment for construction, restore and re-validation;
    // approval goes through requireValidForApproval/markApplied.
    public void setStatus(PhoneDraftStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getMatchedVersion() {
        return matchedVersion;
    }

    public void setMatchedVersion(Long matchedVersion) {
        this.matchedVersion = matchedVersion;
    }
}
