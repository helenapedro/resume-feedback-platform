package com.pedro.resumeapi.demo;

import com.pedro.resumeapi.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/demo")
@ConditionalOnProperty(name = "app.demo.session.enabled", havingValue = "true")
public class DemoSessionController {

    private final DemoSeedService demoSeedService;
    private final DemoSeedProperties properties;
    private final JwtService jwtService;

    @PostMapping("/session")
    public DemoSessionResponse createSession() {
        DemoSeedService.DemoSeedIds ids = demoSeedService.seedDemoData();
        String token = jwtService.generate(ids.userId(), properties.getEmail(), "USER");

        return new DemoSessionResponse(
                token,
                properties.getEmail(),
                ids.userId(),
                ids.resumeId(),
                ids.versionTwoId(),
                ids.versionOneId()
        );
    }
}
