package com.pedro.resumeworker.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage.s3")
@Getter
@Setter
public class S3StorageProperties {
    private String region;
}

