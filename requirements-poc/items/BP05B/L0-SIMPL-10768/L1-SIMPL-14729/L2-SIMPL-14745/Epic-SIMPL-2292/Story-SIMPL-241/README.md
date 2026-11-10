> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14729](../../../README.md) / [L2-SIMPL-14745](../../README.md) / [Epic-SIMPL-2292](../README.md) / **Story-SIMPL-241**

# SIMPL-241: Deploy signer service

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14729](../../../README.md) (L1) › [SIMPL-14745](../../README.md) (L2) › [SIMPL-2292](../README.md) (Epic) › **SIMPL-241** (User story)

## Text

Create the helm charts for the deployment of the Gaia-X XSFC TSA signer service. Deploy the helm chart in the development kubernates cluster. Set up the token to sign SD

`AC:`

- The Signer application is deployed on the development K8 cluster
- The deployed Signer is accessible at the designated URL [https://signer.dev.simpl-europe.eu/signer/](https://signer.dev.simpl-europe.eu/signer/liveness)

- Core features of the Signer are functional, including:
   - Signing of SDs

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-2292](../README.md) (Epic) A Provider signs and  submits a new Resource description or a new version of a Resource description |
