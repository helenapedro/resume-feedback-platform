package com.pedro.resumeapi.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.demo.seed")
public class DemoSeedProperties {

    private boolean enabled = false;
    private String email = "demo@hmpedro.com";
    private String password = "demo-password-change-me";
    private String resumeTitle = "Demo Resume - Product Manager";
    private String s3Bucket = "resume-feedback-platform";
    private String versionOneS3ObjectKey = "seed/pdf1_v1.pdf";
    private String versionTwoS3ObjectKey = "seed/pdf1_v1.pdf";
    private String versionOneS3VersionId;
    private String versionTwoS3VersionId;
    private String versionOneLocalPath = "docs/seed/pdf1_v1.pdf";
    private String versionTwoLocalPath = "docs/seed/pdf1_v2.pdf";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getResumeTitle() {
        return resumeTitle;
    }

    public void setResumeTitle(String resumeTitle) {
        this.resumeTitle = resumeTitle;
    }

    public String getS3Bucket() {
        return s3Bucket;
    }

    public void setS3Bucket(String s3Bucket) {
        this.s3Bucket = s3Bucket;
    }

    public String getVersionOneS3ObjectKey() {
        return versionOneS3ObjectKey;
    }

    public void setVersionOneS3ObjectKey(String versionOneS3ObjectKey) {
        this.versionOneS3ObjectKey = versionOneS3ObjectKey;
    }

    public String getVersionTwoS3ObjectKey() {
        return versionTwoS3ObjectKey;
    }

    public void setVersionTwoS3ObjectKey(String versionTwoS3ObjectKey) {
        this.versionTwoS3ObjectKey = versionTwoS3ObjectKey;
    }

    public String getVersionOneS3VersionId() {
        return versionOneS3VersionId;
    }

    public void setVersionOneS3VersionId(String versionOneS3VersionId) {
        this.versionOneS3VersionId = versionOneS3VersionId;
    }

    public String getVersionTwoS3VersionId() {
        return versionTwoS3VersionId;
    }

    public void setVersionTwoS3VersionId(String versionTwoS3VersionId) {
        this.versionTwoS3VersionId = versionTwoS3VersionId;
    }

    public String getVersionOneLocalPath() {
        return versionOneLocalPath;
    }

    public void setVersionOneLocalPath(String versionOneLocalPath) {
        this.versionOneLocalPath = versionOneLocalPath;
    }

    public String getVersionTwoLocalPath() {
        return versionTwoLocalPath;
    }

    public void setVersionTwoLocalPath(String versionTwoLocalPath) {
        this.versionTwoLocalPath = versionTwoLocalPath;
    }
}
