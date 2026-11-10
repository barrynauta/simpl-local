> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../README.md) · [What the levels mean](../../../../../levels.md) · [All items](../../../../../all-items.md) · [By business process](../../../../../processes.md)

Folder: [items](../../../../README.md) / [BP13](../../../README.md) / [L0-SIMPL-10783](../../README.md) / [L1-SIMPL-1277](../README.md) / **L2-SIMPL-1616**

# SIMPL-1616: Authentication between participant agents

**L2 requirement (detailed requirement)** · Status **In Progress**

**Trace:** [BP13](../../../README.md) › [SIMPL-10783](../../README.md) (L0) › [SIMPL-1277](../README.md) (L1) › **SIMPL-1616** (L2)

## Text

Simpl shall provide an API that allows any data space participant agent component to interact with other agent components, establishing mTLS connections with the agent credentials.\
Simpl shall provide an HTTP proxy to any data space participant agent that will enforce mTLS outbound connections, made by any component, with the agent credentials (coming from onboarding).\
Simpl shall provide a reverse proxy to any data space participant agent that will handle the incoming mTLS connection from any agent.\
Simpl shall provide an API that:

- shall verify if any request from any data space participant agent component can be accepted or rejected.
- shall enforce the presence of the requestor's authentication, incorporating it into the forwarded request.
- shall verify the credentials of the data space participant agent that the data space participant agent component what to communicate with.
- shall block the communication if the credentials of the data space participant agent that the data space participant agent component is not valid (e.g. expired validity, revoked credentials, ...).
- shall verify if the credentials provided in the intra-agent communication are trusted by the data space governance authority identity provider federation.
- shall accept only trusted (by the data space governance authority identity provider federation) mTLS connections and reject the others.
- shall verify the credentials of the calling participant compliant with the selected IAA Tier 2 option.
- shall accept only requests that have validated identity attributes attached to the credentials of the calling participant.
- shall verify that the credentials are trusted by the data space governance authority identity provider federation.
- shall enforce deployed agent policies based on the attached attributes attached the credentials of the calling participant, if any.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-1277](../README.md) (L1) Authentication process - IAA |
| **Children** | [SIMPL-461](Epic-SIMPL-461/README.md) (Epic) Secure communication between agents - reverse proxy<br>[SIMPL-2049](Epic-SIMPL-2049/README.md) (Epic) Secure interaction between agents<br>[SIMPL-2054](Epic-SIMPL-2054/README.md) (Epic) Verify agent credentials |
