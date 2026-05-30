package com.pedro.resumeapi.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.demo.account")
public class DemoAccountProperties {

    private boolean loginEnabled = false;

    public boolean isLoginEnabled() {
        return loginEnabled;
    }

    public void setLoginEnabled(boolean loginEnabled) {
        this.loginEnabled = loginEnabled;
    }
}
