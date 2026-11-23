# Bug reports: simpl-sd-ui

Target: https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-sd-ui

Each numbered section is a self-contained issue, ready to paste into GitLab separately.

Test environment common to all findings:

| Item | Value |
|---|---|
| Date tested | 2026-07-21 |
| Version | v1.15.0, HEAD '3c95eee' |
| '@simpl/vue-components' | 2.24.2 |
| Node / npm | v25.8.2 / 11.12.1 |

---

## Issue 1: Keycloak URLs are built with the pre-17 '/auth' context path

**Severity: high if deployed against a supported Keycloak. Portability defect.**

### Details

'src/services/keycloak.ts:15' hardcodes the legacy '/auth' prefix when building every Keycloak endpoint:

'''ts
const keycloakBaseUrl = '${keycloakServer}/auth/realms/${realm}/protocol/openid-connect';
'''

Keycloak removed '/auth' from the default context path in version 17 (2022), when the WildFly distribution was replaced by Quarkus. Current Keycloak is 26.x.

### Steps to reproduce

1. Clone the repository and run 'npm install'.
2. Create '.env' using the Installation Guide's own example values:
   '''
   PUBLIC_AUTH_KEYCLOAK_SERVER_URL="https://auth.example.com"
   PUBLIC_AUTH_KEYCLOAK_REALM="simpl"
   PUBLIC_AUTH_KEYCLOAK_CLIENT_ID="sd-ui"
   '''
3. Run 'npm run dev'.
4. Request the landing page and follow the redirect: 'curl -i http://localhost:4321/'.

### Actual Result

The first request returns a 302 to a URL carrying the legacy path segment:

'''
302 -> https://auth.example.com/auth/realms/simpl/protocol/openid-connect/auth
       ?client_id=sd-ui&response_type=code&redirect_uri=http%3A%2F%2Flocalhost%3A4321%2F
       &scope=openid&code_challenge_method=S256&code_challenge=...
'''

Against a Keycloak 17 or newer running on default settings, that path does not exist.

### Expected Result

The redirect targets 'https://auth.example.com/realms/simpl/protocol/openid-connect/auth', matching the default layout of every supported Keycloak release, with the relative path configurable for installations that still serve under '/auth'.

### Additional Information

**This is a code-reading and redirect-URL finding, not an end-to-end reproduction.** No Keycloak instance was available during testing, so the 404 against a real modern Keycloak was not observed.

If Simpl deployments currently set 'PUBLIC_AUTH_KEYCLOAK_SERVER_URL' to a value that already absorbs the prefix, or run Keycloak with '--http-relative-path=/auth', the application works today by configuration rather than by design. Either way, the guide's own documented example value produces a broken redirect against a default modern IdP, so a user following the documentation exactly is misled.

Suggested fix: drop the hardcoded '/auth' and derive the base from the server URL as supplied, or introduce a configurable relative path defaulting to empty.

---

## Issue 2: Upstream error bodies are passed through to the browser unmodified

**Severity: medium. Information disclosure.**

### Details

'safeServiceCall' in 'src/services/util/apiErrorHandler.ts' wraps only *thrown* errors. A non-2xx 'Response' from the SD Tooling API is returned to the client untouched, so backend internals reach the browser verbatim.

'''ts
export const safeServiceCall = async (serviceName, methodName, ...args) => {
  try {
    const service = await createMappedServiceSelector(serviceName);
    return await service[methodName](...args);   // upstream 4xx/5xx returned as-is
  } catch (error) {
    return createProblemDetailsResponse(undefined, 'Service unavailable', 503, ...);
  }
};
'''

### Steps to reproduce

1. Start 'sdtooling-api-be' without '--schema-sync-service.repository-path', so '/v2/schemas' returns a 500.
2. Point the SD UI at it and start the dev server.
3. Call the BFF endpoint directly: 'curl -s http://localhost:4321/api/schemas'.

### Actual Result

The BFF returns the upstream 500 body unchanged, including the Java type name and Jackson parser token:

