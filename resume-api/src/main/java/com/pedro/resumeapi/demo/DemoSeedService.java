package com.pedro.resumeapi.demo;

import com.pedro.common.ai.Language;
import com.pedro.common.ai.mongo.AiFeedbackDocument;
import com.pedro.common.ai.mongo.AiProgressDocument;
import com.pedro.resumeapi.ai.domain.AiFeedbackRef;
import com.pedro.resumeapi.ai.domain.AiJob;
import com.pedro.resumeapi.ai.domain.AiProgressRef;
import com.pedro.resumeapi.ai.mongo.AiFeedbackMongoRepository;
import com.pedro.resumeapi.ai.mongo.AiProgressMongoRepository;
import com.pedro.resumeapi.ai.repository.AiFeedbackRefRepository;
import com.pedro.resumeapi.ai.repository.AiJobRepository;
import com.pedro.resumeapi.ai.repository.AiProgressRefRepository;
import com.pedro.resumeapi.resume.domain.Resume;
import com.pedro.resumeapi.resume.domain.ResumeVersion;
import com.pedro.resumeapi.resume.repository.ResumeRepository;
import com.pedro.resumeapi.resume.repository.ResumeVersionRepository;
import com.pedro.resumeapi.user.domain.User;
import com.pedro.resumeapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo.seed.enabled", havingValue = "true")
public class DemoSeedService {

    private static final UUID USER_ID = UUID.fromString("611771e5-59bc-4d32-9e8d-a0c04d2b3b2b");
    private static final UUID RESUME_ID = UUID.fromString("ef86f963-f07a-4b6f-9427-86fa45496e1d");
    private static final UUID VERSION_ONE_ID = UUID.fromString("f74e53b6-88b1-47c4-8e37-0412f0d87546");
    private static final UUID VERSION_TWO_ID = UUID.fromString("093fe2de-a754-45af-a6aa-18d04f67d60f");
    private static final UUID VERSION_THREE_ID = UUID.fromString("c701624c-de83-4784-a3be-b5c3a8bd9690");
    private static final UUID JOB_ONE_ID = UUID.fromString("5c6ff8eb-c6de-48a7-88e3-cbdf8c525746");
    private static final UUID JOB_TWO_ID = UUID.fromString("ddaa6b30-67fc-48f8-9a73-2c3c16b8b6f6");
    private static final UUID JOB_THREE_ID = UUID.fromString("99e7b9fa-f445-42bc-af52-6cbff1c8a237");
    private static final UUID FEEDBACK_REF_ONE_ID = UUID.fromString("f96982b4-134a-4144-9738-7460d5549594");
    private static final UUID FEEDBACK_REF_TWO_ID = UUID.fromString("00dff0da-3590-4f7a-8299-4c829bb64fbf");
    private static final UUID FEEDBACK_REF_THREE_ID = UUID.fromString("f7d9f7d8-7c9e-4d6f-8a15-0f9d4a5b8d31");
    private static final UUID PROGRESS_REF_TWO_ID = UUID.fromString("e4b219e6-e54f-4db8-b71f-0d3fc2734092");
    private static final UUID PROGRESS_REF_THREE_ID = UUID.fromString("7b0f9c1a-2e5b-4f9d-b7f2-8d34f4b8f913");

    private static final String MODEL = "seeded-demo";
    private static final String PROMPT_VERSION = "demo-v1";
    private static final String FEEDBACK_DOC_ONE_ID = "seed-feedback-v1";
    private static final String FEEDBACK_DOC_TWO_ID = "seed-feedback-v2";
    private static final String FEEDBACK_DOC_THREE_ID = "seed-feedback-v3";
    private static final String PROGRESS_DOC_TWO_ID = "seed-progress-v2";
    private static final String PROGRESS_DOC_THREE_ID = "seed-progress-v3";

    private final DemoSeedProperties properties;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final AiJobRepository aiJobRepository;
    private final AiFeedbackRefRepository aiFeedbackRefRepository;
    private final AiProgressRefRepository aiProgressRefRepository;
    private final AiFeedbackMongoRepository aiFeedbackMongoRepository;
    private final AiProgressMongoRepository aiProgressMongoRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedOnStartup() {
        DemoSeedIds ids = seedDemoData();
        log.info("Seeded demo resume data for user={} resume={}", ids.userId(), ids.resumeId());
    }

