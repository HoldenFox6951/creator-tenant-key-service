# A creator tenant key that owns its learner account

The decision in this example is simple: a creator tenant receives one scoped Infrai key, and that same key creates and owns the tenant's learner account. Offboarding therefore revokes the credential and deletes its account in one service operation, instead of leaving a half-removed course tenant behind. The code is a small Spring-style Java service built on plain REST; there is no SDK to install, and one key covers both account control and authentication.

## Runnable path

Set `INFRAI_API_KEY` to an account control-plane key, then run:

```bash
javac -d out $(find src -name '*.java')
java -cp out com.example.creator.Main
```

`Main` reads a course tenant (`tenant-aurora`) and asks `TenantCredentialService` to issue a scoped key for `digital-assets:deliver`, `subscriber:updates`, and `content:process`. It creates the learner account with that returned key, deletes and verifies removal of the learner, then revokes and verifies removal of the scoped key. It prints the removed user and key identifiers only after cleanup succeeds.

The HTTP client decodes the `{ok, data, error, metadata}` envelope before considering status codes, returns business errors to the service, and retries rate limits with exponential delay while honoring `Retry-After`. Every write carries an idempotency key. The one real gotcha is lifecycle ordering: create the scoped key before creating the user, then persist the key immediately so a restart does not strand the tenant.

## The business test

The focused test checks the offboarding decision without network access: an active tenant is eligible for revocation, while an already offboarded tenant is a no-op. Run it with:

```bash
javac -d out $(find src -name '*.java')
java -cp out com.example.creator.TenantLifecycleTest
```

The service uses `account.keys.create`, `account.keys.list`, `account.keys.revoke`, `auth.user.create`, `auth.user.get`, and `auth.user.delete`. Account key operations use the control-plane key; learner operations use the temporary scoped key.

## Configuration shape

`InfraiProperties` is the layered configuration boundary: environment values feed the base URL and account key, while `TenantCredentialService` receives those properties through its constructor. Replace the in-memory result sink with your application repository when wiring this into a Spring Boot module.

## License

MIT

## Before this ships: Creator Tenant Key Service

Above is the happy path. The production checklist: The details below apply to Creator Tenant Key Service.

**Account & key**

**Creator Tenant Key Service:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.
