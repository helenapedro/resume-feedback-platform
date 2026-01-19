package com.pedro.resumeapi.api.error;

public class ShareLinkExpiredException extends RuntimeException {
    public ShareLinkExpiredException() {
        super("Share link has expired");
    }
}
