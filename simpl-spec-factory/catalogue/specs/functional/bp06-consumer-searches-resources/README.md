<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP06 - Consumer searches resources in data space catalogues](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/tree/main/bp06-consumer-searches-resources?ref_type=heads)
</div>

# BP06 - Consumer searches resources in data space catalogues

## Canonical source
[https://simpl-programme.ec.europa.eu/book-page/bp06-consumer-searches-resources-data-space-catalogues](https://simpl-programme.ec.europa.eu/book-page/bp06-consumer-searches-resources-data-space-catalogues)

> **Captured 15 September 2026** from the source page as revised on 1 September 2026 (requirements changelog: 14 new and 5 updated requirements). The revision reworks BP06 around federated search between a Trusted Governance Authority and one or more Trustor Governance Authorities: the process grows from five to eleven steps and the high-level requirements from five to seven, and every 6.x identifier now denotes a different requirement than in the December 2025 capture.

> **Revised on the source site (30 September 2026), captured 5 October 2026.** The DOME Marketplace was added as a third search scope: three new high-level requirements (6.8 to 6.10), a new step BP06.12, a new prerequisite (BP02G), DOME as external entity, two new assumptions, and DOME text in the overview, BP06.01, BP06.04, BP06.11 and the outcome. Both diagrams were replaced. The changelog lists only the three HLRs. The same revision corrected the BP06.05 title (see the step).

## Overview

This business process covers the situation where a data space _Consumer_ can search resources in the catalogue of one or multiple data spaces, or in the DOME Marketplace. Within the process the _Consumer_ can search for resource descriptions, which can refer to data, application, or infrastructure resources. A search has two aspects: the **search type**, which defines how the search criteria are specified, and the **search scope**, which defines the catalogue or catalogues in which the search is performed.

Search types:

- **Quick search**: use one or multiple search terms that are matched against any field in the resource description.
- **Advanced search**: specify values for one or more attributes of the resource description to refine the search results.

Search scopes:

- **Own catalogue search**: search the catalogue of the _Consumer_'s own data space.
- **Federated catalogue search**: search across multiple data spaces simultaneously.
- **DOME Marketplace search**: search the offerings available in the DOME Marketplace.

It includes the following main steps:

- **Search in data space catalogues**: the _Consumer_ initiates a search using either a quick search or an advanced search, within the own data space, another data space (federated catalogue search) or the DOME Marketplace, and inputs the search terms relevant to the intended search.
- **Execute query**: the _Governance Authority_ finds resource descriptions matching the provided search terms within the data space's own catalogue, the DOME Marketplace or, in the case of federated search, within data space catalogues shared by other data spaces.
- **Access the filtered search results and select one or more resources**: the _Governance Authority_ provides the filtered search results to the participant's system, showing only the resource descriptions they are permitted to access. The _Consumer_ can access and display the details of selected resource descriptions.

## Actors

- _Governance Authority_
- _Consumer_

## External entity

- DOME Marketplace

## Assumptions

- There is a mechanism in place to perform a quality rating of the resources based on its predefined quality rules. This quality rating is not applied to offerings retrieved from the DOME Marketplace, as DOME does not provide a corresponding quality profile for its offerings.
- Discovery of DOME offerings requires no onboarding into DOME.
- Consumption of DOME Marketplace offerings takes place outside the scope of Simpl-Open. DOME is expected to provide sufficient information for the _Consumer_ to contact the corresponding DOME provider.

## Prerequisites

- **Dataspace is configured**: the _Governance Authority_ has configured the catalogue with the corresponding vocabulary and schemas to have the general structure of a resource description, contract clauses, and other vital components (BP02).
- **Consumer onboarded**: the _Consumer_ has successfully completed the onboarding business process (BP03A).
- **End-User authenticated & authorised**: the End-User is authenticated and has the appropriate role and permissions to perform the steps in the process (BP03B).
- **DOME discovery enabled**: the option to search in DOME is only available if the _Governance Authority_ has enabled the DOME Marketplace as a search scope for the data space ([BP02G](../bp02-configuration-governance-authority/bp02g-governance-authority-allows-dome-marketplace-discovery-its-data-space.md)).

![BP06 figure 1](./media/bp06-figure-1.png)
*BP06 figure 1 - overview diagram (source page, 30 September 2026 revision)*

![BP06 figure 2](./media/bp06-figure-2.png)
*BP06 figure 2 - detailed business process diagram (source page, 30 September 2026 revision)*

## Process steps

### Trigger - resource search in data space catalogues

The _Consumer_ initiates the search in data space catalogues.

### BP06.01 Search in data space catalogues

The _Consumer_ initiates a search using either a quick search or an advanced search, and selects the applicable search scope: the own data space catalogue, federated catalogues of other data spaces, or the DOME Marketplace catalogue. The _Consumer_ inputs the search terms relevant to the intended search and optionally a filter related to metadata quality classification. That filter applies to data space resources only, as DOME does not provide a corresponding quality profile for its offerings.

### BP06.02 Add access policy filter parameters to the query

The query is enriched to filter on resource descriptions whose discovery access policies permit their discovery by the _Consumer_ (and potentially the specific End-User of the _Consumer_ from the own data space). The filtering applies to the data space's own catalogue.

### BP06.03 Execute query

The _Governance Authority_ finds resource descriptions matching the provided search terms within the data space's own catalogues.

### BP06.04 Conduct a quality rating of the resources

The _Consumer_ accesses the resource descriptions returned for the search:

- if the _Consumer_ has chosen to search only the own catalogues of a data space, the _Governance Authority_ provides resource descriptions only from its own catalogues. The results include the metadata quality of the resource;
- if the _Consumer_ has chosen to search only one or more federated catalogues of other data spaces, the _Governance Authority_ provides resource descriptions only from the selected federated catalogue or catalogues;
- if the _Consumer_ has chosen to search both own and federated catalogues, the search results from both are returned. The results include the metadata quality of the resource only if the resource is from an own catalogue;
- if the _Consumer_ has selected the DOME search scope, the search results provided by the DOME Marketplace are returned.

The search results are available to the _Consumer_, who can access and display the details of selected resource descriptions. For results retrieved from DOME, the _Consumer_ can access the descriptive information DOME provides for each offering.

### BP06.05 Search one or more federated catalogues of a trustor data space

The Trusted _Governance Authority_ identifies the Trustor _Governance Authorities_ of the selected federated catalogues, enriches the search request with the _Consumer_'s identity attribute parameters and forwards the search request to each Trustor _Governance Authority_.

> **Source-site note:** until the 30 September 2026 revision this step was titled "Access the filtered search results and select one or more resources", left over from the previous version and not matching its text. The title now matches.

### BP06.06 Handle a search from a trusted data space

The Trustor _Governance Authority_ receives a search request from a Trusted _Governance Authority_ and maps the _Consumer_'s identity attributes received from the Trusted _Governance Authority_ against the agreed federated attribute mapping.

The federated search mechanism follows a single-hop federation model. A Trustor _Governance Authority_ therefore does not forward the received search request to another Governance Authority or Trusted Governance Authority.

### BP06.07 Add access policy filter using the mapped attributes

The query is enriched to filter on resource descriptions whose discovery access policies permit their discovery by the _Consumer_ (and potentially the specific End-User of the _Consumer_) of the trusted data space, hence considering the mapped identity attributes.

### BP06.08 Execute query for a Consumer of a trusted data space

The Trustor _Governance Authority_ finds resource descriptions matching the provided search terms within the catalogues of the data space for a _Consumer_ of a trusted data space.

### BP06.09 Forward search results to a trusted data space

The Trustor _Governance Authority_ provides the filtered search results to the Trusted _Governance Authority_ that had forwarded the query.

### BP06.10 Receive search results from a trustor data space

The Trusted _Governance Authority_ receives the results for a forwarded search request from a Trustor _Governance Authority_.

### BP06.11 Collect and provide resources

The _Governance Authority_ collects the filtered resource descriptions from its own catalogues and from the federated catalogues across other data spaces. These resource descriptions are provided together to the _Consumer_ that initiated the search. Where the _Consumer_ has selected the DOME search scope, the _Governance Authority_ also collects the offerings retrieved from the DOME Marketplace in step BP06.12 and aggregates them with these resource descriptions.

### BP06.12 Forward query to DOME Marketplace catalogue

Where the _Consumer_ has selected the DOME search scope in step BP06.01, the _Governance Authority_ forwards the query to the DOME Marketplace catalogue and receives the offerings that match the search criteria. Both quick and advanced search are supported. A DOME Marketplace search is not filtered by access policies: all data space Participants can access DOME offerings once the _Governance Authority_ has enabled the DOME search scope through BP02G.

## High-level requirements

| ID | Title | Issue ID | Canonical source |
|----|-------|----------|------------------|
| 6.1 | A Consumer searches resources in the catalogues of a data space. | SIMPL-1329 | [61-…](https://simpl-programme.ec.europa.eu/book-page/61-consumer-searches-resources-catalogues-data-space) |
| 6.2 | A Governance Authority provides resource descriptions matching a search request from own catalogues. | SIMPL-23878 | [62-…](https://simpl-programme.ec.europa.eu/book-page/62-governance-authority-provides-resource-descriptions-matching-search-request-own) |
| 6.3 | A Trustor Governance Authority receives a search request from a trusted data space. | SIMPL-28854 | [63-…](https://simpl-programme.ec.europa.eu/book-page/63-trustor-governance-authority-receives-search-request-trusted-data-space) |
| 6.4 | A Trustor Governance Authority provides the results for a search request to a trusted data space. | SIMPL-28855 | [64-…](https://simpl-programme.ec.europa.eu/book-page/64-trustor-governance-authority-provides-results-search-request-trusted-data-space) |
| 6.5 | A Trusted Governance Authority forwards a search request to a federated data space. | SIMPL-28853 | [65-…](https://simpl-programme.ec.europa.eu/book-page/65-trusted-governance-authority-forwards-search-request-federated-data-space) |
| 6.6 | A Governance Authority provides resources. | SIMPL-28856 | [66-…](https://simpl-programme.ec.europa.eu/book-page/66-governance-authority-provides-resources) |
| 6.7 | A Consumer consults the returned resources of a search request. | SIMPL-4492 | [67-…](https://simpl-programme.ec.europa.eu/book-page/67-consumer-consults-returned-resources-search-request) |
| 6.8 | The Consumer selects the DOME Marketplace catalogue as a search scope. | SIMPL-6760 | [68-…](https://simpl-programme.ec.europa.eu/book-page/68-consumer-selects-dome-marketplace-catalogue-search-scope) |
| 6.9 | The Governance Authority forwards the query of the Consumer on the DOME Marketplace catalogue. | SIMPL-6761 | [69-…](https://simpl-programme.ec.europa.eu/book-page/69-governance-authority-forwards-query-consumer-dome-marketplace-catalogue) |
| 6.10 | The Governance Authority aggregates DOME Marketplace offerings with the other search results. | SIMPL-6762 | [610-…](https://simpl-programme.ec.europa.eu/book-page/610-governance-authority-aggregates-dome-marketplace-offerings-other-search-results) |

> **Renumbered on the source site (1 September 2026).** The previous 6.1 to 6.5 (UI/API search, search in another federated data space, search results, vocabulary-limited search parameters, data quality assessment) were replaced. The former 6.4 (limit search parameters to referred vocabularies) and 6.5 (data quality assessment) no longer exist as high-level requirements; the former 6.1 and 6.3 content now lives under 6.1 and 6.7; the former 6.2 (federated search) is covered by 6.1.2 and 6.5. Identifiers are reused with different meanings, so references to BP06 HLRs made before September 2026 must be re-checked against this table.
>
> **Source-site defect (15 September 2026):** the "High Level Requirements" list at the foot of the canonical BP06 page stops at 6.4; 6.5, 6.6 and 6.7 are only reachable through the book navigation and are mirrored here from their own pages.
>
> **Status at time of capture (15 September 2026):** every high-level and detailed requirement listed here carries status *Proposed* on the source site. 6.8 to 6.10 (captured 5 October 2026) are also *Proposed* and have no detailed requirements.

### Detailed requirements

- [6.1 - A Consumer searches resources in the catalogues of a data space](./bp61-consumer-searches-resources-catalogues-data-space.md)
- [6.2 - A Governance Authority provides resource descriptions matching a search request from own catalogues](./bp62-governance-authority-provides-resource-descriptions-matching-search-request-own.md)
- [6.3 - A Trustor Governance Authority receives a search request from a trusted data space](./bp63-trustor-governance-authority-receives-search-request-trusted-data-space.md)
- [6.4 - A Trustor Governance Authority provides the results for a search request to a trusted data space](./bp64-trustor-governance-authority-provides-results-search-request-trusted-data-space.md)
- [6.5 - A Trusted Governance Authority forwards a search request to a federated data space](./bp65-trusted-governance-authority-forwards-search-request-federated-data-space.md)
- [6.6 - A Governance Authority provides resources](./bp66-governance-authority-provides-resources.md)
- [6.7 - A Consumer consults the returned resources of a search request](./bp67-consumer-consults-returned-resources-search-request.md)
- [6.8 - The Consumer selects the DOME Marketplace catalogue as a search scope](./bp68-consumer-selects-dome-marketplace-catalogue-search-scope.md)
- [6.9 - The Governance Authority forwards the query of the Consumer on the DOME Marketplace catalogue](./bp69-governance-authority-forwards-query-consumer-dome-marketplace-catalogue.md)
- [6.10 - The Governance Authority aggregates DOME Marketplace offerings with the other search results](./bp610-governance-authority-aggregates-dome-marketplace-offerings-other-search-results.md)

## Outcomes

- **Resources selected for review**: the _Consumer_ accesses and can consult the usage contract template identifying the terms and conditions related to the use of the selected resource discovered in the data space catalogues. The _Consumer_ then decides whether or not to consume the resource offered by the _Provider_ (BP08, BP09A, BP09B). If a contract is required for the consumption of a resource and not already in place, the _Consumer_ and _Provider_ establish a usage contract (BP07). Where the selected resource is an offering discovered in the DOME Marketplace, information about the offering and the provider's points of contact are provided. Contracting, provisioning and consumption of that DOME offering take place directly between the _Consumer_ and the DOME provider, outside the data space.
