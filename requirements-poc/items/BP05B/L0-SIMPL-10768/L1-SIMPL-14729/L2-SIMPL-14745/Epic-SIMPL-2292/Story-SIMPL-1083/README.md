> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14729](../../../README.md) / [L2-SIMPL-14745](../../README.md) / [Epic-SIMPL-2292](../README.md) / **Story-SIMPL-1083**

# SIMPL-1083: Simpl Self-Description Frontend

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14729](../../../README.md) (L1) › [SIMPL-14745](../../README.md) (L2) › [SIMPL-2292](../README.md) (Epic) › **SIMPL-1083** (User story)

## Text

As a provider user I want to have an interface (using html forms) that I can use to define self-descriptions of various shape constraints defined using the SHACL ontology

- the interface should be dynamic and displayed based on the shape constraints
- we should have no external branding visible on the interface
- the form elements should display possible constraints and validations as data is inputed
- when submitting the form I want a JSON-LD to be created and the shape as defined to be stored on the federated catalogue (with all the prerequisite steps, eg: signing )
- initially the interface (form) should be able to handle shacl vocabulary as presented on SD-Tooling: [https://gitlab.eclipse.org/eclipse/xfsc/self-description-tooling/sd-creation-wizard-frontend/-/blob/main/documentation/shacl-angular-mapping.md?ref_type=heads](https://gitlab.eclipse.org/eclipse/xfsc/self-description-tooling/sd-creation-wizard-frontend/-/blob/main/documentation/shacl-angular-mapping.md?ref_type=heads)
- I want the ability to change the shape for which I can define self description

Acceptance Criteria:

- I should be able to select different shapes for which I can create self-descriptions
- inputing incorrect data should provide visual feedback (show errors)
- form should not be able to submit with errors
- when submitting I should be notified of status (success, error)
- I should be able to find the submitted data in the federated catalogue

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-2292](../README.md) (Epic) A Provider signs and  submits a new Resource description or a new version of a Resource description |
