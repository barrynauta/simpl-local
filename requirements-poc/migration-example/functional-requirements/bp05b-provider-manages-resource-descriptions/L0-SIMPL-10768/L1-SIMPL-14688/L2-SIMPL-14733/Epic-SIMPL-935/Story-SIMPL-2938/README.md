> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14733](../../README.md) / [Epic-SIMPL-935](../README.md) / **Story-SIMPL-2938**

# SIMPL-2938: API for Access Policy

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14733](../../README.md) (L2) › [SIMPL-935](../README.md) (Epic) › **SIMPL-2938** (User story)

## Text

**As a** Provider User\
**I want to** use an API to assign the previously formulated machine-readable access policies to the self-description and thus to the dataset/application/infrastructure\
**So that** the access to the dataset/application/infrastructure can be evaluated and enforced as defined in the policy.

+**Technical Description**+

The API must retrieve the parameters for the access policies, create a valid ODRL expression and return it.

Definition of the policies is descriped as follows:

an internal page

Should written in the Form of ODRL Policies

[https://www.w3.org/TR/odrl-model/](https://www.w3.org/TR/odrl-model/)

can be embedded in the JSON-LD document of the self-description.

For the MVP we plan to support access policies with limited expressive power. It is possible to define two different actions

- [http://simpl.eu/odrl/actions/](http://simpl.eu/odrl/actions/)search: the attribute holder is able to search for the dataset/application/infrastructure
- [http://simpl.eu/odrl/actions/](http://simpl.eu/odrl/actions/)consume: The attribute holder can consume the dataset/application/infrastructure
- [http://simpl.eu/odrl/actions/](http://simpl.eu/odrl/actions/)restrictedConsume: The attribute holder can consume the dataset/application/infrastructure with restrictive access

**consume, restrictedConsume** implies search.

For constraints we plan to support date time constraints that allow to specify when the policy should be valid.

{RESSOURCE_URI}

, {POLICY_URI}, {PROVIDER_URI} are later automatically replaced with the correct URI. {SECURITY_ATTRIBUTE_ID} need to be specified, for the first iteration from a list of static IDs later available over an API provided by Aruba.

Example can be found here: an internal page

draftId=163807492&draftShareId=d0caf9a0-fa82-4153-b08e-e0238d00adc1& (internal page)

Each Policy from the frontend is mapped into a new permission:

The action, attribute and valid from/until are set by the value from the frontend. If no valid until or from is given then the part is omitted.

Those attributes are filled out automatically:\
`**{POLICY_URI}:** can be generated like uuid`

{{*{RESSOURCE_URI}

:* the web:did from the service offering}}

{{

{PROVIDER_URI}

:\ URI of the provider from API by Aruba}}

(internal page)

+**Acceptance Criteria**+

1. The API creates a valid ODRL Expression and takes the action and the security attribute
1. The API must ensure that only the speficied actions and security attributes are used
1. The API automatically inserts the provider and resource information
1. The API validates the ODRL expression format
1. The API provides comprehensive feedback / error messages in case of mismatch / validation errors
1. The API returns the JSON Object containing the ODRL Expression
1. THE JSON ODRL expression is assigned to the SD formatted as a string

Once US is done the related US can be closed/marked as done

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-935](../README.md) (Epic) A Provider is supported in defining an access policy by a using policy template via API |
