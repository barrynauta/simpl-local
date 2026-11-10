> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10192](../../../README.md) / [L2-SIMPL-1610](../../README.md) / [Epic-SIMPL-10194](../README.md) / **Story-SIMPL-13919**

# SIMPL-13919: Governance Authority intialization with built-in identity attributes

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10192](../../../README.md) (L1) › [SIMPL-1610](../../README.md) (L2) › [SIMPL-10194](../README.md) (Epic) › **SIMPL-13919** (User story)

## Text

**Background**

*The Security Attributes Provider is the component responsible for managing the list of identity attributes within a data space. As part of SIMPL-6485, a startup service was implemented to support the initial upload of a default list of identity attributes during component installation. The current implementation also allows this list to be fully customized by technical people during setup.*

*However, this approach lacks support for built-in identity attributes-a predefined set of attributes that must be consistently available across all SIMPL-Open data spaces. To address this:*

1. *The default list will be updated to include the built-in identity attributes as documented here:*\
   *(internal page)*
1. *The identity attributes contained in the list will be all marked as built in*
1. *Customization of the built-in list during setup will be disabled to preserve consistency. If a Governance Authority needs to introduce additional identity attributes specific to their data space, they can do so manually through the existing UI.*

----

**As a** Dataspace Governance authority,\
**I want** the Simpl-Open built-in identity attributes to be automatically uploaded in the Security Attributes Provider component when I install my agent,\
**So that** I can ensure a consistent and standardized set of identity attributes is available immediately, reducing manual setup effort and minimizing configuration errors.

**Acceptance Criteria**

1. **Given** I am setting up for the first time the Governance Authority Agent for a data space\
   **When** I complete all steps of the installation procedure for a Governance Authority agent\
   **Then** the Simpl-Open built-in identity attributes are automatically stored without manual action

2. **Given** the built-in identity attributes are uploaded\
   **When** I inspect the stored identity attributes\
   **Then** I can see that the identity attributes are all marked as built-in

3. **Given** I am updating the Governance Authority agent\
   **And** the previous version of the agent does not have identity attributes flagged as built-in\
   **When** the upload process completes\
   **Then** the already existing identity attributes whose CODE is equal to a new built-in identity attributes will be updated and marked as built-in

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10194](../README.md) (Epic) Setup default identity attributes in the Governance Authority Agent |
