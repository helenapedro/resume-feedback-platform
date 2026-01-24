package com.pedro.resumeapi.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.storage")
@Getter @Setter
public class StorageProperties {
    private StorageBackend backend = StorageBackend.LOCAL;
}
