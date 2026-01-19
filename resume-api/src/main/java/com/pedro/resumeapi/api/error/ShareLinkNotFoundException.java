package com.pedro.resumeapi.api.error;

public class ShareLinkNotFoundException extends RuntimeException {
    public ShareLinkNotFoundException() {
        super("Share link not found");
    }
}
