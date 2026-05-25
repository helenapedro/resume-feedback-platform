package com.pedro.resumeapi.demo;

import java.util.UUID;

public record DemoSessionResponse(
        String token,
        String email,
        UUID userId,
        UUID resumeId,
        UUID currentVersionId,
        UUID baselineVersionId
) {
}
