package com.pedro.resumeapi.storage;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;


@Service
public class LocalStorageService {

    private final Path baseDir;

    public LocalStorageService(@Value("${app.storage.local-dir}") String dir) {
        this.baseDir = Paths.get(dir).toAbsolutePath().normalize();
    }

    public String store(UUID ownerId, UUID resumeId, int versionNumber, MultipartFile file) throws IOException {
        Files.createDirectories(baseDir);

        String original = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "resume.pdf";
        String safeName = original.replaceAll("[^a-zA-Z0-9._-]", "_");

        Path dir = baseDir.resolve(ownerId.toString()).resolve(resumeId.toString());
        Files.createDirectories(dir);

        Path target = dir.resolve("v" + versionNumber + "_" + safeName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return target.toString(); // storageKey
    }

    public Resource loadAsResource(String storageKey) {
        try {
            Path path = Paths.get(storageKey).normalize().toAbsolutePath();
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalArgumentException("File not found: " + storageKey);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid storageKey: " + storageKey, e);
        }
    }
}
