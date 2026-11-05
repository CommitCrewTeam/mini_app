package com.miniecommerce.inventory.domain;

public enum PhoneDraftStatus {
    PENDING, VALID, INVALID, APPLIED;

    public boolean canTransitionTo(PhoneDraftStatus next) {
        if (next == null) return false;
        if (this == next) return true;
        return switch (this) {
            case PENDING -> next == VALID || next == INVALID;
            case VALID -> next == PENDING || next == INVALID || next == APPLIED;
            case INVALID -> next == PENDING;
            case APPLIED -> false;
        };
    }

    public PhoneDraftStatus transitionTo(PhoneDraftStatus next) {
        if (!canTransitionTo(next)) {
            throw new IllegalStateException("Cannot transition " + this + " to " + next);
        }
        return next;
    }
}
