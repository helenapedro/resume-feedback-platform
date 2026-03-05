package com.pedro.resumeapi.security;

import com.pedro.resumeapi.api.error.UnauthorizedException;
import com.pedro.resumeapi.user.domain.User;
import com.pedro.resumeapi.auth.dto.AuthResponse;
import com.pedro.resumeapi.auth.dto.GoogleAuthRequest;
import com.pedro.resumeapi.auth.dto.LoginRequest;
import com.pedro.resumeapi.auth.dto.ReactivateRequest;
import com.pedro.resumeapi.auth.dto.RegisterRequest;
import com.pedro.resumeapi.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifierService googleTokenVerifierService;

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest req) {
        if (req.email() == null || req.email().isBlank()) throw new IllegalArgumentException("email required");
        if (req.password() == null || req.password().isBlank()) throw new IllegalArgumentException("password required");

        userRepository.findByEmail(req.email()).ifPresent(u -> {
            throw new IllegalArgumentException("email already exists");
        });

        var user = new User();
        user.setEmail(req.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(User.Role.USER);
        user.setEnabled(true);
        user.setCreatedAt(Instant.now());
        user.setLastLoginAt(Instant.now());

        userRepository.save(user);

        String token = jwtService.generate(user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(token);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest req) {
        var user = userRepository.findByEmail(req.email().trim().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("invalid credentials"));

        if (!user.isEnabled()) throw new IllegalArgumentException("user disabled");

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("invalid credentials");
        }
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String token = jwtService.generate(user.getId(),
                user.getEmail(),
                user.getRole().name())
                ;
        return new AuthResponse(token);
    }

    @PostMapping("/google")
    public AuthResponse google(@RequestBody GoogleAuthRequest req) {
        var identity = googleTokenVerifierService.verifyIdToken(req.idToken());

        User user = userRepository.findByEmail(identity.email()).orElseGet(() -> {
            User created = new User();
            created.setEmail(identity.email());
            // local password auth is not used for Google-only sign-in users
            created.setPasswordHash(passwordEncoder.encode("google-oauth-" + identity.sub()));
            created.setRole(User.Role.USER);
            created.setEnabled(true);
            created.setCreatedAt(Instant.now());
            created.setFullName(identity.name());
            created.setAvatarUrl(identity.pictureUrl());
            return created;
        });

        if (!user.isEnabled()) {
            user.setEnabled(true);
        }

        if ((user.getFullName() == null || user.getFullName().isBlank()) && identity.name() != null) {
            user.setFullName(identity.name());
        }
        if ((user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) && identity.pictureUrl() != null) {
            user.setAvatarUrl(identity.pictureUrl());
        }
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String token = jwtService.generate(user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(token);
    }

    @PostMapping("/reactivate")
    public AuthResponse reactivate(@RequestBody ReactivateRequest req) {
        if (req.email() == null || req.email().isBlank()) throw new IllegalArgumentException("email required");
        if (req.password() == null || req.password().isBlank()) throw new IllegalArgumentException("password required");

        var user = userRepository.findByEmail(req.email().trim().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("invalid credentials");
        }

        user.setEnabled(true);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String token = jwtService.generate(user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(token);
    }
}
