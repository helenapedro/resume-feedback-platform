package com.pedro.resumeapi.demo;

import com.pedro.resumeapi.user.domain.User;
import org.springframework.stereotype.Service;

@Service
public class DemoAccountPolicy {

    private final DemoSeedProperties properties;

    public DemoAccountPolicy(DemoSeedProperties properties) {
        this.properties = properties;
    }

    public boolean isDemoUser(User user) {
        return user != null
                && user.getEmail() != null
                && user.getEmail().equalsIgnoreCase(properties.getEmail());
    }
}
