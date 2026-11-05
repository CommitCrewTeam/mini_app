package com.miniecommerce.inventory.domain;

public enum PhoneImportStatus {
    /** Đã tạo lượt import; đang chờ hoặc đang đọc CSV và validate lần đầu. */
    PENDING,

    /** Sẵn sàng validate lại hoặc approve; các draft vẫn có thể có lỗi validation. */
    READY,

    /** Đang chạy yêu cầu validate lại các draft, chưa ghi vào bảng phone. */
    VALIDATING,

    /** Đang validate lại và ghi các draft hợp lệ vào bảng phone khi approve. */
    APPROVING,

    /** Approve hoàn tất và có draft đã được áp dụng; trạng thái kết thúc. */
    DONE,

    /** Job thất bại; khác với lỗi validation của từng draft. Trạng thái kết thúc. */
    FAILED;

    public boolean canTransitionTo(PhoneImportStatus next) {
        if (next == null) return false;
        if (this == next) return true;
        return switch (this) {
            case PENDING -> next == READY || next == FAILED;
            case READY -> next == VALIDATING || next == APPROVING;
            case VALIDATING -> next == READY || next == FAILED;
            case APPROVING -> next == READY || next == DONE || next == FAILED;
            case DONE, FAILED -> false;
        };
    }

    public PhoneImportStatus transitionTo(PhoneImportStatus next) {
        if (!canTransitionTo(next)) {
            throw new IllegalStateException("Cannot transition " + this + " to " + next);
        }
        return next;
    }
}
