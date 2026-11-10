> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10192](../../../README.md) / [L2-SIMPL-1612](../../README.md) / [Epic-SIMPL-28810](../README.md) / **Story-SIMPL-28148**

# SIMPL-28148: [UI][IMPROVEMENT] Governance authority updates an identity attributes

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10192](../../../README.md) (L1) › [SIMPL-1612](../../README.md) (L2) › [SIMPL-28810](../README.md) (Epic) › **SIMPL-28148** (User story)

## Text

*Following the refactoring to align the overall UI of the Security Attributes Provider with the eUI design system (SIMPL-9111), the process for updating an identity attribute was changed. Instead of using a dedicated UI for updates, users could edit identity attributes directly within the paginated table that lists them. By default, the table is read-only, but each row includes an icon that, when clicked, allows the user to edit the information in line. This streamlined the update process and integrated it seamlessly into the main table view.*

*However, this approach is no longer feasible, as the number of attributes for each identity attribute (such as name, code, description, status, etc.) has increased, making it impractical to display and edit all the information within a single table row. Therefore, it is necessary to implement a dedicated edit page for updating identity attributes.*\
----

**As a** Governance Authority user with the Tier 2 identity attributes manager role (IATTR_M) in the Identity Attribute management page, viewing the Identity Attributes table list\
**I want to** be able to update any Identity Attribute displayed on the table, by having and using an UPDATE button for each Identity Attribute created\
**So that** Participants in the dataspace who have that Identity Attribute assigned will inherit the updated information

**Acceptance Criteria:**

1. **Given** I am an authenticated user with IATTR_M role, viewing the paginated table of identity attributes\
   **When** I click the update icon on an identity attribute displayed in the table\
   **Then** I should see a page with a form containing the following input fields:\
   code (read-only field)\
   name\
   description\
   built-in (read-only field)\
   public\
   federable\
   assignable to roles\
   enabled\
   in use (read-only field)\
   displaying the corresponding values of the selected identity attribute,\
   along with the Save and Cancel buttons

2. **Given** I am an authenticated user with IATTR_M role, updating an existing identity attribute\
   **When** I click the Save button\
   **Then** the page calls PUT /tier1/v2/identityAttributes/{identityAttributeId} with a request body including the information provided in the form and a request parameter identityAttributeId corresponding to the unique id of the identity attribute I selected\
   **And** a success message is displayed

3. **Given** I am an authenticated user with IATTR_M role, updating an existing identity attribute\
   **When** I click the Cancel button\
   **Then** the page closes the form and returns to the Identity Attribute Management page without performing any calls to the backend.

4. **Given** I am an authenticated user with IATTR_M role, updating an existing identity attribute\
   **And** I click the Save button\
   **When** the backend operation fails\
   **Then** an error message is displayed\
   **And** the identity attribute is not updated until the action is retried successfully.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-28810](../README.md) (Epic) Governance authority manages the description of identity attributes |