    @Transactional
    public DemoSeedIds seedDemoData() {
        Instant createdAt = Instant.parse("2026-05-25T17:30:00Z");
        Instant versionTwoCreatedAt = Instant.parse("2026-05-25T17:37:00Z");
        Instant versionThreeCreatedAt = Instant.parse("2026-05-29T23:33:59Z");

        User user = seedUser(createdAt);
        Resume resume = seedResume(user, createdAt);
        ResumeVersion versionOne = seedVersion(
                VERSION_ONE_ID,
                resume,
                user,
                1,
                "pdf1_v1.pdf",
                78503L,
                localPath(properties.getVersionOneLocalPath()),
                properties.getVersionOneS3ObjectKey(),
                properties.getVersionOneS3VersionId(),
                createdAt
        );
        ResumeVersion versionTwo = seedVersion(
                VERSION_TWO_ID,
                resume,
                user,
                2,
                "pdf1_v2.pdf",
                86247L,
                localPath(properties.getVersionTwoLocalPath()),
                properties.getVersionTwoS3ObjectKey(),
                properties.getVersionTwoS3VersionId(),
                versionTwoCreatedAt
        );
        ResumeVersion versionThree = seedVersion(
                VERSION_THREE_ID,
                resume,
                user,
                3,
                "pdf1_v3.pdf",
                110797L,
                localPath(properties.getVersionThreeLocalPath()),
                properties.getVersionThreeS3ObjectKey(),
                properties.getVersionThreeS3VersionId(),
                versionThreeCreatedAt
        );

        resume.setCurrentVersion(versionThree);
        resumeRepository.save(resume);

        seedDoneJob(JOB_ONE_ID, versionOne, "demo:" + VERSION_ONE_ID, createdAt);
        seedDoneJob(JOB_TWO_ID, versionTwo, "demo:" + VERSION_TWO_ID, versionTwoCreatedAt);
        seedDoneJob(JOB_THREE_ID, versionThree, "demo:" + VERSION_THREE_ID, versionThreeCreatedAt);

        seedFeedback(versionOne, user.getId(), JOB_ONE_ID, FEEDBACK_DOC_ONE_ID, FEEDBACK_REF_ONE_ID, createdAt, versionOneSummary(),
                List.of(
                        "The resume has a clear career direction and shows ownership across product delivery, stakeholder alignment, and operational execution.",
                        "Several bullets name meaningful business areas, including onboarding, analytics, and cross-functional launch work.",
                        "The structure is readable enough for a reviewer to understand the candidate's product background quickly."
                ),
                List.of(
                        "Most impact statements are activity-based. Add concrete metrics for adoption, cycle-time reduction, revenue, retention, or user engagement.",
                        "The top summary is broad and does not yet position the candidate for a specific product role or seniority level.",
                        "Some bullets describe collaboration without naming the candidate's decision, tradeoff, or measurable outcome.",
                        "Skills and tools are present, but they are not tied to evidence in the experience section."
                )
        );

        seedFeedback(versionTwo, user.getId(), JOB_TWO_ID, FEEDBACK_DOC_TWO_ID, FEEDBACK_REF_TWO_ID, versionTwoCreatedAt, versionTwoSummary(),
                List.of(
                        "Version 2 turns the strongest product work into evidence by adding metrics for activation lift, delivery speed, and stakeholder adoption.",
                        "The revised summary is sharper and now frames the candidate as a product manager who connects discovery, analytics, and execution.",
                        "The experience section now shows a clearer before-and-after story, especially around onboarding and reporting workflows."
                ),
                List.of(
                        "The resume still needs a tighter prioritization story. Add one bullet that explains how competing roadmap options were evaluated.",
                        "A few bullets now include strong metrics but could name the customer or business segment affected by the work.",
                        "The skills section should be trimmed to the tools that are visibly supported by the work history."
                )
        );

        seedFeedback(versionThree, user.getId(), JOB_THREE_ID, FEEDBACK_DOC_THREE_ID, FEEDBACK_REF_THREE_ID, versionThreeCreatedAt, versionThreeSummary(),
                List.of(
                        "Version 3 presents a stronger software engineering narrative with clearer backend, API, and database evidence.",
                        "The resume is easier to scan because the strongest technical contributions are now closer to the top.",
                        "The candidate's project work is more credible because it connects implementation details to user-facing outcomes."
                ),
                List.of(
                        "Some impact claims still need tighter metrics, such as latency, usage, revenue, or operational time saved.",
                        "The AI-related project should clarify scope, model/provider choices, and production constraints.",
                        "The skills section should stay aligned with technologies shown directly in the experience and project bullets."
                )
        );

        seedProgress(versionOne, versionTwo, user.getId(), JOB_TWO_ID, PROGRESS_DOC_TWO_ID, PROGRESS_REF_TWO_ID, versionTwoCreatedAt);
        seedProgress(versionTwo, versionThree, user.getId(), JOB_THREE_ID, PROGRESS_DOC_THREE_ID, PROGRESS_REF_THREE_ID, versionThreeCreatedAt);

        return new DemoSeedIds(user.getId(), RESUME_ID, VERSION_ONE_ID, VERSION_TWO_ID, VERSION_THREE_ID);
    }

