package com.pedro.resumeapi.user.service;

import com.pedro.resumeapi.security.CurrentUser;
import com.pedro.resumeapi.user.domain.User;
import com.pedro.resumeapi.user.dto.UpdateUserProfileRequest;
import com.pedro.resumeapi.user.dto.UserProfileDTO;
import com.pedro.resumeapi.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public UserProfileDTO getMyProfile() {
        User user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        return toProfile(user);
    }

    @Transactional
    public UserProfileDTO updateMyProfile(UpdateUserProfileRequest request) {
        User user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        user.setFullName(normalize(request.fullName()));
        user.setPhone(normalize(request.phone()));
        user.setBio(normalize(request.bio()));
        user.setAvatarUrl(normalize(request.avatarUrl()));

        userRepository.save(user);
        return toProfile(user);
    }

    private UserProfileDTO toProfile(User user) {
        return new UserProfileDTO(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getLastLoginAt(),
                user.getFullName(),
                user.getPhone(),
                user.getBio(),
                user.getAvatarUrl()
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
