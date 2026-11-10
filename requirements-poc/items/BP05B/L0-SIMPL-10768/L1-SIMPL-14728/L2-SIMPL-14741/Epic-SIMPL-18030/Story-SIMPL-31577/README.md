> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14728](../../../README.md) / [L2-SIMPL-14741](../../README.md) / [Epic-SIMPL-18030](../README.md) / **Story-SIMPL-31577**

# SIMPL-31577: [UI] Provider fills in the usage policy template parameters in the new resource description version wizard

**User story (implementable work)** · Status **PBI INTERNAL QA**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14728](../../../README.md) (L1) › [SIMPL-14741](../../README.md) (L2) › [SIMPL-18030](../README.md) (Epic) › **SIMPL-31577** (User story)

## Text

#### Storycard

**As a** Provider authorised to manage resource descriptions,\
**I want to** enter values in the selected usage-policy template,\
**So that** I can prepare a usage policy for the new RD version.

#### Acceptance criteria

##### Render the template fields

| Given|I selected a usage-policy template in the new-version wizard |
| --- |

| When|its details are retrieved |
| --- |

| Then|the UI renders its ordered fields using their labels, types and controls |
| --- |

| And|it retrieves approved choices where the field defines a value source |
| --- |

##### Keep entered values

| Given|the selected template's fields are displayed |
| --- |

| When|I enter or change a field value |
| --- |

| Then|the wizard keeps that value in the policy draft |
| --- |

##### Change a template after entering values

| Given|I entered one or more values for the selected template |
| --- |

| When|I select another template |
| --- |

| Then|the UI asks me to confirm that those values will be discarded |
| --- |

| And|confirmation clears the old values and loads the new fields |
| --- |

| And|cancellation keeps the existing template and values |
| --- |

##### Template details cannot be retrieved

| Given|I selected a template |
| --- |

| When|retrieval of its details fails |
| --- |

| Then|the UI shows an error and offers a retry |
| --- |

| And|it does not display a form from another template |
| --- |

| And|it does not add the policy until the correct details are available |
| --- |

#### Technical details

Usage-policy forms do not display the catalogue/negotiation/transfer selector used by access policies. Any future monitoring metadata must follow the HLF4 contract.

This story covers field rendering and value capture. SIMPL-31454 (internal page) defines validation on policy confirmation. The save/add story defines copying the ODRL policy, filling declared slots and adding the completed policy to wizard state. The UI must support the templates returned by the API without hardcoding a preset count.

Policy confirmation and final submission: SIMPL-31579 (internal page).

#### Additional notes

- API definition ticket: (internal page)
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
