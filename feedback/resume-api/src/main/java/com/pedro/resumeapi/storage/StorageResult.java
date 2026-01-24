package com.pedro.resumeapi.storage;

public record StorageResult(
        String storageKey,
        String s3Bucket,
        String s3ObjectKey,
        String s3VersionId
) {
}
