package com.pedro.resumeworker.ai.provider;

import java.util.List;

public record AiProgressResult(
        String summary,
        String progressStatus,
        Integer progressScore,
        List<String> improvedAreas,
        List<String> unchangedIssues,
        List<String> newIssues,
        String providerModel
) {
}
