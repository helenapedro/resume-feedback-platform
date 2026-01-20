package com.pedro.resumeapi;

import com.pedro.resumeapi.user.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    private final UserRepository userRepository;

    public PingController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/api/ping")
    public String ping() {
        long count = userRepository.count();
        return "pong (users=" + count + ")";
    }
}
