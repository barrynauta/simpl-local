# Experiment: can a supplier build the Vocabulary Manager from public information only? (26 Nov 2026)

## Question

The factory's input for the Vocabulary Manager mixes public and non-public sources. A supplier without access to the programme's Jira and Confluence would only have the public ones. How much does that supplier miss?

| Source | Public | Role in the factory |
|---|---|---|
| OpenAPI `vocabulary_openapi-tier1.yaml` (code.europa.eu) | yes | operations, schemas, status codes |
| Business process BP02B (code.europa.eu) | yes | process steps and requirements |
| CDMs `vocabulary-manager-be`, `schema-manager-be` (code.europa.eu) | yes | domain model |
| 35 Jira stories with 85 live acceptance criteria | no (account needed) | most of the behaviour, and the whole UI (20 UI stories) |
| R07.02 tech description (Confluence) | no (not published) | lifecycle, validation rules, external vocabularies |
| Figma design | not reachable | not used by the factory either |

## Method

- **A. Basis of the decisions.** Each of the 223 live decisions (D, E, Q; one withdrawn) names its basis. A script sorted them by the sources the basis cites.
- **B. Public-only reading.** A fresh agent with no access to anything else read only the three public sources (plus the neighbouring CDM for context) as a builder and tester would, and listed blockers, majors and minors with the step 29 definitions. It was not shown the existing findings or decisions.

## Results

### A. Where the decisions come from

| Basis | Decisions | For a public-only supplier |
|---|---|---|
| No specification covers the point (rules, earlier runs, reasoning) | 163 | the same gap with or without access |
| Public sources only | 23 | available |
| Public and non-public sources | 27 | partly available |
| Non-public sources only (for example D19 name characters, D21 unknown query parameters, E1 test users and roles) | 8 | missing: a guess |
| No basis recorded | 2 | |

Most decisions fill gaps that no specification covers at all. Access to Jira and the tech description matters for about 35 decisions, and for the 85 acceptance criteria that tag about 270 of the 668 test cases of run 14.

Of the 14 blockers found with all sources (step 29), 10 came from reading the stories, 3 from the domain language and 1 from the API.

### B. Public-only reading

- **10 blockers, 24 majors, 13 minors.**
- The agent's estimate: **about 30 % of the behaviour is specified**. The OpenAPI fixes the shape of the backend well (55 to 60 % of the API), but the rules behind the operations are mostly missing, and the admin UI (roughly half of the deliverable) is not specified at all.

| Public-only blocker | Also a blocker with all sources? |
|---|---|
| Vocabulary status versus version status | yes, F-001 |
| Who may do what (roles, 401 and 403) | yes, F-002 |
| Error versus warning on upload | yes, F-003, F-004, D-003 |
| What semantic validation checks | yes, F-005 |
| Status transitions allowed by PATCH | partly, F-001 |
| Publish step in the process, absent from the API | no (decided, not a step 29 blocker) |
| What "latest" means | no |
| Namespace of an internal vocabulary and references between vocabularies | no |
| External vocabularies: URI normalisation and uniqueness | no |
| The admin UI is not specified at all | no: 20 UI stories cover it |

What the public files support with confidence: the REST surface and DTOs, the happy path of create and new version, Turtle syntax checks with line and column, the size limit, external vocabulary registration, and health.

## What it means

- **Public information is not enough to build the component.** About 30 % of the behaviour is specified, and the UI not at all.
- **Access is not the main problem either.** The four hardest blockers (status levels, roles, error versus warning, validation rules) stay blockers with every source available; in some cases the non-public stories create them by contradicting the OpenAPI. And 163 of 223 decisions close gaps that no source covers.
- For a successor contract this means two separate needs: publish the stories, acceptance criteria and tech descriptions (or hand them over), and close the gaps that exist in all sources with written decisions by the owners.

## Limits

- One agent reading, once. The full step 20 to 29 pipeline uses four agents, mechanical checks and triage, so the counts are indicative, not equal measures. Fewer sources also means fewer contradictions to find, which lowers the count.
- The decision basis texts were written by the decision drafter and are not always complete; the classification is by pattern, not by hand.
- Same AI vendor as the rest of the factory.
