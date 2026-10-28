<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP06 - Consumer searches resources](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/tree/main/bp06-consumer-searches-resources?ref_type=heads) / Dynamic view
</div>

# BP06 Dynamic View

## Source

> **See also: [Business process overview](./README.md)** — narrative description of this business process, including actors, prerequisites, outcomes, and the full hierarchy of sub-processes.

---

## Trace

The process describes the end user searching for a resource in the catalogue. The end user can either use the quick search or the advanced search. For the advanced search, it is a prerequisite that the local schema registry of the provider/consumer is synced manually with the central schema registry of the governance authority. The search request is sent to the catalogue. The query mapper translates the query input to the related database query language and adds the filters based on the access policies related to the user performing the request. The search engine executes the search queries and returns the results. The result is then displayed in the end User's search Client.

![ACV Dynamic - BP 06 - Search on Catalogue (Infrastructure, Data, Application).png](./media/bp06-sequence.png)

1. **User Search Request Initiation** The user initiates a search request through the **Search Client**. Here, the user enters the search criteria, which could include keywords, filters, or other parameters relevant to the desired resources (e.g., datasets or applications).
	For the Advanced Search the form of the search is defined by the schema in the **Schema Registry**.
2. **Policy Filter Service** The validated search request is forwarded to the **Policy Filter Service**. This service checks the user’s access rights based on the policies defined in the **Policy Creator Component**.
	By applying the relevant filters, the Policy Filter Service modifies the search query to restrict results to only those resources the user is authorised to view.
3. **Query Translation by Adapter Component** The **Query Mapper Adapter** Component receives the policy-filtered search request and translates it into a query language that aligns with the Catalogue’s database structure.
	This step includes mapping the search parameters to the Catalogue’s internal query schema and embedding any access restrictions set by the Policy Filter Service directly into the query.
4. **Catalogue Component Query Execution** The **Catalogue** Component receives the translated and filtered query from the Adapter. Within the Catalogue, the **Search Engine** processes the query by scanning its database, which houses all signed self-descriptions, metadata and associated policies.
	The Catalogue ensures that each self-description or metadata entry returned aligns with the access policies, ensuring compliance with data governance standards.
5. **Result Return to Search Client** After processing the query, the **Catalogue Component** sends the authorised results back through the **Adapter Component**, which re-formats them for the Search Client’s display needs. The Search Client then presents these results to the user in a structured format, along with relevant metadata to provide a comprehensive view of each item.

---

## Participants

_Link to target repos TBD — original tryout used cross-repo relative paths that do not resolve here._

- [**catalogue-client-application**](https://code.europa.eu/simpl/simpl-open/integration/resource-discovery/resource-catalogue) — Catalogue Client Application / Search Client (UI for search input and result display)
- [**simpl-schema-manager**](https://code.europa.eu/simpl/simpl-open/data/semantics-and-vocabulary/schema-management/simpl-schema-manager) — Schema Registry (drives advanced search form fields; provides schema definitions)
- [**simpl-catalogue**](https://code.europa.eu/simpl/simpl-open/integration/resource-discovery/resource-catalogue) — Simpl Catalogue (executes search queries; returns filtered results)
- **Policy Filter Service / Query Mapper Adapter** - unmatched
