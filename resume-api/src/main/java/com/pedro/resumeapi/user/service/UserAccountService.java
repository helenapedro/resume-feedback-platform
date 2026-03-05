package com.pedro.resumeapi.user.service;

import com.pedro.resumeapi.comment.repository.CommentRepository;
import com.pedro.resumeapi.resume.service.ResumeService;
import com.pedro.resumeapi.security.CurrentUser;
import com.pedro.resumeapi.user.domain.User;
import com.pedro.resumeapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final CurrentUser currentUser;
    private final UserRepository userRepository;
    private final ResumeService resumeService;
    private final CommentRepository commentRepository;

    @Transactional
    public void deactivateMyAccount() {
        User user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        user.setEnabled(false);
        userRepository.save(user);
    }

    @Transactional
    public void deleteMyAccountPermanently() {
        User user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        // Remove all owned resumes and dependent data/files first.
        resumeService.listMyResumes().forEach(resume -> resumeService.deleteResume(resume.getId()));

        // Keep historical comment thread integrity on resumes not owned by this user.
        commentRepository.anonymizeByAuthorUserId(user.getId(), "Deleted user");

        userRepository.delete(user);
    }
}

