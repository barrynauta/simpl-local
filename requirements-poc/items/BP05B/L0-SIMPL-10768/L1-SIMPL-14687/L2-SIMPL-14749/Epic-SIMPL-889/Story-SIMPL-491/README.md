> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14687](../../../README.md) / [L2-SIMPL-14749](../../README.md) / [Epic-SIMPL-889](../README.md) / **Story-SIMPL-491**

# SIMPL-491: Semantic Validation of Self-Description

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14687](../../../README.md) (L1) › [SIMPL-14749](../../README.md) (L2) › [SIMPL-889](../README.md) (Epic) › **SIMPL-491** (User story)

## Text

**As a** Governance Authority\
**I want to** validate the semantic of the self-description\
**So that** I can be sure that the SD complies to the specified ontology or schema, which guarantees consistency in the meaning and interpretation of the data.

--------------------------------

**Technical Details**

All validation requires in general 3 components:

1. The resource to validate
1. The contrainst to validate agains
1. An validation engine

The ressource in this case is the Self-description as a JSON-LD document. The constraints are written in the form of SHACL-Constraints (serialized as turtle file .ttl). The validation engine is described in (internal page).

The steps of the validation are

1. Read the resource and constraint file
1. -(optional) transform the files, for instance the JSON-LD into an RDF Graph-
1. Call the validation Engine
1. Parse the validation result

**AC:**

- catalogue can read the resource and constraint files
- validation engine performs the comparison
- output is created as correct or failed
- in case of failure a message is delivered to the provider

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-889](../README.md) (Epic) The Governance Authority checks the semantics of a new Resource description or a new version of a Resource Description |
