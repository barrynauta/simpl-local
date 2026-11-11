<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.1 - A Provider consults its own Resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md) / [5B.1.3 A Provider consults the version history of one of its own resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md#5b13-a-provider-consults-the-version-history-of-one-of-its-own-resource-descriptions) / Epic
</div>

# Epic: A Provider consults the version history of one of its own resource descriptions [Improvement]

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.1.3 A Provider consults the version history of one of its own resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md#5b13-a-provider-consults-the-version-history-of-one-of-its-own-resource-descriptions), under high-level requirement [5B.1 A Provider consults its own Resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md). In the process it belongs to step [Trigger (resource description management)](../README.md#trigger--resource-description-management).

In the backlog (Jira) it is item SIMPL-29371. It has no user stories yet.

## What this Epic delivers

#### Improvement

This epic was created to address improvement activities needed to align the implementation with the latest API Definition Guidelines, specifically for aspects that were not previously handled correctly.

#### Storycard

**As a** Provider\
**I want to** retrieve the version history of one of my own resource descriptions via UI or API\
**So that** I can review it and decide if any action should be taken.

#### Acceptance criteria

**Scenario:** A Provider consults the version history of one of its own resource descriptions

1. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request to retrieve the version history of one of my own resource descriptions via UI or API\
   **Then** the version history of this resource description is returned.

---

Back to [5B.1.3 A Provider consults the version history of one of its own resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md#5b13-a-provider-consults-the-version-history-of-one-of-its-own-resource-descriptions) · [5B.1 A Provider consults its own Resource descriptions](../bp5b1-provider-consults-its-own-resource-descriptions.md) · [BP05B process page](../README.md)
