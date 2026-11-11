<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.5 - The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md) / [5B.5.7 The Governance Authority validates the semantics of a new Resource description or a new version of a Resource Description](../bp5b5-governance-authority-publishes-resource-description.md#5b57-the-governance-authority-validates-the-semantics-of-a-new-resource-description-or-a-new-version-of-a-resource-description) / Epic
</div>

# Epic: The Governance Authority checks the semantics of a new Resource description or a new version of a Resource Description

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.5.7 The Governance Authority validates the semantics of a new Resource description or a new version of a Resource Description](../bp5b5-governance-authority-publishes-resource-description.md#5b57-the-governance-authority-validates-the-semantics-of-a-new-resource-description-or-a-new-version-of-a-resource-description), under high-level requirement [5B.5 The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md). In the process it belongs to step [BP05B.05 Validate the resource description](../README.md#bp05b05-validate-the-resource-description).

In the backlog (Jira) it is item SIMPL-889. It has 2 user stories, each with acceptance criteria that say when the work is done. For each story, the development status shows whether it is Planned, In development or Developed (built, not necessarily released yet).

## What this Epic delivers

#### Storycard

**As** Governance Authority\
**I want to** check the semantics of a new resource description or a new version of a resource description\
**So that** the published resource descriptions are consistent, reliable and compliant with pre-defined standards.

#### Acceptance criteria

**Scenario:** The Governance Authority checks the semantics of a new Resource description or a new version of a Resource Description with valid semantics (Semantic validation - positive case)\
When I, the Governance Authority, receive a new resource description or a new version of a resource description with valid semantics\
and the Provider is authorised with the identity attributes to manage resource descriptions\
Then the semantics of the content or the resource description is automatically checked\
and the semantic validation for the resource description or new version of a resource description has the result "valid".

**Scenario:** The Governance Authority checks the semantics of a new Resource description or a new version of a Resource Description with invalid semantics (Semantic validation - negative case)\
When I, the Governance Authority, receive a new resource description or a new version of a resource description with invalid semantics\
and the Provider is authorised with the identity attributes to manage resource descriptions\
Then the semantics of the content or the resource description is automatically checked\
and the semantic validation for the resource description or new version of a resource description has the result "invalid".

## User stories

1. [Semantic Validation Engine](#user-story-1-semantic-validation-engine) · Developed
2. [Semantic Validation of Self-Description](#user-story-2-semantic-validation-of-self-description) · Developed

### User story 1: Semantic Validation Engine

**Development:** Developed · backlog item SIMPL-490

**As a** Governance Authority agent\
**I want** integrate a validation engine that is able the validate the semantic of the self-description.\
**So that** I can be sure that the SD complies to the specified ontology or schema, which guarantees consistency in the meaning and interpretation of the data.

------------------

Technical detail

A potential engine to integrate is [https://pypi.org/project/pyshacl/](https://pypi.org/project/pyshacl/) if we can use python for the validation. For Java [https://rdf4j.org/](https://rdf4j.org/) or Apache Jena provides an validation engine. The Self-Description Tooling already contains an implementation to validate SHACL-Constraints that uses pySHACL [https://gitlab.eclipse.org/eclipse/xfsc/self-description-tooling/sd-schemas/sd-schemas/-/blob/main/toolchain/check_shacl.py?ref_type=heads](https://gitlab.eclipse.org/eclipse/xfsc/self-description-tooling/sd-schemas/sd-schemas/-/blob/main/toolchain/check_shacl.py?ref_type=heads)

Also Gaia-X contains a method for the semantic validation in the repository

[https://gitlab.eclipse.org/eclipse/xfsc/cat/fc-service](https://gitlab.eclipse.org/eclipse/xfsc/cat/fc-service)

[https://gitlab.eclipse.org/eclipse/xfsc/cat/fc-service/-/blob/main/fc-service-core/src/main/java/eu/xfsc/fc/core/service/verification/VerificationServiceImpl.java?ref_type=heads](https://gitlab.eclipse.org/eclipse/xfsc/cat/fc-service/-/blob/main/fc-service-core/src/main/java/eu/xfsc/fc/core/service/verification/VerificationServiceImpl.java?ref_type=heads)

Remark: the method `verifySelfDescription` provides several verification systems. The syntax verification is done in all cases the others can be activated/deactivated by setting the boolean parameters. To check against the vocabulary like DCAT we need to use the `verifySchema` functionality.

### User story 2: Semantic Validation of Self-Description

**Development:** Developed · backlog item SIMPL-491

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

---

Back to [5B.5.7 The Governance Authority validates the semantics of a new Resource description or a new version of a Resource Description](../bp5b5-governance-authority-publishes-resource-description.md#5b57-the-governance-authority-validates-the-semantics-of-a-new-resource-description-or-a-new-version-of-a-resource-description) · [5B.5 The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md) · [BP05B process page](../README.md)
