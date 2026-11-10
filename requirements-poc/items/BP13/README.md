> **Simpl-Open requirements, proof of concept.** One business process and the items under it. New here? [Start here](../../README.md) · [What the levels mean](../../levels.md) · [All items](../../all-items.md) · [By business process](../../processes.md)

Folder: [items](../README.md) / **BP13**

# BP13 - IT Administration

BP13 groups the IT administration of a Simpl-Open agent. These are the technical tasks that make an agent run safely in a data space: the network and VPN settings, firewall integration, interoperability with the infrastructure underneath, business continuity and disaster recovery, encryption of data in transit, authentication, setting up and starting the agent, and the landing page that participants use to reach their services.

BP13 is not a sequence of steps like the other processes. Each high-level requirement stands on its own. The public process page only lists these requirements: it has no overview, actors or steps.

This folder holds more high-level requirements than the public page, because it also lists the items that are not published, for example the participant contact email and the public profiles.

Read more on the [public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) and the [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration).

## Who takes part

- Participants and the Governance Authority, as IT administrators of their own agent. The public page names no actors.

## What this folder holds

The L0 item [SIMPL-10783](L0-SIMPL-10783/README.md) stands for the business process. Under it are 18 high-level requirements (L1). Each has its detailed requirements (L2), the Epics that deliver them and the user stories that build them. In total **159 items:** 1 L0, 18 L1, 39 L2, 35 Epic, 66 User story.

| L1 | High-level requirement | Status | Items under it |
| --- | --- | --- | --- |
| [SIMPL-1273](L0-SIMPL-10783/L1-SIMPL-1273/README.md) | Access control policies - agent to agent *(not published)* | In Progress | 16 |
| [SIMPL-1275](L0-SIMPL-10783/L1-SIMPL-1275/README.md) | Agent setup and initialisation | Defined | 11 |
| [SIMPL-1277](L0-SIMPL-10783/L1-SIMPL-1277/README.md) | Authentication process - IAA | In Progress | 17 |
| [SIMPL-1278](L0-SIMPL-10783/L1-SIMPL-1278/README.md) | Business continuity and disaster recovery *(not published)* | Blocked | 5 |
| [SIMPL-1279](L0-SIMPL-10783/L1-SIMPL-1279/README.md) | Encryption mechanisms - data at rest *(not published)* | Blocked | 1 |
| [SIMPL-1280](L0-SIMPL-10783/L1-SIMPL-1280/README.md) | Encryption mechanisms - data in motion | In Progress | 7 |
| [SIMPL-1283](L0-SIMPL-10783/L1-SIMPL-1283/README.md) | Inheritance mechanisms *(not published)* | Blocked | 1 |
| [SIMPL-1294](L0-SIMPL-10783/L1-SIMPL-1294/README.md) | SIMPL Agent standard Cross Cutting Concern to handle cross capabilities/agents communication *(not published)* | Blocked | 5 |
| [SIMPL-1295](L0-SIMPL-10783/L1-SIMPL-1295/README.md) | SIMPL Agent standard Cross Cutting Concern to handle cross capabilities/agents communication - Configurability *(not published)* | Blocked | 5 |
| [SIMPL-1301](L0-SIMPL-10783/L1-SIMPL-1301/README.md) | Business continuity and disaster recovery | Defined | 2 |
| [SIMPL-1376](L0-SIMPL-10783/L1-SIMPL-1376/README.md) | Infrastructure interoperability | Defined | 10 |
| [SIMPL-1378](L0-SIMPL-10783/L1-SIMPL-1378/README.md) | Additional Network and Security Services - Firewall integration - software | Defined | 2 |
| [SIMPL-1379](L0-SIMPL-10783/L1-SIMPL-1379/README.md) | A Participants set the VPN configuration for secure data transfer and secure access to Simpl-Open resources | Defined | 3 |
| [SIMPL-1386](L0-SIMPL-10783/L1-SIMPL-1386/README.md) | Using infrastructure from a provider - IAM | Defined | 1 |
| [SIMPL-13574](L0-SIMPL-10783/L1-SIMPL-13574/README.md) | A Participant uses the Simpl-Open landing page | In Progress | 8 |
| [SIMPL-23891](L0-SIMPL-10783/L1-SIMPL-23891/README.md) | Governance Authority publishes its public profile *(not published)* | In Progress | 23 |
| [SIMPL-31424](L0-SIMPL-10783/L1-SIMPL-31424/README.md) | Governance Authority maintains a contact email for each participant | In Progress | 19 |
| [SIMPL-32447](L0-SIMPL-10783/L1-SIMPL-32447/README.md) | Participant publishes its public profile *(not published)* | In Progress | 4 |

## Tree

Every item, with its level, title and status.

