package com.pedro.resumeapi.utils;

public final class TokenUtil {
    private TokenUtil() {}

    public static String newToken() {
        return java.util.UUID.randomUUID() + "-" + java.util.UUID.randomUUID();
    }

    public static String sha256Hex(String token) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            var bytes = md.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot hash token", e);
        }
    }
}
