package com.pedro.resumeapi.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedro.resumeapi.api.error.UnauthorizedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GoogleTokenVerifierService {

    private final GoogleAuthProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GoogleTokenVerifierService(GoogleAuthProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
    }

    public GoogleIdentity verifyIdToken(String idToken) {
        if (!StringUtils.hasText(idToken)) {
            throw new UnauthorizedException("google idToken required");
        }

        Set<String> allowedClientIds = resolveAllowedClientIds();
        if (allowedClientIds.isEmpty()) {
            throw new IllegalStateException("Google auth is not configured (missing app.auth.google.client-id)");
        }

        String encoded = URLEncoder.encode(idToken, StandardCharsets.UTF_8);
        String endpoint = properties.getTokenInfoUrl() + "?id_token=" + encoded;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new UnauthorizedException("invalid google token");
            }

            GoogleTokenInfo tokenInfo = objectMapper.readValue(response.body(), GoogleTokenInfo.class);
            if (!StringUtils.hasText(tokenInfo.aud()) || !allowedClientIds.contains(tokenInfo.aud())) {
                throw new UnauthorizedException("google token audience mismatch");
            }
            if (!"true".equalsIgnoreCase(tokenInfo.emailVerified())) {
                throw new UnauthorizedException("google email not verified");
            }
            if (!StringUtils.hasText(tokenInfo.email())) {
                throw new UnauthorizedException("google token missing email");
            }
            long exp = parseLong(tokenInfo.exp());
            if (exp <= Instant.now().getEpochSecond()) {
                throw new UnauthorizedException("google token expired");
            }

            return new GoogleIdentity(
                    tokenInfo.sub(),
                    tokenInfo.email().trim().toLowerCase(),
                    normalize(tokenInfo.name()),
                    normalize(tokenInfo.picture())
            );
        } catch (IOException e) {
            throw new UnauthorizedException("invalid google token");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UnauthorizedException("google auth interrupted");
        }
    }

    private Set<String> resolveAllowedClientIds() {
        String joined = String.join(",",
                nullSafe(properties.getClientId()),
                nullSafe(properties.getClientIds()));
        return Arrays.stream(joined.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception ex) {
            throw new UnauthorizedException("invalid google token expiration");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GoogleTokenInfo(
            String aud,
            String email,
            String email_verified,
            String exp,
            String sub,
            String name,
            String picture
    ) {
        String emailVerified() {
            return email_verified;
        }
    }

    public record GoogleIdentity(
            String sub,
            String email,
            String name,
            String pictureUrl
    ) { }
}
