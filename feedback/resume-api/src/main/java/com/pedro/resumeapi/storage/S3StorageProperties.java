package com.pedro.resumeapi.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.storage.s3")
public class S3StorageProperties {
    private String region;
    private Duration presignExpiration = Duration.ofMinutes(10);

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Duration getPresignExpiration() {
        return presignExpiration;
    }

    public void setPresignExpiration(Duration presignExpiration) {
        this.presignExpiration = presignExpiration;
    }
}
