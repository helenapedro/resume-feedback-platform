package com.pedro.resumeworker.ai.provider;

import java.util.List;

public record AiFeedbackResult(
        String summary,
        List<String> strengths,
        List<String> improvements,
        String providerModel
) {
}
