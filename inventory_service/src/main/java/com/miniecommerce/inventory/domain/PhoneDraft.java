package com.miniecommerce.inventory.domain;

import java.util.Locale;
import java.util.Map;

public class PhoneDraft {

    private Long id;
    private long rowNumber;
    private String key;
    private Map<String, Object> detail;
    private Integer quantity;
    private PhoneDraftStatus status;
    private String errorMessage;
    private String updateKey;
    private Long matchedVersion;

    public PhoneDraft() {
    }

    public PhoneDraft(long rowNumber, String key, Integer quantity, PhoneDraftStatus status,
            String errorMessage, String updateKey, Long matchedVersion) {
        this(null, rowNumber, key, quantity, status, errorMessage, updateKey, matchedVersion);
    }

    public PhoneDraft(Long id, long rowNumber, String key, Integer quantity, PhoneDraftStatus status,
            String errorMessage, String updateKey, Long matchedVersion) {
        this.id = id;
        this.rowNumber = rowNumber;
        this.key = normalize(key);
        this.quantity = quantity;
        this.status = status;
        this.errorMessage = errorMessage;
        this.updateKey = updateKey;
        this.matchedVersion = matchedVersion;
    }





    public void validateAgainstPhone(ImportMode mode, PhoneImportMatch match) {
        if (status != PhoneDraftStatus.PENDING) {
            return;
        }
        updateKey = null;
        matchedVersion = null;
        errorMessage = null;
        if (key == null || key.isBlank()) {
            errorMessage = "Key is required";
        } else if (quantity == null || quantity < 0) {
            errorMessage = "Stock must not be negative";
        } else if (mode == ImportMode.APPEND && match != null) {
            errorMessage = "Key already exists in phone";
        } else if (mode != ImportMode.APPEND && match == null) {
            errorMessage = "Key does not exist in phone";
        } else if (match != null) {
            updateKey = match.key();
            matchedVersion = match.version();
        }
        // The worker marks VALID only after every validation step has passed.
        if (errorMessage != null) {
            transitionTo(PhoneDraftStatus.INVALID);
        }
    }

    private static String normalize(String key) {
        return key == null ? null : key.trim().toLowerCase(Locale.ROOT);
    }

    public Long getId() {
        return id;
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

    public void transitionTo(PhoneDraftStatus next) {
        status = status.transitionTo(next);
    }

    public PhoneDraftStatus getStatus() {
        return status;
    }

    // Used when constructing or restoring persisted state; business changes use transitionTo.
    public void setStatus(PhoneDraftStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getUpdateKey() {
        return updateKey;
    }

    public void setUpdateKey(String updateKey) {
        this.updateKey = updateKey;
    }

    public Long getMatchedVersion() {
        return matchedVersion;
    }

    public void setMatchedVersion(Long matchedVersion) {
        this.matchedVersion = matchedVersion;
    }
}
