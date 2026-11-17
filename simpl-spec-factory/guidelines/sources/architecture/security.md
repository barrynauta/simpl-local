<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
<p>
📍 <strong>You are here</strong> / <a href="../README.md">🏠 Home</a> / <a href="README.md">Foundations</a> / Security Architecture
</p>
</div>

[[_TOC_]]


# Simpl-Open Security architecture

## Introduction and Perimeter of Intervention
The scope of Simpl-Open's security is limited to its role as an agent
that facilitates communication between participants (nodes).
An example of a typical end-to-end (E2E) flow is outlined below:

1. A Consumer decides to access a dataset managed by a Data Provider.
- The Data Provider ensures the security of its dataset and compliance with the applicable regulations, at the time of its creation.
- Only a portion of the dataset may be made available for sharing.
- Simpl-Open does not oversee the control of datasets, which remain entirely under the ownership and management of the Data Provider.
2. A Consumer reserves an infrastructure tenant from an Infrastructure Provider.
- This tenant is a Platform-as-a-Service (PaaS) environment derived from Infrastructure-as-a-Service (IaaS) and PaaS services provided by the Infrastructure Provider.
- Both the Data Provider and the Consumer can access this dedicated tenant.
3. Data is transferred from a Data Provider to a Provider's infrastructure (dedicated tenant) using the Simpl-Open Agent, which manages:
    1. Contract Establishment between consumer and provider's organisations.
    2. Secure Communication: Ensuring safe data transfer from the provider (source) organisation to the consumer (target) organisation.
    3. Access Control: Granting tenant access to authorised personnel only.

This typical end-to-end flow is presented on the following figure:

<img src="./media/security-e2e.png" alt="Security end-to-end" width="600" />

Simpl-Open functions as middleware, managing Agent-to-Agent
communication flows without storing any datasets. As the primary
decision-makers in data processing, legal and security responsibilities
regarding the data rest solely with the participants (Data Controllers).
This includes their obligations to comply with legal and regulatory
requirements for both data usage and provision.
As Simpl-Open is a distributed System, the classical end-to-end
responsibilities (such as security, operations etc.) are segmented as
follows:

- Network responsibility: facilitates the pure exchange of information through a network of Simpl-Open agents (i.e. deployment of Simpl-Open).
- Local Node responsibility: each participant (node) is accountable for managing its datasets, applications, infrastructure, and workstation in compliance with its local regulations.

This segmentation of responsibilities is depicted on the following figure:

<img src="./media/security-e2e-segmentation.png" alt="Security end-to-end - Segmentation" width="600" />

The overall security of a Data Space is the result of contributions from
multiple actors, with their respective responsibilities, structured as
follows:
1. Governance Authority: orchestrates the security framework across all participants.
2. Every Participant: Each participant is required to have local IT security plans and implement measures for their personnel, IT systems, and local deployment of the Simpl-Open agent.
3. Deployment of Simpl-Open network: provides security capabilities to ensure a robust protection for the node-to-node communication.
4. Simpl-Open agent: Each agent includes features to comply with the Simpl-Open IT Security Plan, ensuring alignment with the product’s security requirements.
5. Simpl-Open development: The development process adheres to stringent security measures, ensuring the product is resilient against potential threats.


This section focuses exclusively on the architecture of Simpl-Open as a product.

Separate architecture documents will be created for each deployment of
Simpl-Open, including IT security plans tailored to specific Data Spaces
and detailing the responsibilities assigned to each participant.

## DevSecOps Security Architecture related aspects

