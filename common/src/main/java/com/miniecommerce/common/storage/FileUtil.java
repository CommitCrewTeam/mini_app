package com.miniecommerce.common.storage;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;

/** Single place for filesystem work shared by adapters, services and jobs. */
@Configuration
@ConditionalOnMissingBean(FileUtil.class)
public class FileUtil {

    private final Path directory;

    public FileUtil(
            @Value("${app.temp-file.directory:${app.phone-import.directory:${java.io.tmpdir}/temp-uploads}}")
            String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    public Path allocate(String prefix, String suffix) {
        try {
            Files.createDirectories(directory);
            return Files.createTempFile(directory, prefix, suffix);
        } catch (IOException ex) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Cannot allocate temporary file", ex);
        }
    }

    public long size(Path path) {
        try {
            return Files.size(path);
        } catch (IOException ex) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Cannot read file size", ex);
        }
    }

    /** Best-effort cleanup: callers never handle filesystem errors. */
    public void deleteQuietly(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException | RuntimeException ignored) {
            // The periodic cleanup job removes orphaned files.
        }
    }

    public void deleteQuietly(String path) {
        if (path == null) return;
        try {
            deleteQuietly(Path.of(path));
        } catch (InvalidPathException ignored) {
            // Nothing valid to delete.
        }
    }
}
