# SIMPL-395: Provide UI for additional vocabulary
- Status: DevTeam PBI is ready for Implementation check
- Type: Story
- Created: 2024-05-24, updated: 2026-05-19
- Source: https://jira.simplprogramme.eu/browse/SIMPL-395 (copied 2026-10-27)

## Description and acceptance criteria (as shown on the Jira issue page)

Status: Grooming / Ovi will add further Subtasks + AC and will be discussed in the next Grooming

As a governance authority agent

I want to include additional vocabularies if needed

So that I can use the vocabulary for semantic validation and to check search parameter.

 

----------------------

For the first iteration the UI should wrap the vocabulary calls of the catalogue to the user.

The UI should show a list of currently uploaded vocabularies/ontologies and schema/SHACL Constraints in the catalogue.

For each entry in the list the user can either remove the vocabulary from the catalogue or download the vocabulary/schema to check the content (returned as. ttl-file)

The UI should allow to upload new schema/vocabulary to the catalogue (files are ttl).

 

The UI should not be an extension of the existing search UI. The reason is that this needs to deploy later in different nodes. But it makes sense to use common components to build this UI.

a generic vocabulary Hub to upload vocabulary definitions in OWL like IDSA Vocabulary Hub and the used vocabulary provider vocob