| Domain | Reference | 
| ------ | --------- | 
| User management | <img src="./media/security-user-management.png" alt="Security User management" width="400" /> | 
| Audit process | <img src="./media/security-audit-process.png" alt="Security Audit process" width="400" /> | 
| OVH Audit trails | [OVH Log Data Platform](https://www.ovhcloud.com/en/identity-security-operations/logs-data-platform/) service is used for K8s audit logs management. | 
| Security testing (SAST, SCA, DAST) | SAST, DAST and SCA are implemented as part of the [DevSecOps](./devsecops.md) pipelines, as described in the DevSecOps Approach section. | 
| Backup and restore | Cluster backups are made using [Velero](https://velero.io/). |

These part covers the aspect related to the "Production of Simpl-Open"
as SW product. 

## Simpl-Open (Product) Security Architecture
The following tables present the features that have already been
introduced as part of the security architecture of Simpl-Open.
These features were identified consequently to other business features
described in SC1 Annex 1 or were implemented based on standard best
practices in application architecture.
In future version of the architecture document, each relevant section
could be updated to highlight how the security controls are implemented
in Simpl-Open. This could be in the shape of a dedicated security
related paragraph in the respective sections, describing the specific
security control implementation.

### Functional Security

The following table presents the features that have been analysed and
designed to address the security aspects listed below.

> [!note]
> The column 'Section of the document' points to a legacy location and needs to be updated once the new structure is in place

| ID | Domain | Node | Feature | Where documented |
|----|--------|------|---------|---------------------|
| 1 | Tier 1 Access Control | All | RBAC (Role Based Access Control) | [Tier 1 authentication ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/authentication-provider-federation/authentication-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-tier1-authentication-service.md), [Tier 2 authentication ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/authentication-provider-federation/authentication-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-tier2-authentication-service.md), [identity provider ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/identity-provider-federation/identity-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-identity-provider-service.md), [security attributes provider ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/security-attribute-provider-federation/security-attributes-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-security-attributes-provider-service.md), [authorisation service ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/supporting-security-services/gateway/gitlab-profile/-/blob/main/documents/architecture/application-component-view/authorization-service-acv.md); the IAA specification (roles, identity attributes, credentials, gateway rules) is being migrated under SIMPL-33638 |
| 2 | Tier 2 Access Control | Governance Authority (Management) | ABAC (Attribute Based Access Control) | [Tier 1 authentication ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/authentication-provider-federation/authentication-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-tier1-authentication-service.md), [Tier 2 authentication ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/authentication-provider-federation/authentication-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-tier2-authentication-service.md), [identity provider ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/identity-provider-federation/identity-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-identity-provider-service.md), [security attributes provider ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/security-attribute-provider-federation/security-attributes-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-security-attributes-provider-service.md), [authorisation service ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/supporting-security-services/gateway/gitlab-profile/-/blob/main/documents/architecture/application-component-view/authorization-service-acv.md); the IAA specification (roles, identity attributes, credentials, gateway rules) is being migrated under SIMPL-33638 |
| 3 | Local Directory System | All | Tier 1 Authentication Provider (OpenID Connect)<br>User & Roles | [Tier 1 authentication ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/authentication-provider-federation/authentication-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-tier1-authentication-service.md), [Tier 2 authentication ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/authentication-provider-federation/authentication-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-tier2-authentication-service.md), [identity provider ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/identity-provider-federation/identity-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-identity-provider-service.md), [security attributes provider ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/security-attribute-provider-federation/security-attributes-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-security-attributes-provider-service.md), [authorisation service ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/supporting-security-services/gateway/gitlab-profile/-/blob/main/documents/architecture/application-component-view/authorization-service-acv.md); the IAA specification (roles, identity attributes, credentials, gateway rules) is being migrated under SIMPL-33638 |
| 4 | Tier 1 Authorisation | All | Authorisation Tier 1 | [Tier 1 authentication ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/authentication-provider-federation/authentication-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-tier1-authentication-service.md), [Tier 2 authentication ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/authentication-provider-federation/authentication-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-tier2-authentication-service.md), [identity provider ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/identity-provider-federation/identity-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-identity-provider-service.md), [security attributes provider ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/security-attribute-provider-federation/security-attributes-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-security-attributes-provider-service.md), [authorisation service ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/supporting-security-services/gateway/gitlab-profile/-/blob/main/documents/architecture/application-component-view/authorization-service-acv.md); the IAA specification (roles, identity attributes, credentials, gateway rules) is being migrated under SIMPL-33638 |
| 5 | Tier 2 Authorisation | Governance Authority | Authorisation Tier 2 | [Tier 1 authentication ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/authentication-provider-federation/authentication-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-tier1-authentication-service.md), [Tier 2 authentication ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/authentication-provider-federation/authentication-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-tier2-authentication-service.md), [identity provider ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/identity-provider-federation/identity-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-identity-provider-service.md), [security attributes provider ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/security-attribute-provider-federation/security-attributes-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-security-attributes-provider-service.md), [authorisation service ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/supporting-security-services/gateway/gitlab-profile/-/blob/main/documents/architecture/application-component-view/authorization-service-acv.md); the IAA specification (roles, identity attributes, credentials, gateway rules) is being migrated under SIMPL-33638 |
| 6 | Tier 1 Authentication | All | Tier 1 Authentication Provider (OpenID Connect) | [Tier 1 authentication ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/authentication-provider-federation/authentication-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-tier1-authentication-service.md), [Tier 2 authentication ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/authentication-provider-federation/authentication-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-tier2-authentication-service.md), [identity provider ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/identity-provider-federation/identity-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-identity-provider-service.md), [security attributes provider ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/security-attribute-provider-federation/security-attributes-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-security-attributes-provider-service.md), [authorisation service ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/supporting-security-services/gateway/gitlab-profile/-/blob/main/documents/architecture/application-component-view/authorization-service-acv.md); the IAA specification (roles, identity attributes, credentials, gateway rules) is being migrated under SIMPL-33638 |
| 7 | Tier 2 Authentication | Governance Authority | Identity Provider Federation<br>Security Attribute Provider | [Tier 1 authentication ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/authentication-provider-federation/authentication-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-tier1-authentication-service.md), [Tier 2 authentication ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/authentication-provider-federation/authentication-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-tier2-authentication-service.md), [identity provider ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/identity-provider-federation/identity-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-identity-provider-service.md), [security attributes provider ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/security-attribute-provider-federation/security-attributes-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-security-attributes-provider-service.md), [authorisation service ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/supporting-security-services/gateway/gitlab-profile/-/blob/main/documents/architecture/application-component-view/authorization-service-acv.md); the IAA specification (roles, identity attributes, credentials, gateway rules) is being migrated under SIMPL-33638 |
| 8 | Tier 2 Authentication | All | Tier 2 Authentication Provider | [Tier 1 authentication ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/authentication-provider-federation/authentication-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-tier1-authentication-service.md), [Tier 2 authentication ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/authentication-provider-federation/authentication-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-tier2-authentication-service.md), [identity provider ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/identity-provider-federation/identity-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-identity-provider-service.md), [security attributes provider ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/security-attribute-provider-federation/security-attributes-provider/gitlab-profile/-/blob/main/documents/architecture/application-component-view/acv-security-attributes-provider-service.md), [authorisation service ACV](https://code.europa.eu/simpl/simpl-open/security/access-control-and-trust/supporting-security-services/gateway/gitlab-profile/-/blob/main/documents/architecture/application-component-view/authorization-service-acv.md); the IAA specification (roles, identity attributes, credentials, gateway rules) is being migrated under SIMPL-33638 |
| 9 | Communication | All | Encryption<br>Integrity<br>Authentication | Agent-to-agent communication: see [Agent-to-Agent Communication - Details](#agent-to-agent-communication---details) below; the Data Space Concepts page (Tier I and Tier II participants) is being migrated under SIMPL-33625 |
| 10 | Logging | All | Logging<br>Monitoring<br>Reporting | [Monitoring static architecture](https://code.europa.eu/simpl/simpl-open/administration/observability/logging/eck-monitoring/gitlab-profile/-/blob/main/documents/architecture/monitoring-static-architecture.md); the business-logging specification is being migrated under SIMPL-33642 |

### Technical Security

Technical security includes deployment aspects of the open-source
technology components, which are outlined below. These have been
implemented based on general secure development guidelines and standard
security architecture patterns.

> [!note]
> The column 'Section of the document' points to a legacy location and needs to be updated once the new structure is in place


| ID | Domain | Node | Components | Section of document |
|----|--------|------|------------|---------------------|
| 1 | Local Directory System | All | Keycloak federated with any Local IDP<br>User & Roles microservice | Simpl-Open Technology Architecture > Technology Components Views > TCV - Domain 1 - Onboarding & IAA |
| 2 | Tier 1 Authorisation | All | Spring Cloud Gateway | Simpl-Open Technology Architecture > Technology Components Views > TCV - Domain 1 - Onboarding & IAA |
| 3 | Tier 2 Authorisation | Governance Authority | Spring Cloud Gateway | Simpl-Open Technology Architecture > Technology Components Views > TCV - Domain 1 - Onboarding & IAA |
| 4 | Tier 1 Authentication | All | Keycloak | Simpl-Open Technology Architecture > Technology Components Views > TCV - Domain 1 - Onboarding & IAA |
| 5 | Tier 2 Authentication | Governance Authority | EJBCA<br>Security Attribute Provider microservice | Simpl-Open Technology Architecture > Technology Components Views > TCV - Domain 1 - Onboarding & IAA |
| 6 | Tier 2 Authentication | All | Tier 2 Authentication Provider microservice | Simpl-Open Technology Architecture > Technology Components Views > TCV - Domain 1 - Onboarding & IAA |
| 7 | Logging | All | Monitoring Service (ELK) | Simpl-Open Technology Architecture > Technology Components Views > TCV - Domain 3 - Management/Operation of Data Space |

### Deployment Consideration
Next to the above features implemented as part of the Simpl-Open
product, every participant should consider the following typical
infrastructure deployment practices (outside of Simpl-Open scope),
including technical security / hardening features, such as:
- DMZ protected access and Network Security of the DMZ (DDoS or any other attack), IDS, FW, VPN
- 2-3 tiers deployment view (Via Container Security or VMs/Network Security)
    - API Gateway / Front End Layer
    - Backend Services
    - Secure Interface towards each Node Applications/Data Sources

These recommended measures are depicted on the following figure:
<img src="./media/security-participant-node.png" alt="Security - Participant node" width="600" />

### Agent-to-Agent Communication - Details

This section describes the handshake process to establish a secured mTLS
connection with another agent. 

#### Initial version (current release)

The initial version of the handshake process is designed to assume that,
the called endpoint belongs to another Simpl-Open agent (is secured by
its Tier 2 Authorisation Gateway) and for this reason is triggered in
any HTTP call performed by the mTLS HTTP Client (the only option for
the current release) to try to establish
a mTLS connection.
However, if the target endpoint does not belong to a Simpl-Open agent,
the communication fallback to standard HTTPS(TLS) 

Handshake process steps:
1. The caller mTLS client uses the Tier 2 Authentication Provider to retrieve a valid Ephemeral Proof
    1. if it already exists, from the agent cache.
    2. if not present in the cache, a new proof is then requested from the Authority and stored in the cache for subsequent calls until it expires.
2. The caller agent performs an mTLS authentication
    1. the caller agent performs always a credential check (OCSP request) to the Authority Identity Provider.
    2. the called agent Tier 2 Authentication Gateway performs a credential check (OCSP request) to the Authority Identity Provider only if no validated proof associated with the credential's public key is found in his cache.
3. The caller agent always sends the ephemeral proof to be validated
    1. the called agent checks the cache to see if the proof was already validated (if true step 3.b is not performed).
    2. the proof is validated and stored in the cache with a TTL (Time To Live) calculated according to its expiration time.
4. Call the endpoint 
    1. after that ABAC policies are enforced and passed, and the request is processed. 

<img src="./media/security-agent-to-agent-communication.png" alt="Security - Agent to agent communication" width="600" />    

#### Enhanced version

The enhanced version of the handshake process is designed to discover if
the called endpoint belongs to another Simpl-Open agent (is secured by
its Tier 2 Authorisation Gateway) and only in this case an attempt to
establish
a mTLS connection
is done. Anyway, if the target endpoint does not belong to a Simpl-Open
Agent the communication fallback to standard HTTPS (TLS), the main
optimisations done are:
- No mTLS authentication is done for non-Simpl-Open endpoints
- Only one credential check per called agent per proof is performed
- No multiple exchanges of the proof that are already exchanged
- Extended Ephemeral Proof Validation(issuance and expiration time are checked to be valid by all agents)

Handshake process steps:

1. The caller agent mTLS client (client/transparent proxy/any other implementation) uses the Tier 2 Authentication Provider to retrieve a valid Ephemeral Proof and its HASH
    1. if it already exists, from the agent cache
    2. if not present in the cache a new proof is then requested from the Authority and stored in the cache for subsequent calls until it expires.
2. The caller agent performs a preflight call (HTTP OPTION) to the Called T2 preflight endpoint using the proof HASH as query string param, to notify which proof will be used:
    1. if the called agent retrieves the proof return a 200 - OK HTTP status code otherwise a 204 - No Content that means that the proof needs to be validated (all other statuses means that the called endpoint doesn't belong to another agent and the handshake is stopped)
3. The caller agent performs an mTLS authentication
    1. the caller agent tries to retrieve the credential check response associated with the received credential's public key from the cache
    2. if no credential check response is found in the cache, the caller agent performs a credential check request (OCSP) to the Authority Identity Provider and stores it in the cache with a specified TTL
    3. the called agent Tier 2 Authentication Gateway tries to retrieve the credential check response associated with the received credential's public key from the cache
    4. if no credential check response is found in the cache, the called agent Tier 2 Authentication Gateway performs a credential check request (OCSP) to the Authority Identity Provider and stores it in the cache with a specified TTL
4. only if a 204 - No Content status code was received in step 2 then the caller agent sends the ephemeral proof to be validated:
    1. the proof is validated and stored in the cache with a TTL calculated according to its expiration time(and not greater than a specified MaxTTL parameter)
5. Call the endpoint 
    1. after that ABAC policies are enforced and passed, and the request is processed 
<img src="./media/security-agent-to-agent-communication2.png" alt="Security - Agent to agent communication" width="600" />    


### Principles of Operations Security: Technical Accounts for Administration
The administration of the Simpl-Open agent requires technical accounts
that are allowed to:

- Perform the first configuration of the agent
- Create the accounts for managing the operations of the agent components. A standard structure for these accounts will be proposed but this structure should be tailored to the business and technical organisation of each participant, where rights are assigned to roles, and roles are assigned to people.
