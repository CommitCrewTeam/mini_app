package com.miniecommerce.inventory.app.command;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;

public record ApprovePhoneImportCommand(Long importId, boolean warningSkip) {

    public ApprovePhoneImportCommand {
        if (importId == null || importId < 1) {
            throw new AppException(ErrorCode.BAD_REQUEST, "importId must be positive");
        }
    }
}
