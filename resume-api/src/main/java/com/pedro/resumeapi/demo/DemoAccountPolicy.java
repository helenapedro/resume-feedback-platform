package com.pedro.resumeapi.demo;

import com.pedro.resumeapi.api.error.ForbiddenException;
import com.pedro.resumeapi.user.domain.User;
import org.springframework.stereotype.Service;

@Service
public class DemoAccountPolicy {

    private final DemoSeedProperties properties;
    private final DemoAccountProperties accountProperties;

    public DemoAccountPolicy(DemoSeedProperties properties, DemoAccountProperties accountProperties) {
        this.properties = properties;
        this.accountProperties = accountProperties;
    }

    public boolean isDemoUser(User user) {
        return user != null
                && user.getEmail() != null
                && user.getEmail().equalsIgnoreCase(properties.getEmail());
    }

    public boolean isDemoEmail(String email) {
        return email != null && email.equalsIgnoreCase(properties.getEmail());
    }

    public void requireMutableAccount(User user) {
        if (isDemoUser(user)) {
            throw new ForbiddenException("Demo account is read-only. Create your own account to make changes.");
        }
    }

    public void requireLoginAllowed(String email) {
        if (!accountProperties.isLoginEnabled() && isDemoEmail(email)) {
            throw new ForbiddenException("Shared demo account login is disabled. Create your own account to continue.");
        }
    }
}
