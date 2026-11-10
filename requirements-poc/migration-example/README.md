> Part of the [Simpl-Open requirements proof of concept](../README.md).

# Migration example: BP05B

What one business process could look like in GitLab if its requirements move out of `business-processes` into their own repository. The example takes BP05B, the process with the largest tree here.

It has two folders, one per repository:

| Folder | Stands for | What is in it |
| --- | --- | --- |
| [business-processes/bp05b-provider-manages-resource-descriptions](business-processes/bp05b-provider-manages-resource-descriptions/README.md) | the existing `foundations/business-processes` repository | The BP05B page and its two dynamic views, taken from the `develop` branch on 10 November 2026 (3bcdac9). |
| [functional-requirements/bp05b-provider-manages-resource-descriptions](functional-requirements/bp05b-provider-manages-resource-descriptions/README.md) | a new `foundations/functional-requirements` repository | The BP05B requirements from this proof of concept: L0, 9 high-level requirements, 32 detailed requirements, Epics and user stories. |

## What changed in business-processes

The process page is the page from GitLab, with its overview, actors, diagrams and steps. Nothing is removed from it except the copies of the requirements. What is added is marked with 📋.

- **Under each process step.** A box lists the detailed requirements that the step needs, with their status and the number of Epics and user stories under them. Each one opens its page in functional-requirements. Example: step [BP05B.05 Validate the resource description](business-processes/bp05b-provider-manages-resource-descriptions/README.md#bp05b05-validate-the-resource-description) needs six detailed requirements of 5B.5.
- **High-level requirements.** The same table as before, with the same IDs (5B.1 to 5B.9) and the same links to the requirements site. The title now opens the requirement page. New columns show the status, the process steps and the dynamic view sections.
- **Detailed requirements.** The nine pages `bp5b1-...md` to `bp5b9-...md` are gone. In their place, one fold-out per high-level requirement lists its detailed requirements, with status and counts.
- **Dynamic views.** Each section of the [dynamic view](business-processes/bp05b-provider-manages-resource-descriptions/bp05b-dynamic-view.md) and the [TCV dynamic view](business-processes/bp05b-provider-manages-resource-descriptions/bp05b-tcv-dynamic-view.md) now starts with a line listing the high-level requirements it shows. The text and the diagrams are unchanged.
- **Other links.** Links to other process folders point to GitLab, because those folders are not in this example.

## What changed in functional-requirements

- **Folder page.** One section per high-level requirement, in the order 5B.1 to 5B.9: its status, process steps and dynamic view, a table of its detailed requirements, and a fold-out with the Epics and user stories. The page links to the process page instead of repeating it.
- **Links back.** Each high-level requirement page shows its ID on the requirements site, its process steps and its dynamic view sections, with links. Each detailed requirement page shows its ID on the requirements site, for example 5B.5.7. All 32 detailed requirements on the site were found in the tree; 12 of them were matched by hand, because the site uses short titles for 5B.5.

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

- **Process steps.** The link between a detailed requirement and a process step (for example 5B.2.3 to BP05B.02) is our reading. 5B.1 (consult own descriptions) has no step of its own; it is placed under the trigger. The items have no field for it. If it becomes a field, the generator can fill these links.
- **Dynamic view sections.** The same: the link from a requirement to a section of the dynamic view is made by hand here. A field or a label on the item would make it repeatable.
- **Requirements site numbers.** The IDs 5B.1 and 5B.2.1 exist only on the site. The generator needs them from the site, or as a field on the item.
- **Which text and which status.** The site shows every requirement as Proposed. The items show Defined or In Progress. The final publication must choose one.