    private User seedUser(Instant createdAt) {
        User user = userRepository.findByEmail(properties.getEmail().trim().toLowerCase())
                .orElseGet(User::new);
        user.setId(user.getId() == null ? USER_ID : user.getId());
        user.setEmail(properties.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(properties.getPassword()));
        user.setRole(User.Role.USER);
        user.setEnabled(true);
        user.setCreatedAt(user.getCreatedAt() == null ? createdAt : user.getCreatedAt());
        user.setFullName("Demo Guest");
        user.setBio("Seeded demo account for the version-aware resume improvement workflow.");
        return userRepository.save(user);
    }

    private Resume seedResume(User user, Instant createdAt) {
        Resume resume = resumeRepository.findById(RESUME_ID).orElseGet(Resume::new);
        resume.setId(RESUME_ID);
        resume.setOwner(user);
        resume.setTitle(properties.getResumeTitle());
        resume.setCreatedAt(createdAt);
        return resumeRepository.save(resume);
    }

    private ResumeVersion seedVersion(UUID id,
                                      Resume resume,
                                      User user,
                                      int versionNumber,
                                      String filename,
                                      long fileSizeBytes,
                                      String storageKey,
                                      String s3ObjectKey,
                                      String s3VersionId,
                                      Instant createdAt) {
        ResumeVersion version = resumeVersionRepository.findById(id).orElseGet(ResumeVersion::new);
        version.setId(id);
        version.setResume(resume);
        version.setCreatedBy(user);
        version.setVersionNumber(versionNumber);
        version.setOriginalFilename(filename);
        version.setFileName(filename);
        version.setContentType("application/pdf");
        version.setFileSizeBytes(fileSizeBytes);
        version.setStorageKey(storageKey);
        version.setS3Bucket(properties.getS3Bucket());
        version.setS3ObjectKey(s3ObjectKey);
        version.setS3VersionId(blankToNull(s3VersionId));
        version.setChecksumSha256(null);
        version.setCreatedAt(createdAt);
        return resumeVersionRepository.save(version);
    }

    private void seedDoneJob(UUID id, ResumeVersion version, String idempotencyKey, Instant createdAt) {
        AiJob job = aiJobRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(AiJob::new);
        job.setId(job.getId() == null ? id : job.getId());
        job.setResumeVersion(version);
        job.setStatus(AiJob.Status.DONE);
        job.setAttemptCount(1);
        job.setIdempotencyKey(idempotencyKey);
        job.setLanguage(Language.EN);
        job.setCreatedAt(createdAt);
        job.setStartedAt(createdAt.plusSeconds(5));
        job.setFinishedAt(createdAt.plusSeconds(20));
        job.setErrorCode(null);
        job.setErrorDetail(null);
        job.setNextRetryAt(null);
        aiJobRepository.save(job);
    }

