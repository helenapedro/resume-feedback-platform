package com.pedro.resumeapi.api.error;

public class ShareLinkMaxUsesReachedException extends RuntimeException {
    public ShareLinkMaxUsesReachedException() {
        super("Share link maximum usage limit reached");
    }
}
