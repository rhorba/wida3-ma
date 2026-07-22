package com.wida3.files.service;

import com.wida3.files.exception.FileTooLargeException;
import com.wida3.files.exception.UnsupportedFileTypeException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    private final Path storageRoot;
    private final long maxSizeBytes;
    private final Set<String> allowedContentTypes;

    public FileStorageService(
            @Value("${app.file-storage.path}") String storagePath,
            @Value("${app.file-storage.max-size-mb}") long maxSizeMb,
            @Value("${app.file-storage.allowed-content-types}") String allowedContentTypesCsv) {
        this.storageRoot = Path.of(storagePath);
        this.maxSizeBytes = maxSizeMb * 1024 * 1024;
        this.allowedContentTypes = Set.copyOf(Arrays.asList(allowedContentTypesCsv.split(",")));
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create file storage directory: " + storageRoot, e);
        }
    }

    /** @return the public URL path (e.g. "/uploads/xxxx.jpg") the stored file is served at. */
    public String store(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !allowedContentTypes.contains(contentType)) {
            throw new UnsupportedFileTypeException(contentType);
        }
        if (file.getSize() > maxSizeBytes) {
            throw new FileTooLargeException(maxSizeBytes / (1024 * 1024));
        }

        String extension = EXTENSION_BY_CONTENT_TYPE.get(contentType);
        String filename = UUID.randomUUID() + extension;
        Path target = storageRoot.resolve(filename);

        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store uploaded file", e);
        }

        return "/uploads/" + filename;
    }
}
