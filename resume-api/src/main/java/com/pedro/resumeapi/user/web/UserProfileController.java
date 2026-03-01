package com.pedro.resumeapi.user.web;

import com.pedro.resumeapi.user.dto.UpdateUserProfileRequest;
import com.pedro.resumeapi.user.dto.UserProfileDTO;
import com.pedro.resumeapi.user.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/api/users/me")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    public UserProfileDTO me() {
        return userProfileService.getMyProfile();
    }

    @PatchMapping
    public UserProfileDTO update(@Valid @RequestBody UpdateUserProfileRequest request) {
        return userProfileService.updateMyProfile(request);
    }
}
