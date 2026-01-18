package com.pedro.resumeapi.security;

import com.pedro.resumeapi.api.error.UnauthorizedException;
import com.pedro.resumeapi.domain.User;
import com.pedro.resumeapi.repository.UserRepository;
import com.pedro.resumeapi.security.dto.*;
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

        String token = jwtService.generate(user.getId(),
                user.getEmail(),
                user.getRole().name())
                ;
        return new AuthResponse(token);
    }
}
