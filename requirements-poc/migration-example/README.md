> Part of the [Simpl-Open requirements proof of concept](../README.md).

# Migration example: BP05B

What one business process could look like in GitLab if its requirements move out of `business-processes` into their own repository. The example takes BP05B, the process with the largest tree here.

It has two folders, one per repository:

| Folder | Stands for | What is in it |
| --- | --- | --- |
| [business-processes/bp05b-provider-manages-resource-descriptions](business-processes/bp05b-provider-manages-resource-descriptions/README.md) | the existing `foundations/business-processes` repository | The BP05B page and its two dynamic views, taken from the `develop` branch on 10 November 2026 (3bcdac9). |
| [functional-requirements/bp05b-provider-manages-resource-descriptions](functional-requirements/bp05b-provider-manages-resource-descriptions/README.md) | a new `foundations/functional-requirements` repository | The BP05B requirements from this proof of concept: L0, 9 high-level requirements, 32 detailed requirements, Epics and user stories. |

## What changed in business-processes

- **High-level requirements.** The section keeps one row per requirement, with the ID from the requirements site (5B.1 to 5B.9). The title links to the requirement page in functional-requirements. New columns show the status, the number of items under it, the process steps and the dynamic view sections.
- **Detailed requirements removed.** The list of detailed requirements and the nine pages `bp5b1-...md` to `bp5b9-...md` are gone. Their content is in the functional-requirements tree, one page per item.
- **Dynamic views.** Each section of the [dynamic view](business-processes/bp05b-provider-manages-resource-descriptions/bp05b-dynamic-view.md) and the [TCV dynamic view](business-processes/bp05b-provider-manages-resource-descriptions/bp05b-tcv-dynamic-view.md) now starts with a line listing the high-level requirements it shows. The text and the diagrams are unchanged.
- **Other links.** Links to other process folders point to GitLab, because those folders are not in this example.

Everything else on the process page is unchanged.

## What changed in functional-requirements

- **Links back.** Each high-level requirement page shows its ID on the requirements site, its process steps and its dynamic view sections, with links. Each detailed requirement page shows its ID on the requirements site, for example 5B.5.7. All 32 detailed requirements on the site were found in the tree; 12 of them were matched by hand, because the site uses short titles for 5B.5.
- **Folder page.** The BP05B folder page links to the process page instead of repeating it.

## Links to the dynamic views

| Dynamic view section | High-level requirements |
| --- | --- |
| Define and publish self-description | 5B.2, 5B.3, 5B.4, 5B.5 |
| Retrieve SD metadata | 5B.1 |
| Retrieve full self-description | 5B.1 |
| Revoke a self-description | 5B.7, 5B.8 |
| TCV dynamic view (publication flow, schema synchronisation) | 5B.2, 5B.4, 5B.5 |

5B.6 and 5B.9 (inform Consumers about a new version or a revocation) have no dynamic view yet. 

## Open points

- **Process steps.** The link between a requirement and the process steps (for example 5B.2 to BP05B.01, .02 and .03) is our reading. The items have no field for it. If it becomes a field, the generator can fill these links.
- **Dynamic view sections.** The same: the link from a requirement to a section of the dynamic view is made by hand here. A field or a label on the item would make it repeatable.
- **Requirements site numbers.** The IDs 5B.1 and 5B.2.1 exist only on the site. The generator needs them from the site, or as a field on the item.
- **Which text and which status.** The site shows every requirement as Proposed. The items show Defined or In Progress. The final publication must choose one.
