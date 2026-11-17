# Digest notes: PSO run

## Per scenario

- **TC_CB_02** (`GET /agreements/<uuid>`, key, 404). Before: 404 (D12, D34). Now: same, confirmed.
- **TC_CB_04** (`/issue`, random ids, body `{}`, key, 404). Before: 400 `validationFailure` (D12, D25); the OpenAPI has no 404 here. Now: 404. `{}` counts as no body. Extended to every CM operation on an existing record (D12). New order of checks (D84).
- **TC_CB_06** (`GET /agreements/<uuid>/file`, key, 404). Before: 404. Now: confirmed.
- **TC_CB_07** (`GET /health`, no key, 401). Before: health open (D13, D65). Now: CM health needs the key. UIBE health stays open.
- **TCCB_SV03** (`/issue`, no key, 401, error schema). Before: 401 problem body, order unstated. Now: confirmed; 401 comes first (D84).
- **TCCB_SV04** (`/create` at the Provider, no key, 401). Before: 401, but also 400 `wrongParty` with no order. Now: 401 before party and body checks.
- **TCCB_SV05** (`/agreements/test-negotiation/search`, no key, 401). Before: 401. Now: confirmed, 401 before the missing `assetId`.
- **TCCB_SV07** (`GET /agreements/<uuid>`, key, 404, error schema). Before: 404 problem body. Now: confirmed. D11 also states that `status` equals the HTTP status, and that `instance` is optional and is the request path.
- **README environment**: new row D85.

## Too thin to decide

- Where 405, 406 and 415 sit against 401. Tests must send a valid key for those.
- A partial body on `/issue` (some fields, no nid). Only `{}` is decided.
- Whether UIBE follows the same rules. No scenario touches UIBE, so I left it unchanged.
- The bodies of TC_CB_02, 04 and 06 are not checked. Only SV07 checks a 404 body.

## Questionable scenarios

- **TC_CB_07**: it is tagged `@live-only`, and a health endpoint behind a key breaks plain liveness probes. In a live data space the 401 probably comes from the real gateway, not from CM. The stand-in forwards the request without a key, so CM must answer anyway (open questions 19, 20).
- **TC_CB_04**: random ids plus an empty body. It cannot tell "unknown id" apart from "empty body". The 404 may just reflect how the current build orders its checks.
- **Error schema**: it requires no field and allows any extra field, so any JSON object passes. Only empty or non-JSON bodies fail. It also says `instance` is a URI reference, while the OpenAPI `Problem` schema says it is an absolute URI.