'''json
{"type":"urn:problem-type:simpl:internalServerError","title":"Internal Server Error",
 "status":500,"detail":"schema metadata file IO error",
 "issues":[{"detail":"Cannot deserialize value of type
 'eu.europa.ec.simpl.data1.common.model.schemasync.SchemaMetadata'
 from Array value (token 'JsonToken.START_ARRAY') ..."}]}
'''

This text then reaches the rendered page.

### Expected Result

The BFF inspects the upstream status and translates non-2xx responses into a sanitised problem-details payload with a stable error code, logging the original detail server-side only.

### Additional Information

This is one half of a shared defect. The rendering half is filed against 'simpl-vue-components', which displays whatever it receives without sanitising. Fixing either side alone leaves the hole open.

---

## Issue 3: Installation Guide is inaccurate in three checkable places

**Severity: low, but it is the first thing every evaluator encounters.**

### Details

Following 'documents/Installation Guide.md' literally on a clean clone, three of its instructions do not match the software's behaviour.

### Steps to reproduce

1. Clone the repository fresh.
2. Create '.npmrc' with only the registry line, no auth token:
   '@simpl:registry=https://code.europa.eu/api/v4/packages/npm/'
3. Run 'npm install'.
4. Create '.env' per the guide's Configuration section and run 'npm run dev'.
5. Separately, request the landing page with a valid-looking Keycloak config.

### Actual Result

| Guide states | Actual behaviour |
|---|---|
| "The application will be available at 'http://localhost:4322'", stated for both local dev and Docker | 'npm run dev' serves on **4321** |
| A code.europa.eu PAT with 'read_api' is required for '@simpl/vue-components', with a full "Package Registry Configuration" section and 401 troubleshooting | **Not required.** 'GET https://code.europa.eu/api/v4/packages/npm/@simpl/vue-components' returns 200 anonymously. 'npm install' completed in 6 seconds with a token-less '.npmrc' |
| The three 'PUBLIC_AUTH_KEYCLOAK_*' variables are listed under "Required Environment Variables" | Setting all three to **empty strings** disables authentication entirely, and is the only way to run the application without an IdP. This is undocumented |

Following the guide as written, the first 'GET /' returns a 302 to Keycloak. Without a working IdP, an evaluator cannot render a single page, and nothing in the documentation indicates a supported way around it.

### Expected Result

The port reads 4321. The PAT section is downgraded to "only needed for private packages or CI". The empty-string switch is documented as a supported no-authentication evaluation mode.

### Additional Information

The undocumented bypass is implemented in 'src/services/keycloak.ts:20':

'''ts
export const isAuthenticationEnabled = () =>
  keycloakServer?.length > 0 && clientId?.length > 0 && realm?.length > 0;
'''

and consumed in 'src/middleware.ts:17':

'''ts
if (ctx.url.pathname.startsWith('/status') || !isAuthenticationEnabled()) {
  return setCacheHeaders(await next());
}
'''

This is the single largest barrier to evaluating the component, and it is a documentation problem rather than a code one. The mechanism already exists and works.

---

## Issue 4: npm audit reports 20 vulnerabilities, 3 critical

**Severity: to be triaged.**

### Details

A clean install of v1.15.0 reports a significant number of dependency vulnerabilities.

### Steps to reproduce

1. Clone the repository fresh at v1.15.0.
2. Run 'npm install'.
3. Run 'npm audit' for the breakdown.

### Actual Result

'''
20 vulnerabilities (3 low, 11 moderate, 3 high, 3 critical)
'''

### Expected Result

No critical or high vulnerabilities in a released version, or a documented set of accepted risks with justification.

### Additional Information

Worth triaging because the repository's recent history includes commit 'eeed32c' "Fix security vulnerabilities remarked by Fortify", which indicates dependency scanning is already in scope for this project. The npm dependency tree appears not to be covered by whatever scanning is in place.

Some of these may be transitive and non-exploitable in this application's usage. The point of the issue is to get them triaged and recorded, not to assert that all 20 are exploitable.