    private void seedFeedback(ResumeVersion version,
                              UUID ownerId,
                              UUID jobId,
                              String mongoDocId,
                              UUID refId,
                              Instant createdAt,
                              String summary,
                              List<String> strengths,
                              List<String> improvements) {
        AiFeedbackDocument document = aiFeedbackMongoRepository.findById(mongoDocId).orElseGet(AiFeedbackDocument::new);
        document.setId(mongoDocId);
        document.setJobId(jobId);
        document.setResumeId(RESUME_ID);
        document.setResumeVersionId(version.getId());
        document.setOwnerId(ownerId);
        document.setCreatedAt(createdAt);
        document.setModel(MODEL);
        document.setPromptVersion(PROMPT_VERSION);
        document.setSummary(summary);
        document.setStrengths(strengths);
        document.setImprovements(improvements);
        aiFeedbackMongoRepository.save(document);

        AiFeedbackRef ref = aiFeedbackRefRepository
                .findByResumeVersion_IdAndFeedbackVersion(version.getId(), 1)
                .orElseGet(AiFeedbackRef::new);
        ref.setId(ref.getId() == null ? refId : ref.getId());
        ref.setResumeVersion(version);
        ref.setFeedbackVersion(1);
        ref.setMongoDocId(mongoDocId);
        ref.setModel(MODEL);
        ref.setPromptVersion(PROMPT_VERSION);
        ref.setCreatedAt(createdAt);
        aiFeedbackRefRepository.save(ref);
    }

    private void seedProgress(ResumeVersion versionOne,
                              ResumeVersion versionTwo,
                              UUID ownerId,
                              UUID jobId,
                              String mongoDocId,
                              UUID refId,
                              Instant createdAt) {
        AiProgressDocument document = aiProgressMongoRepository.findById(mongoDocId).orElseGet(AiProgressDocument::new);
        document.setId(mongoDocId);
        document.setJobId(jobId);
        document.setResumeId(RESUME_ID);
        document.setResumeVersionId(versionTwo.getId());
        document.setBaselineResumeVersionId(versionOne.getId());
        document.setOwnerId(ownerId);
        document.setCreatedAt(createdAt);
        document.setModel(MODEL);
        document.setPromptVersion(PROMPT_VERSION);
        int baseline = versionOne.getVersionNumber();
        int target = versionTwo.getVersionNumber();
        document.setSummary("Version " + target + " improves on Version " + baseline + " by making the resume more evidence-based and easier to evaluate. The remaining work is mostly about sharper metrics, clearer prioritization, and tighter technical positioning.");
        document.setProgressStatus("IMPROVED");
        document.setProgressScore(76);
        document.setImprovedAreas(List.of(
                "Resolved: Version " + baseline + " had more activity-based bullets; Version " + target + " adds stronger outcome and scope signals.",
                "Resolved: The summary is more targeted and gives recruiters a clearer role fit.",
                "Improved: The strongest projects are easier to scan because the revised bullets lead with result, scope, and tool context."
        ));
        document.setUnchangedIssues(List.of(
                "Still needs work: The resume should include one prioritization example that names the options considered and the decision criteria used.",
                "Still needs work: Some metrics need clearer business context, such as user segment, team size, or revenue/customer impact.",
                "Still needs work: The skills section remains broader than the evidence shown in the experience section."
        ));
        document.setNewIssues(List.of(
                "Regression: Version " + target + " adds more detail, but a few bullets may need trimming for faster recruiter scanning."
        ));
        aiProgressMongoRepository.save(document);

        AiProgressRef ref = aiProgressRefRepository
                .findByResumeVersion_IdAndProgressVersion(versionTwo.getId(), 1)
                .orElseGet(AiProgressRef::new);
        ref.setId(ref.getId() == null ? refId : ref.getId());
        ref.setResumeVersion(versionTwo);
        ref.setBaselineResumeVersion(versionOne);
        ref.setProgressVersion(1);
        ref.setMongoDocId(mongoDocId);
        ref.setModel(MODEL);
        ref.setPromptVersion(PROMPT_VERSION);
        ref.setCreatedAt(createdAt);
        aiProgressRefRepository.save(ref);
    }

    private String versionOneSummary() {
        return "Version 1 is a solid initial draft, but it reads more like a responsibility list than an evidence-based product resume. The clearest conversion opportunity is to show measurable impact across the strongest projects.";
    }

    private String versionTwoSummary() {
        return "Version 2 is materially stronger because it converts several generic delivery bullets into outcome-based evidence. It now demonstrates an iteration loop: the candidate responded to feedback, added metrics, and made the product story easier to trust.";
    }

    private String versionThreeSummary() {
        return "Version 3 is the strongest demo version because it shifts the resume toward a clearer software engineering story. It gives recruiters more technical evidence while preserving the before-and-after improvement arc from earlier versions.";
    }

    private String localPath(String path) {
        return Path.of(path).toAbsolutePath().normalize().toString();
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    public record DemoSeedIds(UUID userId, UUID resumeId, UUID versionOneId, UUID versionTwoId, UUID versionThreeId) {
    }
}
