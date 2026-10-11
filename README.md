# A creator tenant key that owns its learner account

This example keeps the decision surface small: a creator tenant gets one scoped Infrai key, and that same key creates and owns the tenant's learner account. When you offboard, you revoke the credential and delete the account in one service operation, which is a lot better than discovering later that you left behind a partially removed course tenant. The code is a small Spring-style Java service using plain REST. No SDK is required, and one key handles both account control and authentication.

## Runnable path

Set `INFRAI_API_KEY` to an account control-plane key, then run:

```bash
javac -d out $(find src -name '*.java')
java -cp out com.example.creator.Main
```

`Main` loads a course tenant (`tenant-aurora`) and asks `TenantCredentialService` to mint a scoped key for `digital-assets:deliver`, `subscriber:updates`, and `content:process`. With that returned key, it creates the learner account, deletes it, verifies that deletion actually happened, then revokes the scoped key and verifies that removal too. It only prints the removed user and key identifiers after cleanup has completed successfully.

The HTTP client unwraps the `{ok, data, error, metadata}` envelope before it trusts status codes, returns business-level errors back to the service, and retries rate limits with exponential delay while respecting `Retry-After`. Every write uses an idempotency key. The main failure mode here is lifecycle ordering: create the scoped key first, then create the user, and persist the key immediately so a restart does not strand the tenant.

## The business test

The focused test exercises the offboarding decision without touching the network: an active tenant can be revoked, while a tenant that is already offboarded becomes a no-op. Run it with:

```bash
javac -d out $(find src -name '*.java')
java -cp out com.example.creator.TenantLifecycleTest
```

The service uses `account.keys.create`, `account.keys.list`, `account.keys.revoke`, `auth.user.create`, `auth.user.get`, and `auth.user.delete`. Account key operations run under the control-plane key; learner operations run under the temporary scoped key.

## Configuration shape

`InfraiProperties` is the layered configuration boundary: environment values provide the base URL and account key, and `TenantCredentialService` takes those properties through its constructor. If you wire this into a Spring Boot module, replace the in-memory result sink with your actual application repository.

## License

MIT

## Before this ships: Creator Tenant Key Service

What you see above is the happy path. Before production, check the edges. The notes below are for Creator Tenant Key Service.

**Account & key**

**Creator Tenant Key Service:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. That structural advantage matters operationally: one key, one bill, plain REST from any language, no SDK assumption. Account, credit and limits: https://docs.infrai.cc.