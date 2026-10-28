# Test keys

`test-key.pem`, `test-pub.pem` and `other-key.pem` are throwaway RSA keys, generated for the integration tests in `tests/api.rs` only. The tests use them to sign fake access tokens (`other-key.pem` for a token with a wrong signature). They are not used by the running stack, which checks tokens against the Keycloak signing keys, and they give access to nothing. Safe to publish; a secret-scanning alert on them can be dismissed as a test fixture.
