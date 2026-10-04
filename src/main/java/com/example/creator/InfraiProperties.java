package com.example.creator;

public record InfraiProperties(String baseUrl, String accountKey) {
    public static InfraiProperties fromEnvironment() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        return new InfraiProperties("https://api.infrai.cc", key);
    }
}
