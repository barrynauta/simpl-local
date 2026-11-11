> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14687](../../../README.md) / [L2-SIMPL-22687](../../README.md) / [Epic-SIMPL-24060](../README.md) / **Story-SIMPL-27899**

# SIMPL-27899: Governance Authority evaluate metadata quality scores for submitted resource description

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14687](../../../README.md) (L1) › [SIMPL-22687](../../README.md) (L2) › [SIMPL-24060](../README.md) (Epic) › **SIMPL-27899** (User story)

## Text

**As a** Governance Authority\
**I want to** Simpl-Open to automatically evaluate a submitted resource description against the active Metadata Quality Profile\
**So that** metadata quality is measured consistently and objectively.

##### **Technical Details:**

- Implement SHACL-based rule execution for rule types `EXISTENCE`, `IS_IN`, and `CUSTOM_SHACL`.
- Transform configured rules into SHACL constraints and execute validation against the Self-description JSON-LD graph using Apache Jena.
- Can be achieved by a three step approach
   1. Generate a SHACL Constraint file from the MQP (generated_shapes_from_examples.ttl)
   1. Use a SHACL validation on generated_shapes_from_examples.ttl against the self-description, results in validation_report.ttl
   1. Enrich that validation_report.ttl with the list for all rules, scores and classification (validation_report_with_rule_status.ttl)

```
arq --data vocabulary/examples.ttl --query vocabulary/rule_to_shacl_construct.rq --results TURTLE > vocabulary/generated_shapes_from_examples.ttl

shacl validate --shapes vocabulary/generated_shapes_from_examples.ttl --data vocabulary/example_dataset_for_generated_rules.ttl > vocabulary/validation_report.ttl

arq --data vocabulary/examples.ttl --data vocabulary/generated_shapes_from_examples.ttl --data vocabulary/validation_report.ttl --query vocabulary/enrich_validation_report_with_rule_status.rq --results TURTLE > vocabulary/validation_report_with_rule_status.ttl
```

- Generate one validation graph per profile version and cache it for reuse; invalidate cache on profile version change.
- Produce rule-level outputs with at least rule identifier, rule type, and binary result (`1` or `0`).

(image not shown)

- Add explicit validation/error handling for malformed or missing rule definitions; any processing or validation error fails the evaluation with a clear error message.
- Compute weighted dimension and overall scores from rule outputs with deterministic precision and rounding behavior.

(image not shown)

(image not shown)

- Skip rules and/or dimensions with zero weights; fail evaluation when required weights are missing.
- Cover success/failure paths with unit tests and validate cache hit/miss behavior with integration tests.

##### Acceptance Criteria

###### AC1

1. **Given** a resource description is submitted for publication\
   **And** structural and schema validation succeeded\
   **When** the quality evaluation pipeline starts\
   **Then** Simpl-Open retrieves exactly one active Metadata Quality Profile linked to the RD schema from the Schema Manager

###### AC2

2. **Given** a validation graph is available\
   **And** a given Resource Description is validated against its metadata quality rules\
   **When** a rule condition is met for a property of that Resource Description\
   **Then** the rule result is set to 1.

###### AC3

3. **Given** a validation graph is available\
   **And** a given Resource Description is validated against its metadata quality rules\
   **When** a rule condition is not met for a property of that Resource Description\
   **Then** the rule result is set to 0.

###### AC4

4. **Given** a Resource Description for which metadata quality is evaluated\
   **And** the weight of each rule per dimension is available in the Metadata Quality Profile\
   **When** the quality rules have been evaluated for that Resource Description, providing a rule result for each rule\
   **Then** for each dimension, the dimension score is calculated as the weighted sum of the rule results for that dimension.

###### AC5

5. **Given** a Resource Description for which metadata quality is evaluated\
   **And** the dimension scores are available\
   **And** the weight of each dimension is available in the Metadata Quality Profile\
   **When** the overall metadata quality score is calculated\
   **Then** the overall metadata quality score is calculated as the weighted sum of the dimension scores.

###### AC6

6. **Given** no Metadata Quality Profile exists for the submitted resource description schema\
   **When** the validation step is reached\
   **Then** Simpl-Open skips validation execution\
   **And** emits a warning indicating that no MQP exists for that schema.

**Remark:** In alignment with API First and the modularity in testing, the current implementation will use a mock/stub of the endpoint to retrieve the MQP. The implementation test is here SIMPL-30617 to test the integration without mocks

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-24060](../README.md) (Epic) The Governance Authority evaluates the metadata quality of a Resource description |
