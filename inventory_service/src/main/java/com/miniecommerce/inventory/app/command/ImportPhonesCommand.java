package com.miniecommerce.inventory.app.command;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import com.miniecommerce.common.hexagonal.adapter.Pojo.StagedFile;
import com.miniecommerce.inventory.domain.enums.ImportMode;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Neutral upload data handed from the web adapter to the import use case.
 * The file is a random temporary path; the client-supplied name is kept for display only.
 */
public record ImportPhonesCommand(String originalFilename, ImportMode mode, long size, Path file) {

    public static final long MAX_SIZE = 20 * 1024 * 1024;

    /** Entry point cho controller: from(f, mode) */
    public static ImportPhonesCommand from(StagedFile f, String mode) {
        return create(f.filename(), mode, f.size(), f.path());
    }

    /** Validates the web values and builds the command. */
    public static ImportPhonesCommand create(String originalFilename, String rawMode, long size, Path file) {
        if (originalFilename == null || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Upload a .csv file");
        }
        ImportMode mode;
        try {
            mode = ImportMode.valueOf(rawMode);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Mode must be CREATE, UPDATE or RESTOCK");
        }
        return new ImportPhonesCommand(originalFilename, mode, size, file);
    }

    public ImportPhonesCommand {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "filename required");
        }
        if (mode == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "mode required");
        }
        if (size <= 0 || size > MAX_SIZE) {
            throw new AppException(ErrorCode.BAD_REQUEST, "file size must be 1.." + MAX_SIZE + " bytes");
        }
        if (file == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "uploaded file required");
        }
    }
}
