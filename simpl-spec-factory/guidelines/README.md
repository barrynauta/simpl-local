# Guidelines: the fixed instructions

The guidelines every build must follow, whatever the component. Step 11 turns them into numbered rules ([`rules.md`](./rules.md), [`rules.json`](./rules.json)) so the later steps can check and report per rule instead of re-reading prose.

Frozen copies, collected **2026-11-17**. Do not refresh them in place: a new copy is a new, dated input set, and step 11 runs again on it.

## What is here

| File | Source |
|---|---|
| `sources/architecture/api-guidelines.md` | code.europa.eu `simpl/simpl-open/foundations/architecture`, commit `5d28e04a` (main, 2026-11-14) |
| `sources/architecture/coding-guidelines.md` | same commit |
| `sources/architecture/architecture-principles.md` | same commit |
| `sources/architecture/architectural-patterns.md` | same commit |
| `sources/architecture/data-modeling-guidelines.md` | same commit |
| `sources/architecture/security.md` | same commit |
| `sources/architecture/devsecops.md` | same commit |
| `sources/nfr/secure-coding-guidelines-owasp-top-ten.md` | code.europa.eu `simpl/simpl-open/foundations/non-functional-requirements`, commit `b71d016` (main), NFR14 |
| `sources/nfr/enforce-coding-and-development-guidelines.md` | same, NFR08 |
| `sources/nfr/uis-must-follow-ux-guidelines.md` | same, NFR15 |
| (engine) | The factory's own rules moved to the engine on 17 Nov 2026: DDD, MACH, microservices, OWASP Top 10:2025, OWASP API Security Top 10 2023 and the Belgif REST guide, 146 rules in `spec-factory/guidelines/rules.json`. The scripts read them together with these Simpl rules (17 Nov 2026): domain-driven design, ubiquitous language and the entities in the APIs. Rules `DDD-01` to `DDD-15`, written by hand from this page |

Images under `media/` are not copied; the rules come from the text.

## Result of step 11 (17 Nov 2026)

277 rules from the Simpl guidelines (the DDD rules are engine rules since 17 Nov 2026): API 80, CODE 55, OPS 37, ARCH 35, DATA 29, SEC 23, OWASP 14, UX 4. MUST 84, SHOULD 192, MAY 1 (many sources have no modal verb, so the default SHOULD applies, with a note). Check type: tool 93, test 44, review 125, none 15. Conflicts, vague rules and the documents the sources refer to but that are not here are listed at the end of [`rules.md`](./rules.md). OWASP: NFR14 names no version; Top 10:2021 was used, to be confirmed.

## Not here yet

- **The PSO Development Handbook** (v1.8, with the PSO coding guidelines). It is not published, so a copy belongs in `sources/_private/` (gitignored) and its rules get the prefix `HB-`. Without it, step 11 covers the published guidelines only.
- **The OWASP Top 10 itself.** NFR14 points to it; step 11 uses the version NFR14 names, as a public standard, one rule per category.
