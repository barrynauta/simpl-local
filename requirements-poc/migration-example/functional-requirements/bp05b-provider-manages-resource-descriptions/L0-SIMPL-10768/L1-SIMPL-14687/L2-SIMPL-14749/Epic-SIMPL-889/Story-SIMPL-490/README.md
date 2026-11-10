> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14687](../../../README.md) / [L2-SIMPL-14749](../../README.md) / [Epic-SIMPL-889](../README.md) / **Story-SIMPL-490**

# SIMPL-490: Semantic Validation Engine

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14687](../../../README.md) (L1) › [SIMPL-14749](../../README.md) (L2) › [SIMPL-889](../README.md) (Epic) › **SIMPL-490** (User story)

## Text

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

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-889](../README.md) (Epic) The Governance Authority checks the semantics of a new Resource description or a new version of a Resource Description |
