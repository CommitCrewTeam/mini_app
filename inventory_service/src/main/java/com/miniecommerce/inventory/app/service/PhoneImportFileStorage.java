package com.miniecommerce.inventory.app.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PhoneImportFileStorage {
    private final Path directory;

    public PhoneImportFileStorage(@Value("${app.phone-import.directory}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    public Path allocate() throws IOException {
        Files.createDirectories(directory);
        return Files.createTempFile(directory, "phones-", ".csv");
    }
}
