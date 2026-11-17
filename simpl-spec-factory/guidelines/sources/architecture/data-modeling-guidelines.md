<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
<p>
📍 <strong>You are here</strong> / <a href="../README.md">🏠 Home</a> / <a href="README.md">Foundations</a> / Data Modeling Guidelines
</p>
</div>

[[_TOC_]]

# Data Modeling Guidelines

> **Scope.** These guidelines cover relational/persistence data modeling — the
> design of data structures backing an information system, and the conceptual
> domain models that align with API Definition contracts. *They do not cover
> semantic or ontology modeling* of the data space's resource descriptions (RDF,
> RDFS, OWL, SHACL, DCAT-AP). That is a separate modeling discipline with its
> own notation and tooling, and is not governed by this document.

Data modeling is the process of defining and analyzing the data requirements
needed to support the business processes within the scope of an information
system. Three data models are produced while progressing from requirements to
the database that backs the system:

1. **Conceptual Data Model (CDM)** — a technology-independent, business-focused
   description of the entities, their business attributes, and their
   relationships. Used to align expectations between the client and all
   contractors (SC-X) and to discuss and validate requirements with business
   stakeholders.
2. **Logical Data Model (LDM)** — an implementation-neutral refinement of the
   conceptual model that documents data structures in enough detail to be
   implemented in a database. One conceptual model may give rise to multiple
   logical models.
3. **Physical Data Model (PDM)** — a mapping of the logical model to a specific
   database technology, organised into tables and accounting for access,
   performance, and storage. The PDM defines data elements, their structures,
   and the relationships between them.

## Purpose

- To align expectations between the client and all contractors and contributors.
- To standardise the documentation of conceptual data models for every API
  Definition contract (see [API Guidelines](https://code.europa.eu/simpl/simpl-open/foundations/architecture/-/blob/master/api-guidelines.md?ref_type=heads)).
- To facilitate the review, maintenance, and evolution of data models over time.

## Conceptual Data Model (CDM)

The conceptual model captures *what* entities exist in the domain, their
business-relevant attributes, and *how* they relate — without any concern for
how the data is stored or transferred. It is a communication artefact: its
correctness is judged by whether domain experts agree it reflects reality.

### Key principles

- **Business focus.** Describe the business domain, not technical
  implementation. Focus on what the data means, not how it is stored or
  transferred.
- **Abstraction.** The model may differ from API objects (e.g. YAML
  definitions) or database tables. It captures the essence of the business
  concepts.
- **Clarity.** Clearly state what entities exist and how they relate.
- **No technical detail.** Do not include UUIDs, database keys, audit trails, or
  security fields unless they have explicit business value.
- **Visual relationships.** Use associations and multiplicity to show
  relationships, not redundant attributes.
- **Enumerate business states.** For fields like status or type, list the valid
  business values (e.g. `status: OPEN, APPROVED, REJECTED`).
- **Name relationships.** Label association lines with verbs where possible
  (e.g. "Account requests RoleAssignmentRequest").

### Step-by-step

1. **Identify business entities.** Use meaningful names (e.g. `User`, `Role`,
   `CatalogueItem`). These are usually nouns with independent existence that the
   business wants to track. Each becomes a UML class.
2. **Define attributes.** Include only attributes with business value. Avoid
   technical metadata.
3. **Simplify data types.** No need for strict types (`int`, `varchar`). Use
   general terms like `Money`, `Date`, or `Text` where a type is needed at all.
4. **Refine attributes into business concepts.**
   - Transform technical flags into business concepts (e.g. `status:
     AccountStatus` instead of `enabled: boolean`).
   - Rename generic timestamps to business events (e.g. `publicationDate`,
     `expiryDate`).
   - For fields like status or type, indicate the valid business values.
5. **Model relationships.**
   - Convert foreign keys into visual associations.
   - Indicate multiplicity (e.g. `1..1`, `1..*`, `0..1`).
   - Do not list a relationship as an attribute if a connector line already
     exists.
   - Name relationships with verbs where possible.

### Anti-patterns

- Do not include API mechanics (`...Request`, `...Response`, DTOs).
- Do not include redundant attributes or technical IDs.
- Do not mix technical and business concepts in the same model.
- Do not duplicate attributes and relationships.

### Examples

| Good example | Bad example |
|---|---|
| **Entities:** `Account`, `Identity`, `RoleAssignmentRequest`, `AccessRole`, `RoleAssignment`. | **Entities:** technical objects such as `Response`, DTOs. |
| **Relationships:** Account *has* Identity; Account *requests* RoleAssignmentRequest; RoleAssignmentRequest *results in* RoleAssignment; RoleAssignment *grants* AccessRole. | **Attributes:** UUIDs, database keys, audit fields, or redundant foreign keys. |
| **Attributes:** only business-relevant fields (e.g. `status`, `publicationDate`). | **Relationships:** missing business context. |
| **Visual:** lines connect entities; cardinality is indicated (e.g. `1..*`, `0..1`); relationships are labelled with verbs. | **Visual:** mixes technical and business concepts; attributes and relationships are duplicated. |

A conforming conceptual model:

![Conceptual data model, conforming example](./media/cdm-ok.png)

A non-conforming model that exhibits the anti-patterns above:

![Conceptual data model, non-conforming example](./media/cdm-nok.png)


## Logical Data Model (LDM)

The logical model refines the entities introduced by the CDM and forms the
basis of the physical model. It defines the detailed structure of the data
elements and the relationships between them, in a way that can be implemented in
a database but is not yet tied to a specific DBMS.

Represent the LDM as a UML class diagram with:

- Attributes shown on each class, named in `snake_case`.
- Logical data types and identifying attributes (candidate keys) declared.
- Associations with multiplicities; resolve many-to-many associations into
  association classes where the relationship carries its own data.

## Physical Data Model (PDM)

The physical model maps the logical model to a specific database technology and
can be used to generate a database schema. 

Represent the PDM as a UML class diagram using UML relationship notation, where:

- Each table is a UML class.
- Table columns are modelled as UML attributes with physical types.
- Primary keys and foreign keys are modelled as UML attributes/operations as
  appropriate to the modelling tool.
- Indexes and other physical access structures are declared where relevant.

## Considerations

CDMs must go hand in hand with Contract definitions. Each domain change entails:

- Updating the CDM / domain model (conceptual).
- Updating the affected Contract / API.
- Updating the technical and architecture documentation and specifications
  where that model is referenced.

The CDM and the APIs must evolve together, maintaining a single versioned
source of truth.
