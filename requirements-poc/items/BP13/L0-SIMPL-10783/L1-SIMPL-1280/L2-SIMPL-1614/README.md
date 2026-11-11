> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../README.md) · [What the levels mean](../../../../../levels.md) · [All items](../../../../../all-items.md) · [By business process](../../../../../processes.md)

Folder: [items](../../../../README.md) / [BP13](../../../README.md) / [L0-SIMPL-10783](../../README.md) / [L1-SIMPL-1280](../README.md) / **L2-SIMPL-1614**

# SIMPL-1614: Controlling communication between participants

**L2 requirement (detailed requirement)**

**Trace:** [BP13](../../../README.md) › [SIMPL-10783](../../README.md) (L0) › [SIMPL-1280](../README.md) (L1) › **SIMPL-1614** (L2)

## Text

SIMPL shall provide a service that will be in charge of controlling every communication between participants. It will fulfil this by:

- encrypting, using mTLS approach, any communication between participants.
- enabling connection from/to data space participant only if mTLS authentication with ephemeral proof exchange is performed.
- allowing the exclusion of mTLS enforcement for specific resources (e.g. web API endpoints), making the resources available publicly (not only to data space agents) via standard TLS protocol.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-1280](../README.md) (L1) Encryption mechanisms - data in motion |
| **Children** | [SIMPL-2048](Epic-SIMPL-2048/README.md) (Epic) Communication agent to agent(tier 2)- exceptions in mTLS |