- [SIMPL-10783](L0-SIMPL-10783/README.md) L0 · BP13 - IT Administration · New
  - [SIMPL-1273](L0-SIMPL-10783/L1-SIMPL-1273/README.md) L1 · Access control policies - agent to agent · In Progress · *not published*
    - [SIMPL-1615](L0-SIMPL-10783/L1-SIMPL-1273/L2-SIMPL-1615/README.md) L2 · Ensure ABAC compliance · In Progress · *not published*
      - [SIMPL-463](L0-SIMPL-10783/L1-SIMPL-1273/L2-SIMPL-1615/Epic-SIMPL-463/README.md) Epic · Participant enforces ABAC policies · Defined · *not published*
        - [SIMPL-484](L0-SIMPL-10783/L1-SIMPL-1273/L2-SIMPL-1615/Epic-SIMPL-463/Story-SIMPL-484/README.md) User story · Provide Ephemeral Proof with latest Identity Attributes · Ready for DG Acceptance · *not published*
        - [SIMPL-855](L0-SIMPL-10783/L1-SIMPL-1273/L2-SIMPL-1615/Epic-SIMPL-463/Story-SIMPL-855/README.md) User story · Request Ephemeral Proof for accessing other participant agent functionality · DevTeam PBI is ready for Implementation check · *not published*
        - [SIMPL-885](L0-SIMPL-10783/L1-SIMPL-1273/L2-SIMPL-1615/Epic-SIMPL-463/Story-SIMPL-885/README.md) User story · Sending Ephemeral Proof to peer dataspace participant · DevTeam PBI is ready for Implementation check · *not published*
        - [SIMPL-886](L0-SIMPL-10783/L1-SIMPL-1273/L2-SIMPL-1615/Epic-SIMPL-463/Story-SIMPL-886/README.md) User story · API for receiving Ephemeral Proof from peer dataspace participant · DevTeam PBI is ready for Implementation check · *not published*
        - [SIMPL-887](L0-SIMPL-10783/L1-SIMPL-1273/L2-SIMPL-1615/Epic-SIMPL-463/Story-SIMPL-887/README.md) User story · Ensuring ABAC compliance · DevTeam PBI is ready for Implementation check · *not published*
        - [SIMPL-4501](L0-SIMPL-10783/L1-SIMPL-1273/L2-SIMPL-1615/Epic-SIMPL-463/Story-SIMPL-4501/README.md) User story · [API] [IMPROVEMENT] Adding Tier1 public key into the Ephemeral Proof · Ready for DG Acceptance · *not published*
        - [SIMPL-17492](L0-SIMPL-10783/L1-SIMPL-1273/L2-SIMPL-1615/Epic-SIMPL-463/Story-SIMPL-17492/README.md) User story · [API] Participant agent retrieves identity attributes of a participant · Ready for DG Acceptance · *not published*
        - [SIMPL-17529](L0-SIMPL-10783/L1-SIMPL-1273/L2-SIMPL-1615/Epic-SIMPL-463/Story-SIMPL-17529/README.md) User story · [API] Dataspace Participant agent retrieves current agent participant identity attributes · Ready for DG Acceptance · *not published*
        - [SIMPL-17530](L0-SIMPL-10783/L1-SIMPL-1273/L2-SIMPL-1615/Epic-SIMPL-463/Story-SIMPL-17530/README.md) User story · [API] Dataspace Participant agent retrieves identity attributes of a participant · Ready for DG Acceptance · *not published*
        - [SIMPL-17636](L0-SIMPL-10783/L1-SIMPL-1273/L2-SIMPL-1615/Epic-SIMPL-463/Story-SIMPL-17636/README.md) User story · [API] Dataspace Participant agent requests an ephemeral proof · Ready for DG Acceptance · *not published*
      - [SIMPL-32434](L0-SIMPL-10783/L1-SIMPL-1273/L2-SIMPL-1615/Epic-SIMPL-32434/README.md) Epic · Participant enforces ABAC policies (Extension) · In Progress · *not published*
        - [SIMPL-33929](L0-SIMPL-10783/L1-SIMPL-1273/L2-SIMPL-1615/Epic-SIMPL-32434/Story-SIMPL-33929/README.md) User story · [API] Participant checks if Tier2 session with another participant is present (non-mtls OPTIONS /tier2/v2/session?sessionId={sessionId}) · DevTeam PBI is ready for Implementation check · *not published*
        - [SIMPL-33930](L0-SIMPL-10783/L1-SIMPL-1273/L2-SIMPL-1615/Epic-SIMPL-32434/Story-SIMPL-33930/README.md) User story · [API] Participant creates a Tier 2 session (POST /tier2/v2/session) · DevTeam PBI is ready for Implementation check · *not published*
        - [SIMPL-33931](L0-SIMPL-10783/L1-SIMPL-1273/L2-SIMPL-1615/Epic-SIMPL-32434/Story-SIMPL-33931/README.md) User story · [API] Participant returns active Tier2 session (GET /tier1/v2/session/{sessionId}) · DevTeam PBI is ready for Implementation check · *not published*
  - [SIMPL-1275](L0-SIMPL-10783/L1-SIMPL-1275/README.md) L1 · Agent setup and initialisation · Defined
    - [SIMPL-1608](L0-SIMPL-10783/L1-SIMPL-1275/L2-SIMPL-1608/README.md) L2 · Agent initialisation · Defined
    - [SIMPL-1609](L0-SIMPL-10783/L1-SIMPL-1275/L2-SIMPL-1609/README.md) L2 · Loading preconfigured roles · New · *not published*
    - [SIMPL-1611](L0-SIMPL-10783/L1-SIMPL-1275/L2-SIMPL-1611/README.md) L2 · 2 Tier IAA (Identification, Authentication, Authorisation) · New · *not published*
    - [SIMPL-1613](L0-SIMPL-10783/L1-SIMPL-1275/L2-SIMPL-1613/README.md) L2 · Tier 2 attributes management - services · Defined
      - [SIMPL-466](L0-SIMPL-10783/L1-SIMPL-1275/L2-SIMPL-1613/Epic-SIMPL-466/README.md) Epic · Monitoring service for identity attributes syncronization · Internal QA check
        - [SIMPL-723](L0-SIMPL-10783/L1-SIMPL-1275/L2-SIMPL-1613/Epic-SIMPL-466/Story-SIMPL-723/README.md) User story · Request Ephemeral Proof for Identity Attribute Synchronization · DevTeam PBI is ready for Implementation check
      - [SIMPL-467](L0-SIMPL-10783/L1-SIMPL-1275/L2-SIMPL-1613/Epic-SIMPL-467/README.md) Epic · Synchronisation process for local identity attribute list update · Internal QA check
        - [SIMPL-724](L0-SIMPL-10783/L1-SIMPL-1275/L2-SIMPL-1613/Epic-SIMPL-467/Story-SIMPL-724/README.md) User story · Identity Attribute Synchronization Procedure · Ready for DG Acceptance
        - [SIMPL-725](L0-SIMPL-10783/L1-SIMPL-1275/L2-SIMPL-1613/Epic-SIMPL-467/Story-SIMPL-725/README.md) User story · Identity Attribute Synchronization Procedure - attributes to local roles assignment update procedure · Ready for DG Acceptance
        - [SIMPL-19923](L0-SIMPL-10783/L1-SIMPL-1275/L2-SIMPL-1613/Epic-SIMPL-467/Story-SIMPL-19923/README.md) User story · [API] Participant triggers manual synchronisation of identity attributes · Ready for DG Acceptance
        - [SIMPL-20512](L0-SIMPL-10783/L1-SIMPL-1275/L2-SIMPL-1613/Epic-SIMPL-467/Story-SIMPL-20512/README.md) User story · [UI] Participant triggers manual synchronisation of identity attributes · Ready for DG Acceptance
  - [SIMPL-1277](L0-SIMPL-10783/L1-SIMPL-1277/README.md) L1 · Authentication process - IAA · In Progress
    - [SIMPL-1616](L0-SIMPL-10783/L1-SIMPL-1277/L2-SIMPL-1616/README.md) L2 · Authentication between participant agents · In Progress
      - [SIMPL-461](L0-SIMPL-10783/L1-SIMPL-1277/L2-SIMPL-1616/Epic-SIMPL-461/README.md) Epic · Secure communication between agents - reverse proxy · Internal QA check
      - [SIMPL-2049](L0-SIMPL-10783/L1-SIMPL-1277/L2-SIMPL-1616/Epic-SIMPL-2049/README.md) Epic · Secure interaction between agents · In Preparation
        - [SIMPL-4013](L0-SIMPL-10783/L1-SIMPL-1277/L2-SIMPL-1616/Epic-SIMPL-2049/Story-SIMPL-4013/README.md) User story · Provide ping functionality to dataspace agents · Ready for DG Acceptance
        - [SIMPL-4026](L0-SIMPL-10783/L1-SIMPL-1277/L2-SIMPL-1616/Epic-SIMPL-2049/Story-SIMPL-4026/README.md) User story · Sending ping request to dataspace agents · Ready for DG Acceptance
        - [SIMPL-4625](L0-SIMPL-10783/L1-SIMPL-1277/L2-SIMPL-1616/Epic-SIMPL-2049/Story-SIMPL-4625/README.md) User story · [UI] Verify connectivity to other dataspace participants · Ready for DG Acceptance
        - [SIMPL-17648](L0-SIMPL-10783/L1-SIMPL-1277/L2-SIMPL-1616/Epic-SIMPL-2049/Story-SIMPL-17648/README.md) User story · [API] Participant user pings a remote participant agent · Ready for DG Acceptance
        - [SIMPL-17653](L0-SIMPL-10783/L1-SIMPL-1277/L2-SIMPL-1616/Epic-SIMPL-2049/Story-SIMPL-17653/README.md) User story · [API] Dataspace Participant agent pings a peer participant · Ready for DG Acceptance
        - [SIMPL-17954](L0-SIMPL-10783/L1-SIMPL-1277/L2-SIMPL-1616/Epic-SIMPL-2049/Story-SIMPL-17954/README.md) User story · [UI][REFACTOR] Verify connectivity to other dataspace participants · Ready for DG Acceptance
      - [SIMPL-2054](L0-SIMPL-10783/L1-SIMPL-1277/L2-SIMPL-1616/Epic-SIMPL-2054/README.md) Epic · Verify agent credentials · In Progress
        - [SIMPL-17495](L0-SIMPL-10783/L1-SIMPL-1277/L2-SIMPL-1616/Epic-SIMPL-2054/Story-SIMPL-17495/README.md) User story · [API] Participant agent retrieves participant details by credential · Ready for DG Acceptance
        - [SIMPL-17496](L0-SIMPL-10783/L1-SIMPL-1277/L2-SIMPL-1616/Epic-SIMPL-2054/Story-SIMPL-17496/README.md) User story · [API] Participant agent retrieves participant details by identifier · Ready for DG Acceptance
        - [SIMPL-17516](L0-SIMPL-10783/L1-SIMPL-1277/L2-SIMPL-1616/Epic-SIMPL-2054/Story-SIMPL-17516/README.md) User story · [API] Dataspace Participant agent retrieves participant details by credential · Ready for DG Acceptance
        - [SIMPL-17517](L0-SIMPL-10783/L1-SIMPL-1277/L2-SIMPL-1616/Epic-SIMPL-2054/Story-SIMPL-17517/README.md) User story · [API] Dataspace Participant agent retrieves participant details · Ready for DG Acceptance
        - [SIMPL-17645](L0-SIMPL-10783/L1-SIMPL-1277/L2-SIMPL-1616/Epic-SIMPL-2054/Story-SIMPL-17645/README.md) User story · [API] Dataspace Participant agent validates a credential · Ready for DG Acceptance
        - [SIMPL-17685](L0-SIMPL-10783/L1-SIMPL-1277/L2-SIMPL-1616/Epic-SIMPL-2054/Story-SIMPL-17685/README.md) User story · [API] Dataspace participant agent retrieves its participant profile · Ready for DG Acceptance
        - [SIMPL-18905](L0-SIMPL-10783/L1-SIMPL-1277/L2-SIMPL-1616/Epic-SIMPL-2054/Story-SIMPL-18905/README.md) User story · [API] Dataspace participant agent validates a Tier 1 credential · Ready for DG Acceptance
  - [SIMPL-1278](L0-SIMPL-10783/L1-SIMPL-1278/README.md) L1 · Business continuity and disaster recovery · Blocked · *not published*
    - [SIMPL-1638](L0-SIMPL-10783/L1-SIMPL-1278/L2-SIMPL-1638/README.md) L2 · Replication of identity provider federation · Blocked · *not published*
    - [SIMPL-1639](L0-SIMPL-10783/L1-SIMPL-1278/L2-SIMPL-1639/README.md) L2 · Replication of security attribute provider · Blocked · *not published*
    - [SIMPL-1640](L0-SIMPL-10783/L1-SIMPL-1278/L2-SIMPL-1640/README.md) L2 · Replication of authentication components · Blocked · *not published*
    - [SIMPL-1641](L0-SIMPL-10783/L1-SIMPL-1278/L2-SIMPL-1641/README.md) L2 · Replication of participants management components · Blocked · *not published*
    - [SIMPL-1642](L0-SIMPL-10783/L1-SIMPL-1278/L2-SIMPL-1642/README.md) L2 · Replication of SIMPL agent API gateway · Blocked · *not published*
  - [SIMPL-1279](L0-SIMPL-10783/L1-SIMPL-1279/README.md) L1 · Encryption mechanisms - data at rest · Blocked · *not published*
    - [SIMPL-1632](L0-SIMPL-10783/L1-SIMPL-1279/L2-SIMPL-1632/README.md) L2 · Onboarding data are secured · Blocked · *not published*
  - [SIMPL-1280](L0-SIMPL-10783/L1-SIMPL-1280/README.md) L1 · Encryption mechanisms - data in motion · In Progress
    - [SIMPL-1614](L0-SIMPL-10783/L1-SIMPL-1280/L2-SIMPL-1614/README.md) L2 · Controlling communication between participants · In Progress
      - [SIMPL-2046](L0-SIMPL-10783/L1-SIMPL-1280/L2-SIMPL-1614/Epic-SIMPL-2046/README.md) Epic · Communication agent to agent(tier 2) - secure data exchange · New
      - [SIMPL-2047](L0-SIMPL-10783/L1-SIMPL-1280/L2-SIMPL-1614/Epic-SIMPL-2047/README.md) Epic · Communication agent to agent(tier 2) - secure connection · In Preparation
      - [SIMPL-2048](L0-SIMPL-10783/L1-SIMPL-1280/L2-SIMPL-1614/Epic-SIMPL-2048/README.md) Epic · Communication agent to agent(tier 2)- exceptions in mTLS · In Progress
        - [SIMPL-2569](L0-SIMPL-10783/L1-SIMPL-1280/L2-SIMPL-1614/Epic-SIMPL-2048/Story-SIMPL-2569/README.md) User story · [UI] Tier 2 Gateway Resources Exclusions List · PBI Closed
        - [SIMPL-2572](L0-SIMPL-10783/L1-SIMPL-1280/L2-SIMPL-1614/Epic-SIMPL-2048/Story-SIMPL-2572/README.md) User story · [UI] Tier 2 Gateway Resources Exclusions List - filtering · PBI Closed
        - [SIMPL-2574](L0-SIMPL-10783/L1-SIMPL-1280/L2-SIMPL-1614/Epic-SIMPL-2048/Story-SIMPL-2574/README.md) User story · [UI] Tier 2 Gateway Resources Exclusions List - ordering · PBI Closed
  - [SIMPL-1283](L0-SIMPL-10783/L1-SIMPL-1283/README.md) L1 · Inheritance mechanisms · Blocked · *not published*
    - [SIMPL-1631](L0-SIMPL-10783/L1-SIMPL-1283/L2-SIMPL-1631/README.md) L2 · Inheritance mechanism on consent management component · Blocked · *not published*
  - [SIMPL-1294](L0-SIMPL-10783/L1-SIMPL-1294/README.md) L1 · SIMPL Agent standard Cross Cutting Concern to handle cross capabilities/agents communication · Blocked · *not published*
    - [SIMPL-1633](L0-SIMPL-10783/L1-SIMPL-1294/L2-SIMPL-1633/README.md) L2 · Load mitigation strategies on identity provider federation · Blocked · *not published*
    - [SIMPL-1634](L0-SIMPL-10783/L1-SIMPL-1294/L2-SIMPL-1634/README.md) L2 · Load mitigation strategies on security attribute provider federation · Blocked · *not published*
    - [SIMPL-1635](L0-SIMPL-10783/L1-SIMPL-1294/L2-SIMPL-1635/README.md) L2 · Load mitigation strategies on authentication components · Blocked · *not published*
    - [SIMPL-1636](L0-SIMPL-10783/L1-SIMPL-1294/L2-SIMPL-1636/README.md) L2 · Load mitigation strategies on SIMPL agent API gateway · Blocked · *not published*
    - [SIMPL-1637](L0-SIMPL-10783/L1-SIMPL-1294/L2-SIMPL-1637/README.md) L2 · Load mitigation strategies on participant management components · Blocked · *not published*
  - [SIMPL-1295](L0-SIMPL-10783/L1-SIMPL-1295/README.md) L1 · SIMPL Agent standard Cross Cutting Concern to handle cross capabilities/agents communication - Configurability · Blocked · *not published*
    - [SIMPL-1602](L0-SIMPL-10783/L1-SIMPL-1295/L2-SIMPL-1602/README.md) L2 · Description of identity provider federation deployability · Blocked · *not published*
    - [SIMPL-1603](L0-SIMPL-10783/L1-SIMPL-1295/L2-SIMPL-1603/README.md) L2 · Description of participant management API deployability · Blocked · *not published*
    - [SIMPL-1604](L0-SIMPL-10783/L1-SIMPL-1295/L2-SIMPL-1604/README.md) L2 · Description of SIMPL agent API deployability · Blocked · *not published*
    - [SIMPL-1605](L0-SIMPL-10783/L1-SIMPL-1295/L2-SIMPL-1605/README.md) L2 · Description of security attribute provider federation deployability · Blocked · *not published*
    - [SIMPL-1606](L0-SIMPL-10783/L1-SIMPL-1295/L2-SIMPL-1606/README.md) L2 · Description of authentication components deployability · Blocked · *not published*
  - [SIMPL-1301](L0-SIMPL-10783/L1-SIMPL-1301/README.md) L1 · Business continuity and disaster recovery · Defined
    - [SIMPL-10365](L0-SIMPL-10783/L1-SIMPL-1301/L2-SIMPL-10365/README.md) L2 · Automated self-healing mechanisms for monitoring · In Preparation · *not published*
    - [SIMPL-10366](L0-SIMPL-10783/L1-SIMPL-1301/L2-SIMPL-10366/README.md) L2 · Simpl-Open should have scheduled tests to validate Business Continuity · On Hold · *not published*
  - [SIMPL-1376](L0-SIMPL-10783/L1-SIMPL-1376/README.md) L1 · Infrastructure interoperability · Defined
    - [SIMPL-1627](L0-SIMPL-10783/L1-SIMPL-1376/L2-SIMPL-1627/README.md) L2 · Virtualisation ready · Defined
      - [SIMPL-2089](L0-SIMPL-10783/L1-SIMPL-1376/L2-SIMPL-1627/Epic-SIMPL-2089/README.md) Epic · Offer Virtualization-Ready Minimal Consumer Agent Image · For DG Connect Review
      - [SIMPL-2090](L0-SIMPL-10783/L1-SIMPL-1376/L2-SIMPL-1627/Epic-SIMPL-2090/README.md) Epic · VM Repository for Minimal Consumer Agent Image · In Preparation
      - [SIMPL-2091](L0-SIMPL-10783/L1-SIMPL-1376/L2-SIMPL-1627/Epic-SIMPL-2091/README.md) Epic · Select a Standard for the building and registry of the Agent image · For DG Connect Review
    - [SIMPL-1628](L0-SIMPL-10783/L1-SIMPL-1376/L2-SIMPL-1628/README.md) L2 · Containerisation ready · Defined
      - [SIMPL-2096](L0-SIMPL-10783/L1-SIMPL-1376/L2-SIMPL-1628/Epic-SIMPL-2096/README.md) Epic · Register Container Image to be offered for Consumer Agent · In Preparation
      - [SIMPL-2097](L0-SIMPL-10783/L1-SIMPL-1376/L2-SIMPL-1628/Epic-SIMPL-2097/README.md) Epic · Use Trusted Container Registry for Consumer Agent · In Preparation
      - [SIMPL-2098](L0-SIMPL-10783/L1-SIMPL-1376/L2-SIMPL-1628/Epic-SIMPL-2098/README.md) Epic · Ensure Standards Compliance for Container's Orchestration Platform · In Preparation
      - [SIMPL-2100](L0-SIMPL-10783/L1-SIMPL-1376/L2-SIMPL-1628/Epic-SIMPL-2100/README.md) Epic · Provide Installation Scripts for Consumer Agent · In Preparation
      - [SIMPL-17537](L0-SIMPL-10783/L1-SIMPL-1376/L2-SIMPL-1628/Epic-SIMPL-17537/README.md) Epic · Provide Deployment Scripts for Governance Authority Agent · For DG Connect Review
  - [SIMPL-1378](L0-SIMPL-10783/L1-SIMPL-1378/README.md) L1 · Additional Network and Security Services - Firewall integration - software · Defined
    - [SIMPL-1645](L0-SIMPL-10783/L1-SIMPL-1378/L2-SIMPL-1645/README.md) L2 · A Participant integrates their Simpl-Open agent with software firewall solutions · Defined
      - [SIMPL-2119](L0-SIMPL-10783/L1-SIMPL-1378/L2-SIMPL-1645/Epic-SIMPL-2119/README.md) Epic · A Participant integrates their Simpl-Open agent with software firewall solutions · Defined
  - [SIMPL-1379](L0-SIMPL-10783/L1-SIMPL-1379/README.md) L1 · A Participants set the VPN configuration for secure data transfer and secure access to Simpl-Open resources · Defined
    - [SIMPL-1648](L0-SIMPL-10783/L1-SIMPL-1379/L2-SIMPL-1648/README.md) L2 · A Participant sets the VPN configuration for data transfer between the Simpl-Open agent and data resource source or destination · Defined
      - [SIMPL-30019](L0-SIMPL-10783/L1-SIMPL-1379/L2-SIMPL-1648/Epic-SIMPL-30019/README.md) Epic · A Provider sets the VPN configuration for data transfer between the data resource source and their Simpl-Open agent · Defined
      - [SIMPL-30020](L0-SIMPL-10783/L1-SIMPL-1379/L2-SIMPL-1648/Epic-SIMPL-30020/README.md) Epic · A Consumer sets the VPN configuration for data transfer between their Simpl-Open agent and the data resource destination · Defined
  - [SIMPL-1386](L0-SIMPL-10783/L1-SIMPL-1386/README.md) L1 · Using infrastructure from a provider - IAM · Defined
    - [SIMPL-1670](L0-SIMPL-10783/L1-SIMPL-1386/L2-SIMPL-1670/README.md) L2 · Integration with Infrastructure Identity and Access Management (IAM) · In Preparation · *not published*
  - [SIMPL-13574](L0-SIMPL-10783/L1-SIMPL-13574/README.md) L1 · A Participant uses the Simpl-Open landing page · In Progress
    - [SIMPL-12044](L0-SIMPL-10783/L1-SIMPL-13574/L2-SIMPL-12044/README.md) L2 · A Participant uses the Simpl-Open landing page adapted to their Participant type · In Progress
      - [SIMPL-12046](L0-SIMPL-10783/L1-SIMPL-13574/L2-SIMPL-12044/Epic-SIMPL-12046/README.md) Epic · A Participant uses the Simpl-Open landing page adapted to their Participant type · Ready for validation and verification
        - [SIMPL-27801](L0-SIMPL-10783/L1-SIMPL-13574/L2-SIMPL-12044/Epic-SIMPL-12046/Story-SIMPL-27801/README.md) User story · A Participant displays functionalities aligned with their Participant type on the landing page UI · DevTeam PBI is ready for Implementation check
        - [SIMPL-27802](L0-SIMPL-10783/L1-SIMPL-13574/L2-SIMPL-12044/Epic-SIMPL-12046/Story-SIMPL-27802/README.md) User story · Configure landing page tiles via UI · Ready for DG Acceptance
      - [SIMPL-28280](L0-SIMPL-10783/L1-SIMPL-13574/L2-SIMPL-12044/Epic-SIMPL-28280/README.md) Epic · A Participant uses the Simpl-Open landing page adapted to their user role [Improvement] · In Progress
        - [SIMPL-28207](L0-SIMPL-10783/L1-SIMPL-13574/L2-SIMPL-12044/Epic-SIMPL-28280/Story-SIMPL-28207/README.md) User story · A Participant displays the functionalities aligned with their user role on the landing page UI · PBI INTERNAL QA
        - [SIMPL-32348](L0-SIMPL-10783/L1-SIMPL-13574/L2-SIMPL-12044/Epic-SIMPL-28280/Story-SIMPL-32348/README.md) User story · A Participant uses the landing page to access the ticketing system · PBI INTERNAL QA
      - [SIMPL-29224](L0-SIMPL-10783/L1-SIMPL-13574/L2-SIMPL-12044/Epic-SIMPL-29224/README.md) Epic · A Participant uses the Simpl-Open landing page adapted to their user role: Contract & Audit UI [Improvement] · For DG Connect Review
  - [SIMPL-23891](L0-SIMPL-10783/L1-SIMPL-23891/README.md) L1 · Governance Authority publishes its public profile · In Progress · *not published*
    - [SIMPL-23890](L0-SIMPL-10783/L1-SIMPL-23891/L2-SIMPL-23890/README.md) L2 · Governance Authority exposes public identity attributes in its public profile · Defined · *not published*
      - [SIMPL-24737](L0-SIMPL-10783/L1-SIMPL-23891/L2-SIMPL-23890/Epic-SIMPL-24737/README.md) Epic · Governance Authority publishes identity attributes through its public discovery profile · Defined · *not published*
        - [SIMPL-25117](L0-SIMPL-10783/L1-SIMPL-23891/L2-SIMPL-23890/Epic-SIMPL-24737/Story-SIMPL-25117/README.md) User story · [API] Governance authority publishes identity attribute via public endpoint · Ready for DG Acceptance · *not published*
    - [SIMPL-23892](L0-SIMPL-10783/L1-SIMPL-23891/L2-SIMPL-23892/README.md) L2 · Governance Authority exposes public identity credentials in its public profile · Defined · *not published*
      - [SIMPL-24738](L0-SIMPL-10783/L1-SIMPL-23891/L2-SIMPL-23892/Epic-SIMPL-24738/README.md) Epic · Governance Authority publishes identity credentials in its public discovery profile · Defined · *not published*
        - [SIMPL-25107](L0-SIMPL-10783/L1-SIMPL-23891/L2-SIMPL-23892/Epic-SIMPL-24738/Story-SIMPL-25107/README.md) User story · [API] Governance authority publishes its credential details in the form of a DID document via public endpoint · Ready for DG Acceptance · *not published*
        - [SIMPL-25108](L0-SIMPL-10783/L1-SIMPL-23891/L2-SIMPL-23892/Epic-SIMPL-24738/Story-SIMPL-25108/README.md) User story · [API] Governance authority publishes its credentials as a certificate chain in pem format via public endpoint · Ready for DG Acceptance · *not published*
      - [SIMPL-31748](L0-SIMPL-10783/L1-SIMPL-23891/L2-SIMPL-23892/Epic-SIMPL-31748/README.md) Epic · Governance Authority publishes identity credentials in its public discovery profile (Extension) · Defined · *not published*
        - [SIMPL-31653](L0-SIMPL-10783/L1-SIMPL-23891/L2-SIMPL-23892/Epic-SIMPL-31748/Story-SIMPL-31653/README.md) User story · [IMPROVEMENT][API] Governance authority publishes in the DID document multiple verification methods · DevTeam PBI is ready for Implementation check · *not published*
    - [SIMPL-24798](L0-SIMPL-10783/L1-SIMPL-23891/L2-SIMPL-24798/README.md) L2 · Governance Authority exposes general dataspace information in its public profile · In Progress · *not published*
      - [SIMPL-24951](L0-SIMPL-10783/L1-SIMPL-23891/L2-SIMPL-24798/Epic-SIMPL-24951/README.md) Epic · Governance Authority publishes dataspace information in its public discovery endpoint · Defined · *not published*
        - [SIMPL-25032](L0-SIMPL-10783/L1-SIMPL-23891/L2-SIMPL-24798/Epic-SIMPL-24951/Story-SIMPL-25032/README.md) User story · Governance authority configures dataspace name and contact email · Ready for DG Acceptance · *not published*
        - [SIMPL-25106](L0-SIMPL-10783/L1-SIMPL-23891/L2-SIMPL-24798/Epic-SIMPL-24951/Story-SIMPL-25106/README.md) User story · [API] Governance Authority shares public dataspace information · Ready for DG Acceptance · *not published*
        - [SIMPL-25136](L0-SIMPL-10783/L1-SIMPL-23891/L2-SIMPL-24798/Epic-SIMPL-24951/Story-SIMPL-25136/README.md) User story · [API] Governance authority manages public dataspace information · Ready for DG Acceptance · *not published*
        - [SIMPL-25137](L0-SIMPL-10783/L1-SIMPL-23891/L2-SIMPL-24798/Epic-SIMPL-24951/Story-SIMPL-25137/README.md) User story · [UI] Governance Authority manages public dataspace information · Ready for DG Acceptance · *not published*
        - [SIMPL-28285](L0-SIMPL-10783/L1-SIMPL-23891/L2-SIMPL-24798/Epic-SIMPL-24951/Story-SIMPL-28285/README.md) User story · [UI][IMPROVEMENT] Governance Authority warned when dataspace name and contact email are not filled · DevTeam PBI is ready for Implementation check · *not published*
      - [SIMPL-32446](L0-SIMPL-10783/L1-SIMPL-23891/L2-SIMPL-24798/Epic-SIMPL-32446/README.md) Epic · Governance Authority publishes dataspace information in its public discovery profile (Extension) · In Progress · *not published*
        - [SIMPL-33852](L0-SIMPL-10783/L1-SIMPL-23891/L2-SIMPL-24798/Epic-SIMPL-32446/Story-SIMPL-33852/README.md) User story · [API] Governance Authority shares public dataspace information using DataspaceCredential · DevTeam PBI is ready for Implementation check · *not published*
    - [SIMPL-24969](L0-SIMPL-10783/L1-SIMPL-23891/L2-SIMPL-24969/README.md) L2 · Governance Authority exposes its agent participant information in its public profile · In Progress · *not published*
      - [SIMPL-24970](L0-SIMPL-10783/L1-SIMPL-23891/L2-SIMPL-24969/Epic-SIMPL-24970/README.md) Epic · Governance Authority exposes agent participant information through its public discovery profile · Defined · *not published*
        - [SIMPL-25115](L0-SIMPL-10783/L1-SIMPL-23891/L2-SIMPL-24969/Epic-SIMPL-24970/Story-SIMPL-25115/README.md) User story · [API] Governance Authority publishes public participant agent information via public endpoint · Ready for DG Acceptance · *not published*
      - [SIMPL-32445](L0-SIMPL-10783/L1-SIMPL-23891/L2-SIMPL-24969/Epic-SIMPL-32445/README.md) Epic · Governance Authority exposes agent participant information through its public discovery profile (Extension) · In Progress · *not published*
        - [SIMPL-33854](L0-SIMPL-10783/L1-SIMPL-23891/L2-SIMPL-24969/Epic-SIMPL-32445/Story-SIMPL-33854/README.md) User story · [API] Governance Authority publishes public participant agent information via a public endpoint using the ParticipantCredential · DevTeam PBI is ready for Implementation check · *not published*
  - [SIMPL-31424](L0-SIMPL-10783/L1-SIMPL-31424/README.md) L1 · Governance Authority maintains a contact email for each participant · In Progress
    - [SIMPL-31425](L0-SIMPL-10783/L1-SIMPL-31424/L2-SIMPL-31425/README.md) L2 · Governance authority registers participants' contact email · In Progress
      - [SIMPL-31426](L0-SIMPL-10783/L1-SIMPL-31424/L2-SIMPL-31425/Epic-SIMPL-31426/README.md) Epic · Applicant provides participant contact email during onboarding · In Progress
        - [SIMPL-31016](L0-SIMPL-10783/L1-SIMPL-31424/L2-SIMPL-31425/Epic-SIMPL-31426/Story-SIMPL-31016/README.md) User story · [UI][IMPROVEMENT] Governance authority views contact email address of an onboarding request · DevTeam PBI is ready for Implementation check · *not published*
        - [SIMPL-31308](L0-SIMPL-10783/L1-SIMPL-31424/L2-SIMPL-31425/Epic-SIMPL-31426/Story-SIMPL-31308/README.md) User story · [UI][IMPROVEMENT] Applicant provides contact email address during onboarding · DevTeam PBI is ready for Implementation check · *not published*
        - [SIMPL-31459](L0-SIMPL-10783/L1-SIMPL-31424/L2-SIMPL-31425/Epic-SIMPL-31426/Story-SIMPL-31459/README.md) User story · [API][IMPROVEMENT] Applicant provides organization contact email · DevTeam PBI is ready for Implementation check · *not published*
        - [SIMPL-31472](L0-SIMPL-10783/L1-SIMPL-31424/L2-SIMPL-31425/Epic-SIMPL-31426/Story-SIMPL-31472/README.md) User story · [API][IMPROVEMENT] Governance authority returns contact email address in onboarding request details · DevTeam PBI is ready for Implementation check · *not published*
        - [SIMPL-31473](L0-SIMPL-10783/L1-SIMPL-31424/L2-SIMPL-31425/Epic-SIMPL-31426/Story-SIMPL-31473/README.md) User story · [API][IMPROVEMENT] Governance authority returns contact email address in onboarding request list · DevTeam PBI is ready for Implementation check · *not published*
        - [SIMPL-31476](L0-SIMPL-10783/L1-SIMPL-31424/L2-SIMPL-31425/Epic-SIMPL-31426/Story-SIMPL-31476/README.md) User story · [UI] Applicant views contact email address of an onboarding request · DevTeam PBI is ready for Implementation check · *not published*
    - [SIMPL-31427](L0-SIMPL-10783/L1-SIMPL-31424/L2-SIMPL-31427/README.md) L2 · Participant updates contact email · Defined · *not published*
      - [SIMPL-31428](L0-SIMPL-10783/L1-SIMPL-31424/L2-SIMPL-31427/Epic-SIMPL-31428/README.md) Epic · Governance Authority maintains contact email registry for participants · Defined · *not published*
        - [SIMPL-28552](L0-SIMPL-10783/L1-SIMPL-31424/L2-SIMPL-31427/Epic-SIMPL-31428/Story-SIMPL-28552/README.md) User story · [API][IMPROVEMENT] Governance Authority returns participant contact email address via Tier 2 communication · DevTeam PBI is ready for Implementation check · *not published*
        - [SIMPL-28553](L0-SIMPL-10783/L1-SIMPL-31424/L2-SIMPL-31427/Epic-SIMPL-31428/Story-SIMPL-28553/README.md) User story · [API][IMPROVEMENT] Participant retrieves other participants' email address (Tier1) · DevTeam PBI is ready for Implementation check · *not published*
        - [SIMPL-31482](L0-SIMPL-10783/L1-SIMPL-31424/L2-SIMPL-31427/Epic-SIMPL-31428/Story-SIMPL-31482/README.md) User story · [API][IMPROVEMENT] Governance Authority provides contact email upon participant creation · DevTeam PBI is ready for Implementation check · *not published*
        - [SIMPL-31483](L0-SIMPL-10783/L1-SIMPL-31424/L2-SIMPL-31427/Epic-SIMPL-31428/Story-SIMPL-31483/README.md) User story · [API][IMPROVEMENT] Governance Authority reads participant contact email from the list · DevTeam PBI is ready for Implementation check · *not published*
        - [SIMPL-31484](L0-SIMPL-10783/L1-SIMPL-31424/L2-SIMPL-31427/Epic-SIMPL-31428/Story-SIMPL-31484/README.md) User story · [UI] Governance Authority views participant contact email · DevTeam PBI is ready for Implementation check · *not published*
        - [SIMPL-31485](L0-SIMPL-10783/L1-SIMPL-31424/L2-SIMPL-31427/Epic-SIMPL-31428/Story-SIMPL-31485/README.md) User story · [API] Participant updates its contact email address via Tier 2 communication · DevTeam PBI is ready for Implementation check · *not published*
      - [SIMPL-31429](L0-SIMPL-10783/L1-SIMPL-31424/L2-SIMPL-31427/Epic-SIMPL-31429/README.md) Epic · Participant updates contact email after onboarding · Defined
        - [SIMPL-31015](L0-SIMPL-10783/L1-SIMPL-31424/L2-SIMPL-31427/Epic-SIMPL-31429/Story-SIMPL-31015/README.md) User story · [UI] Participant updates its contact email address · DevTeam PBI is ready for Implementation check · *not published*
        - [SIMPL-31486](L0-SIMPL-10783/L1-SIMPL-31424/L2-SIMPL-31427/Epic-SIMPL-31429/Story-SIMPL-31486/README.md) User story · [API] Participant user updates participant's contact email address (Tier1) · DevTeam PBI is ready for Implementation check · *not published*
  - [SIMPL-32447](L0-SIMPL-10783/L1-SIMPL-32447/README.md) L1 · Participant publishes its public profile · In Progress · *not published*
    - [SIMPL-32448](L0-SIMPL-10783/L1-SIMPL-32447/L2-SIMPL-32448/README.md) L2 · Consumers and Providers expose public identity credentials in their public profile · In Progress · *not published*
      - [SIMPL-32449](L0-SIMPL-10783/L1-SIMPL-32447/L2-SIMPL-32448/Epic-SIMPL-32449/README.md) Epic · Consumers and Providers publish identity credentials in their public discovery profile · In Progress · *not published*
        - [SIMPL-31663](L0-SIMPL-10783/L1-SIMPL-32447/L2-SIMPL-32448/Epic-SIMPL-32449/Story-SIMPL-31663/README.md) User story · [IMPROVEMENT][API] Consumer and Provider can publish their credential details in the form of a DID document via public endpoint · DevTeam PBI is ready for Implementation check · *not published*
        - [SIMPL-31743](L0-SIMPL-10783/L1-SIMPL-32447/L2-SIMPL-32448/Epic-SIMPL-32449/Story-SIMPL-31743/README.md) User story · [IMPROVEMENT][API] Consumer and Provider publishes its credentials as a certificate chain in pem format via public endpoint · DevTeam PBI is ready for Implementation check · *not published*
