package com.pedro.resumeapi.resume.mapper;

import com.pedro.resumeapi.resume.domain.Resume;
import com.pedro.resumeapi.resume.domain.ResumeVersion;
import com.pedro.resumeapi.resume.dto.ResumeSummaryDTO;
import com.pedro.resumeapi.resume.dto.ResumeVersionDTO;
import org.springframework.stereotype.Component;

@Component
public class ResumeMapper {
    public ResumeSummaryDTO toSummaryDTO(Resume r) {
        return new ResumeSummaryDTO(
                r.getId(),
                r.getTitle(),
                r.getCurrentVersion() != null ? r.getCurrentVersion().getId() : null,
                r.getCreatedAt()
        );
    }

    public ResumeVersionDTO toVersionDTO(ResumeVersion v) {
        return new ResumeVersionDTO(
                v.getId(),
                v.getVersionNumber(),
                v.getOriginalFilename(),
                v.getContentType(),
                v.getFileSizeBytes(),
                v.getCreatedBy() != null ? v.getCreatedBy().getId() : null,
                v.getCreatedAt()
        );
    }
}
