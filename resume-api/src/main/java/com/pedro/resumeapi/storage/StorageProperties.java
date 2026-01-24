package com.pedro.resumeapi.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {
    private StorageBackend backend = StorageBackend.LOCAL;

    public StorageBackend getBackend() {
        return backend;
    }

    public void setBackend(StorageBackend backend) {
        this.backend = backend;
    }
}
