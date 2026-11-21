# Physical model: RDF store (D87, D106)

Apache Jena Fuseki 6.2.0, one TDB2 dataset `vocabulary`. Shapes: `physical-model-shapes.ttl` (checked by `PhysicalModelShaclTest` against data written through the API).

| Named graph | Content |
|---|---|
| `urn:simpl:vocabulary-manager:graph:internal` | `vm:InternalVocabulary` and `vm:InternalVocabularyVersion` resources |
| `urn:simpl:vocabulary-manager:graph:external` | `vm:ExternalVocabulary` resources |
| `urn:simpl:vocabulary-manager:graph:content` | `vm:content` of each version and each external vocabulary |

Namespace `vm:` = `https://w3id.org/simpl/vocabulary-manager#`.

| Resource | IRI |
|---|---|
| Vocabulary | `urn:simpl:vocabulary-manager:vocabulary:<Name>` |
| Version | `urn:simpl:vocabulary-manager:vocabulary:<Name>:version:<n>` |
| External vocabulary | `urn:simpl:vocabulary-manager:external:<Name>` |

Access structures: TDB2 keeps its standard triple and quad indexes (SPOG, POSG, OSPG and permutations), so lookups by subject IRI, by `vm:nameKey`, `vm:namespaceKey` and `vm:aliasKey` are index lookups.

Technical properties, not in the logical model:

- `vm:nameKey` (lower-case name, uniqueness, D37), `vm:namespaceKey` and `vm:aliasKey` (URI without trailing '#' or '/', D30), `vm:aliasList` (aliases in the order sent, space separated, D73).
- `vm:writeToken` and `vm:statusToken`: the request id of the last conditional write, used to detect whether this request's update applied (D82).
- `vm:contentSha256` and `vm:contentLength`.
- `vm:content` holds the uploaded bytes: a plain string literal when they are valid UTF-8 without control characters, otherwise `xsd:base64Binary` (bytes kept exactly as uploaded, D29).

Every write is one SPARQL update request, so one TDB2 write transaction (D8). Updates carry a WHERE condition on the state the request read (latest version, current status, name not taken); a request whose condition failed changes nothing and retries (D82, D157).
