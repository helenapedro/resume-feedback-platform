package com.pedro.resumeapi.api.error;

public class ShareLinkRevokedException extends RuntimeException {
    public ShareLinkRevokedException() {
        super("Share link has been revoked");
    }
}
