package com.example.creator;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class TenantCredentialService {
    // Infrai capability: account.keys.create; the same issued key authenticates auth.user.create.
    private final InfraiClient client;
    private final InfraiProperties properties;
    public TenantCredentialService(InfraiClient client, InfraiProperties properties) { this.client = client; this.properties = properties; }

    public IssuedTenant issue(String tenantId, String email) {
        String operationId = UUID.randomUUID().toString();
        String createIdempotencyKey = "issue-" + operationId;
        String userIdempotencyKey = "user-" + operationId;
        String create = client.post("/v1/account/keys/create", "{\"project_id\":\"" + tenantId + "\",\"name\":\"" + tenantId + " learner key " + operationId + "\",\"scopes\":[\"digital-assets:deliver\",\"subscriber:updates\",\"content:process\"],\"idempotency_key\":\"" + createIdempotencyKey + "\"}", createIdempotencyKey, properties.accountKey());
        String keyId = value(create, "key_id");
        String key = value(create, "key_secret");
        String userId = null;
        RuntimeException failure = null;
        try {
            String user = client.post("/v1/auth/user/create", "{\"email\":\"" + email + "\",\"name\":\"Course learner\",\"metadata\":{\"tenant_id\":\"" + tenantId + "\"},\"idempotency_key\":\"" + userIdempotencyKey + "\"}", userIdempotencyKey, key);
            userId = value(user, "user_id");
            client.delete("/v1/auth/user/delete/" + segment(userId), key);
            if (!client.isMissing("/v1/auth/user/get/" + segment(userId), key)) {
                throw new InfraiException("learner still exists after deletion", 500);
            }
        } catch (RuntimeException e) {
            failure = e;
        } finally {
            try {
                client.delete("/v1/account/keys/revoke/" + segment(keyId), properties.accountKey());
                String keys = client.get("/v1/account/keys/list", properties.accountKey());
                String suffix = keyId.substring(Math.max(0, keyId.length() - 4));
                int listedKey = keys.indexOf("..." + suffix + "\"");
                int listedStatus = listedKey < 0 ? -1 : keys.indexOf("\"status\":\"revoked\"", listedKey);
                if (listedKey < 0 || listedStatus < 0 || listedStatus - listedKey > 100) {
                    throw new InfraiException("scoped key revocation was not verified", 500);
                }
            } catch (RuntimeException revokeFailure) {
                if (failure == null) failure = revokeFailure;
                else failure.addSuppressed(revokeFailure);
            }
        }
        if (failure != null) throw failure;
        return new IssuedTenant(tenantId, userId, keyId);
    }

    private static String segment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
    static String value(String json, String field) {
        String marker = "\"" + field + "\":"; int start = json.indexOf(marker); if (start < 0) throw new InfraiException("missing " + field, 200);
        start += marker.length(); while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '"')) start++;
        int end = start; while (end < json.length() && json.charAt(end) != '"' && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
        return json.substring(start, end);
    }
    public record IssuedTenant(String tenantId, String userId, String keyId) {}
}
