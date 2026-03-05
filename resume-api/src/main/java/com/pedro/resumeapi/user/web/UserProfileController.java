package com.pedro.resumeapi.user.web;

import com.pedro.resumeapi.user.dto.UpdateUserProfileRequest;
import com.pedro.resumeapi.user.dto.UserProfileDTO;
import com.pedro.resumeapi.user.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import com.pedro.resumeapi.user.service.UserAccountService;

@RestController
@AllArgsConstructor
@RequestMapping("/api/users/me")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final UserAccountService userAccountService;

    @GetMapping
    public UserProfileDTO me() {
        return userProfileService.getMyProfile();
    }

    @PatchMapping
    public UserProfileDTO update(@Valid @RequestBody UpdateUserProfileRequest request) {
        return userProfileService.updateMyProfile(request);
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserProfileDTO uploadAvatar(@RequestPart("file") MultipartFile file) throws IOException {
        return userProfileService.uploadMyAvatar(file);
    }

    @PostMapping("/deactivate")
    public ResponseEntity<Void> deactivateMyAccount() {
        userAccountService.deactivateMyAccount();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteMyAccountPermanently() {
        userAccountService.deleteMyAccountPermanently();
        return ResponseEntity.noContent().build();
    }
}
