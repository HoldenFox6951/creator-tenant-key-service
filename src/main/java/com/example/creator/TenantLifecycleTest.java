package com.example.creator;

public final class TenantLifecycleTest {
    public static void main(String[] args) {
        TenantState active = new TenantState("tenant-aurora", true);
        if (!active.shouldRevoke()) throw new AssertionError("active tenant must be revoked");
        TenantState offboarded = new TenantState("tenant-aurora", false);
        if (offboarded.shouldRevoke()) throw new AssertionError("offboarded tenant is already complete");
        System.out.println("Tenant lifecycle decision passed");
    }
    record TenantState(String id, boolean credentialActive) { boolean shouldRevoke() { return credentialActive; } }
}
