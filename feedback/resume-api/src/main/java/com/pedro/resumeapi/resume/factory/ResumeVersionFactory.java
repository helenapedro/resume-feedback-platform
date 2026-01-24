package com.pedro.resumeapi.resume.factory;

import com.pedro.resumeapi.resume.domain.Resume;
import com.pedro.resumeapi.resume.domain.ResumeVersion;
import com.pedro.resumeapi.user.domain.User;
import com.pedro.resumeapi.storage.ResumeStorageWriter;
import com.pedro.resumeapi.storage.StorageResult;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@AllArgsConstructor
public class ResumeVersionFactory {

    private final ResumeStorageWriter storageWriter;

    public ResumeVersion create(Resume resume, int version, MultipartFile file, User owner) throws IOException {
        String original = defaultIfBlank(file.getOriginalFilename(), "resume.pdf");
        String contentType = defaultIfBlank(file.getContentType(), "application/pdf");

        ResumeVersion rv = new ResumeVersion();
        rv.setResume(resume);
        rv.setVersionNumber(version);
        rv.setOriginalFilename(original);
        rv.setFileName(original);
        rv.setContentType(contentType);
        rv.setFileSizeBytes(file.getSize());
        rv.setCreatedBy(owner);
        StorageResult storage = storageWriter.store(owner.getId(), resume.getId(), version, file, original, contentType);
        rv.setStorageKey(storage.storageKey());
        rv.setS3Bucket(storage.s3Bucket());
        rv.setS3ObjectKey(storage.s3ObjectKey());
        rv.setS3VersionId(storage.s3VersionId());
        return rv;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
