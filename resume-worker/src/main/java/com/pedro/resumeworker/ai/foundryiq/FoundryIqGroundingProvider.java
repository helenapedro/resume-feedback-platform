package com.pedro.resumeworker.ai.foundryiq;

import com.pedro.common.ai.Language;
import com.pedro.common.ai.mongo.AiFeedbackDocument;

public interface FoundryIqGroundingProvider {

    FoundryIqGroundingProvider NONE = new FoundryIqGroundingProvider() {
        @Override
        public String feedbackGrounding(Language language, String resumeText) {
            return "";
        }

        @Override
        public String progressGrounding(
                Language language,
                String currentResumeText,
                String previousResumeText,
                AiFeedbackDocument previousFeedback) {
            return "";
        }
    };

    String feedbackGrounding(Language language, String resumeText);

    String progressGrounding(
            Language language,
            String currentResumeText,
            String previousResumeText,
            AiFeedbackDocument previousFeedback);
}
