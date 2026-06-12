package com.pedro.resumeworker.ai.openai;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Component
class OpenAiTransport {

    private static final String INVALID_RESPONSE_ERROR_CODE = "AI_PROVIDER_INVALID_RESPONSE";
    private static final String INTERRUPTED_ERROR_CODE = "AI_PROVIDER_INTERRUPTED";

    private final HttpClient httpClient;
    private final OpenAiResponseHandler responseHandler;

    OpenAiTransport(HttpClient openAiHttpClient, OpenAiResponseHandler responseHandler) {
        this.httpClient = openAiHttpClient;
        this.responseHandler = responseHandler;
    }

    OpenAiCallResult send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return responseHandler.handle(response);
        } catch (IOException ex) {
            return OpenAiCallResult.failure(INVALID_RESPONSE_ERROR_CODE, ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return OpenAiCallResult.failure(INTERRUPTED_ERROR_CODE, ex.getMessage());
        }
    }
}
