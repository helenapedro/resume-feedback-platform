package com.pedro.resumeapi.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.storage.s3")
public class S3StorageProperties {
    private String region;
    private String bucket;
    private String cloudfrontUrl;
    private Duration presignExpiration = Duration.ofMinutes(10);

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getCloudfrontUrl() {
        return cloudfrontUrl;
    }

    public void setCloudfrontUrl(String cloudfrontUrl) {
        this.cloudfrontUrl = cloudfrontUrl;
    }

    public Duration getPresignExpiration() {
        return presignExpiration;
    }

    public void setPresignExpiration(Duration presignExpiration) {
        this.presignExpiration = presignExpiration;
    }
}
