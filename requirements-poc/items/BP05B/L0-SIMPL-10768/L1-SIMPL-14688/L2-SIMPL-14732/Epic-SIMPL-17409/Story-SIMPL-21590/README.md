> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14732](../../README.md) / [Epic-SIMPL-17409](../README.md) / **Story-SIMPL-21590**

# SIMPL-21590: [UI] Add infrastructure resource type specific metadata

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14732](../../README.md) (L2) › [SIMPL-17409](../README.md) (Epic) › **SIMPL-21590** (User story)

## Text

#### Storycard

| As a|Provider end user |
| --- |

| I want|to add infrastructure resource type specific metadata to a new resource description via UI |
| --- |

| so that|I can ensure that the new resource description includes all relevant information to be compliant with the data space schema of the selected resource type. |
| --- |

#### Acceptance criteria

*Note: Out of scope is adding the deployment script. This story only covers general infrastructure metadata.*

| Scenario | A Provider adds infrastructure resource type specific metadata to a new Resource description via UI |
| --- | --- |

| Given|I am authenticated as a Provider end user |
| --- |

| and |I have a role authorising the management of Resource Descriptions |
| --- |

| and|I am creating a new resource description[link title](http://example.com/) |
| --- |

| when|I reach the step "Infrastructure metadata" in the wizard |
| --- |

| then|I can add infrastructure resource type specific metadata as defined in the schema of the selected resource type to the new resource description. |
| --- |
|  |  |

| Scenario | A Provider finishes adding infrastructure resource type specific metadata to a new Resource description via UI |
| --- | --- |

| Given|I am authenticated as a Provider end user |
| --- |

| and |I have a role authorising the management of Resource Descriptions |
| --- |

| and|I am creating a new resource description |
| --- |

| when|I reach the step "Infrastructure metadata" in the wizard |
| --- |

| and|I have added infrastructure resource type specific metadata to the new resource description |
| --- |

| then|I can navigate to the next step in the wizard. |
| --- |

**Technical details:**

Test for the resource type infrastructure, that during the creation of the resource description, the resource type specific properties is part of the wizard, meaning every property that is exclusively part of the Resource Description Schema.

- `{}Infrastructure: I{`}nfrastructurePropertiesShape - See also [Infrastructure Schema](https://code.europa.eu/simpl/simpl-open/development/data1/sdtooling-sd-schemas/-/blob/develop/shape/service-offering/infrastructure-offeringShape.ttl?ref_type=heads)

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-17409](../README.md) (Epic) A Provider adds resource type specific metadata to a new Resource description via UI |
