> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14728](../../../README.md) / [L2-SIMPL-14741](../../README.md) / [Epic-SIMPL-18030](../README.md) / **Story-SIMPL-31576**

# SIMPL-31576: [UI] Provider selects a usage policy template in the new resource description version wizard

**User story (implementable work)** · Status **PBI INTERNAL QA**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14728](../../../README.md) (L1) › [SIMPL-14741](../../README.md) (L2) › [SIMPL-18030](../README.md) (Epic) › **SIMPL-31576** (User story)

## Text

#### Storycard

**As a** Provider authorised to manage resource descriptions,\
**I want to** select a usage-policy template while creating a new resource-description version,\
**So that** I can add a usage policy from a reviewed template.

#### Acceptance criteria

##### Retrieve the list of usage-policy templates

| Given|I am selecting a usage-policy template in the new-version wizard |
| --- |

| When|the available template list is retrieved successfully |
| --- |

| Then|the UI displays the returned usage-policy templates with their user-facing names |
| --- |

##### Select a template from the list

| Given|the available usage-policy templates are displayed |
| --- |

| When|I select a template and its details are retrieved successfully |
| --- |

| Then|the selected template and its details are available for preparing the policy |
| --- |

Field entry remains covered by SIMPL-31577. Adding the policy remains covered by SIMPL-31579.

##### Continue without a selection

| Given|I have not selected a usage-policy template |
| --- |

| When|I continue to the next step |
| --- |

| Then|the wizard allows me to continue without adding a new usage policy |
| --- |

| And|it does not remove any carried-over policies |
| --- |

##### No templates are installed

| Given|I am creating a new resource-description version |
| --- |

| When|I reach the Usage policies step |
| --- |

| And|the template API returns an empty list for kind usage |
| --- |

| Then|the UI shows that no templates are available |
| --- |

| And|I can continue without adding a new policy |
| --- |

##### Template retrieval fails

| Given|I am creating a new resource-description version |
| --- |

| When|I reach the Usage policies step |
| --- |

| And|the template API returns an error |
| --- |

| Then|the UI shows an error and offers a retry |
| --- |

| And|no new template can be selected until retrieval succeeds |
| --- |

| And|I can continue without adding a new policy |
| --- |

#### Technical details

This story covers selection. The field-entry story covers rendering. Policy confirmation and final RD submission are separate actions. The shared API is defined in SIMPL-31345. A failure to retrieve new templates does not remove existing usage policies.

#### Additional notes

- (internal page)
- (internal page)

- Shared policy-template API definition, SIMPL-31345 (internal page)
- R08 ODRL template technical description (internal page)
- ODRL template architecture rationale (internal page)

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-18030](../README.md) (Epic) A Provider updates the usage policies of a new version of a resource description by using policy template via UI |
