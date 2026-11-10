> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14728](../../../README.md) / [L2-SIMPL-14741](../../README.md) / [Epic-SIMPL-18030](../README.md) / **Story-SIMPL-31575**

# SIMPL-31575: [UI] Provider sees the usage policies carried over from the previous resource description version

**User story (implementable work)** · Status **PBI INTERNAL QA**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14728](../../../README.md) (L1) › [SIMPL-14741](../../README.md) (L2) › [SIMPL-18030](../README.md) (Epic) › **SIMPL-31575** (User story)

## Text

#### Storycard

**As a** Provider authorised to manage resource descriptions,\
**I want to** see the usage policies already assigned to a resource description when I prepare a new RD version,\
**So that** I can review and retain them or make changes before submitting.

#### Acceptance criteria

##### Show the existing usage policies

| Given|I am creating a new version of an existing resource description |
| --- |

| And|the existing resource description has been loaded |
| --- |

| When|I reach the Usage policies step |
| --- |

| Then|its assigned usage policies are shown with their saved values |
| --- |

| And|policies supported by the available templates can be opened for editing |
| --- |

| And|I can keep or remove the listed policies |
| --- |

##### No usage policies are assigned

| Given|I am creating a new resource-description version |
| --- |

| And|the existing resource description has no usage policies |
| --- |

| When|I reach the Usage policies step |
| --- |

| Then|the UI shows an empty list |
| --- |

| And|I can add a usage policy if a template is available |
| --- |

| And|I can continue without adding a usage policy |
| --- |

##### A saved policy cannot be reopened for editing

| Given|the existing resource description contains a usage policy |
| --- |

| When|the form needed to edit that policy cannot be opened |
| --- |

| Then|the policy remains in the resource description with its saved values |
| --- |

| And|I can view it as read-only information |
| --- |

| And|I can keep it or remove it and add a replacement from an available template |
| --- |

| And|I can continue preparing the resource description |
| --- |

##### The existing resource description cannot be loaded

| Given|I want to create a new version of an existing resource description |
| --- |

| When|the existing resource description cannot be retrieved |
| --- |

| Then|the UI shows an error and offers a retry |
| --- |

| And|it does not open an empty new-version wizard in its place |
| --- |

#### Technical details

- Read the assigned usage policies from the existing RD content and copy their saved values into wizard state. Display editable policies through the same controls used for policies added in the wizard.
- Resolve an available template only to restore the policy's form. Open it for editing when its fixed terms and field structure are compatible with the saved policy.
- If the template is missing, unavailable or incompatible, preserve the completed policy as read-only. Do not replace saved values with current template defaults.
- Retention applies to the saved policy document. To replace a retained document with template-based policies, remove it first and then add the replacement policies.
- Loading an editing form and loading the RD are separate operations. Failure to load a form does not discard the RD or its policy content.
- Keep the saved policies in wizard state. Final submission and policy validation use the existing RD submission flow and the related stories.
- This story covers showing the existing usage policies and their editing availability. Selection, field entry, removal and submission are covered by SIMPL-31576, SIMPL-31577, SIMPL-31578 and SIMPL-31579.

#### Additional notes

- API definition ticket: (internal page)
- (internal page)
- (internal page)

- Shared policy-template API definition, SIMPL-31345 (internal page)
- R08 technical description (internal page)

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-18030](../README.md) (Epic) A Provider updates the usage policies of a new version of a resource description by using policy template via UI |
