package com.pedro.resumeapi.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.storage.s3")
@Getter @Setter
public class S3StorageProperties {
    private String region;
    private String bucket;
    private String cloudfrontUrl;
    private Duration presignExpiration = Duration.ofMinutes(10);
}
