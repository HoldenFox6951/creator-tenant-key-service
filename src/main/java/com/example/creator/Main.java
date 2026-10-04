package com.example.creator;

public final class Main {
    public static void main(String[] args) {
        InfraiProperties p = InfraiProperties.fromEnvironment();
        TenantCredentialService service = new TenantCredentialService(new InfraiClient(p), p);
        TenantCredentialService.IssuedTenant tenant = service.issue("tenant-aurora", "chenhua@changba.com");
        System.out.println("Verified removal of learner " + tenant.userId() + " and scoped key " + tenant.keyId());
    }
}
