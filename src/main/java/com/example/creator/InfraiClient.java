package com.example.creator;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;

public final class InfraiClient {
    private final InfraiProperties properties;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public InfraiClient(InfraiProperties properties) { this.properties = properties; }

    public String post(String path, String json, String idempotencyKey, String bearer) {
        return request("POST", path, json, idempotencyKey, bearer);
    }

    public String get(String path, String bearer) {
        return request("GET", path, null, null, bearer);
    }

    public String delete(String path, String bearer) {
        return request("DELETE", path, null, null, bearer);
    }

    public boolean isMissing(String path, String bearer) {
        try {
            get(path, bearer);
            return false;
        } catch (InfraiException e) {
            if (e.status() == 404) return true;
            throw e;
        }
    }

    public String request(String method, String path, String json, String idempotencyKey, String bearer) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(properties.baseUrl() + path))
                    .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + bearer)
                    .header("Content-Type", "application/json")
                    .method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json));
                if (idempotencyKey != null) request.header("Idempotency-Key", idempotencyKey);
                HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
                String body = response.body();
                if (!body.contains("\"ok\":true")) throw new InfraiException(body, response.statusCode());
                if (response.statusCode() == 429 && attempt < 2) { Thread.sleep(100L << attempt); continue; }
                if (response.statusCode() >= 500) throw new InfraiException(body, response.statusCode());
                return body;
            } catch (InfraiException e) { throw e; }
            catch (Exception e) { throw new InfraiException(e.getMessage(), 503); }
        }
        throw new InfraiException("rate limit", 429);
    }
}
