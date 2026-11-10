> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14734](../../README.md) / [Epic-SIMPL-960](../README.md) / **Story-SIMPL-3241**

# SIMPL-3241: API for Usage Policy

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14734](../../README.md) (L2) › [SIMPL-960](../README.md) (Epic) › **SIMPL-3241** (User story)

## Text

As a Provider User\
I want to use an API to assign the previously formulated machine-readable usage policies to the self-description and thus to the dataset/application/infrastructure\
so that the usage to the dataset/application/infrastructure can be evaluated and enforced as defined in the policy.

+**Technical Description**+

The API must retrieve the parameters for the usage policies, create a valid ODRL expression and return it.

Definition of the policies is described as follows:

[an internal page]

Should written in the form of ODRL Policies

[https://www.w3.org/TR/odrl-model/](https://www.w3.org/TR/odrl-model/)

can be embedded in the JSON-LD document of the self-description.

Example can be found here:

[an internal page]

```
{
  "@context": "http://www.w3.org/ns/odrl.jsonld",
  "@type": "Policy",
  "uid": "{POLICY_URI}",
  "profile": "http://www.w3.org/ns/odrl/2/odrl.jsonld",
  "target": "{RESSOURCE_URI}",
  "assigner": {
    "uid": "{PROVIDER_URI}",
    "role": "http://www.w3.org/ns/odrl/2/assigner"
  },
  "permission": [
    {
      "assignee": {
        "uid": "Consumer",
        "role": "http://www.w3.org/ns/odrl/2/assignee"
      },
      "action": [
        { "name": "http://www.w3.org/ns/odrl/2/use" }
      ],
      "target": "{RESSOURCE_URI}",
      "constraint": [
        {to be added by frontend)
      ]
    }
  ]
}
```

- POLICY_URI: generated uuid
- RESSOURCE_URI: web:did of the service offering (SD)
- PROVIDER_URI: participant ID from Aruba API

the constraints are set using the information from the frontend

**Restricted Number of Usages:**
```
        {
          "leftOperand": "http://www.w3.org/ns/odrl/2/count",
          "operator": "http://www.w3.org/ns/odrl/2/lteq",
          "rightOperand": "10"
        } 
```
The 10 is replaced by the parameter provided in the frontend

**Duration-restricted Data Usage**
```
        {
          "leftOperand": "http://www.w3.org/ns/odrl/2/dateTime",
          "operator": "http://www.w3.org/ns/odrl/2/leq",
          "rightOperand": "2024-12-31T23:59:59"
        },
        {
          "leftOperand": "http://www.w3.org/ns/odrl/2/dateTime",
          "operator": "http://www.w3.org/ns/odrl/2/geq",
          "rightOperand": "2024-01-01T00:00:00"
        } 
```
the rightOperand are replaced by the value from the frontend

**Deletion after usage (no parameter)**
```
        {
          "leftOperand": "http://www.w3.org/ns/odrl/2/deletion",
          "operator": "http://www.w3.org/ns/odrl/2/eq",
          "rightOperand": "after_use"
        } 
```
+**Acceptance Criteria**+

1. The API creates a valid ODRL Expression (manual testing, validaton of JSON format automatically)
1. The API must ensure that all specified constraints are part of the ODRL Expression
1. The API automatically inserts the provider and resource information
1. The API validates the ODRL expression format
1. The API provides comprehensive feedback / error messages in case of missmatch / validation errors
1. The API returns the JSON Object containing the ODRL Expression
1. THE JSON ODRL expression is assigned to the SD formatted as a string

Once US is done the related US can be closed/marked as done

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-960](../README.md) (Epic) A Provider is supported in defining a usage policy by using a policy template via API |
