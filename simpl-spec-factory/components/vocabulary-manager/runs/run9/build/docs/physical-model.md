# Physical model in the RDF store (D87)

Store: Apache Jena Fuseki, dataset `vocabulary` (TDB2), staging dataset `vocabulary-restore` for restores.
Namespace of the store vocabulary: `vm: <https://simpl-programme.ec.europa.eu/ns/vocabulary-manager#>`.
SHACL shapes per named graph are in `docs/shapes/`; `StoreModelTest` runs them against data the service stored.

| Named graph | Holds | Shapes |
|---|---|---|
| `urn:simpl:vocabulary-manager:graph:vocabularies` | every vocabulary (`vm:Vocabulary`) and vocabulary version (`vm:VocabularyVersion`) | `shapes/vocabularies-graph.ttl` |
| `urn:simpl:vocabulary-manager:graph:external-vocabularies` | every external vocabulary and its aliases | `shapes/external-vocabularies-graph.ttl` |
| `urn:simpl:vocabulary-manager:content:<Name>:<n>` | the content of one version as base64 chunks | `shapes/content-graph.ttl` |
| `urn:simpl:vocabulary-manager:external-content:<Name>` | the content of one external vocabulary | `shapes/content-graph.ttl` |

IRIs: vocabulary `urn:simpl:vocabulary-manager:vocabulary:<Name>`, version `...:vocabulary:<Name>:version:<n>`,
external vocabulary `urn:simpl:vocabulary-manager:external-vocabulary:<Name>`, alias `...:alias:<i>`, content chunk
`<owner IRI>:chunk:<i>`. Names are checked against D37 before they reach an IRI.

Properties of a vocabulary: `vm:name`, `vm:nameKey` (lower case, for case-insensitive uniqueness), `vm:status`,
`vm:latestVersion`, `vm:createdBy`, `vm:creationDate`, `vm:lastUpdateDate` (`xsd:dateTime`, UTC, seconds).
Version: `vm:ofVocabulary`, `vm:version`, `vm:description`, `vm:changelog`, `vm:status`, `vm:createdBy`,
`vm:creationDate`, `vm:lastUpdateDate`, `vm:contentSha256`, `vm:contentLength`, `vm:contentGraph`.
External: `vm:name`, `vm:nameKey`, `vm:namespaceUri`, `vm:namespaceKey` (D30 comparison key), `vm:alias` (node
with `vm:uri`, `vm:key`, `vm:index`), `vm:sourceUri`, `vm:description`, `vm:createdBy`, `vm:creationDate`,
`vm:contentGraph`. Chunk: `vm:chunkOf`, `vm:index`, `vm:data` (`xsd:base64Binary`).

`vm:writeToken` is a short-lived marker of the conditional update protocol (D82): each write adds a random token
with its condition, the service asks whether the token is there (the condition held), then removes it.

Queries: every list query has ORDER BY, LIMIT and OFFSET with a separate COUNT (D85). Bounds above 101 rows: the
reference check (D12, Q5, at most 20 000 rows), the vocabulary search (D38, at most 10 000 rows), content reads
(at most `MAX_CHUNKS` = 130 chunks for the 6 MB request limit), the external list (21 rows per item for aliases).
