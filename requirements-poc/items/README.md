> **Simpl-Open requirements, proof of concept.** The folder of all items. New here? [Start here](../README.md) · [What the levels mean](../levels.md) · [All items](../all-items.md) · [By business process](../processes.md)

# Items by folder

The folders follow the requirement levels: business process › L0 › L1 › L2 › Epic › user story. Each item is a folder named after its key, with its page as the `README.md` inside. A child item is a folder inside its parent.

An item sits as deep as its recorded parents go. An item without a recorded parent sits directly in its business process folder, or in `no-business-process/`. Its page then carries a **⚠ Trace gap** tag.

- [BP02A/](BP02A/README.md): BP02A - Configure ID/Trust security solution
- [SA09/](SA09/README.md): SA09 - Data Provider manages consent assertions
- [SA01/](SA01/README.md): SA01 - Data orchestration used by a Participant
- [no-business-process/](no-business-process/README.md): ⚠ No business process

```
BP02A/
  SIMPL-18783/  L0 · BP02A - Configure ID/Trust security solution
    SIMPL-10171/  L1 · Configure Onboarding Procedure Templates
SA09/
  SIMPL-26074/  L0 · SA09 - Data Provider manages consent assertions
SA01/
  SIMPL-30560/  L1 · Participant executes workflow on saved distribution configuration
no-business-process/
  SIMPL-30641/  User story · A Provider removes contract clauses from a resource description via UI, resulting in a new version of the resource description
  SIMPL-31424/  L1 · Governance Authority maintains a contact email for each participant
    SIMPL-31425/  L2 · Governance authority registers participants' contact email
      SIMPL-31426/  Epic · Applicant provides participant contact email during onboarding
  SIMPL-31429/  Epic · Participant updates contact email after onboarding
  SIMPL-31853/  Epic · A Provider triggers the decommissioning of an infrastructure resource when revocation request is accepted
```
