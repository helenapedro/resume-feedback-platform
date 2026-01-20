package com.pedro.resumeapi.resume.repository;

import com.pedro.resumeapi.resume.domain.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {
    List<Resume> findByOwner_IdOrderByCreatedAtDesc(UUID ownerId);

}