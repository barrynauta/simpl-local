# Bug reports: simpl-vue-components

Target: https://code.europa.eu/simpl/simpl-open/development/data1/simpl-vue-components

Each numbered section is a self-contained issue, ready to paste into GitLab separately.

Test environment common to all findings:

| Item | Value |
|---|---|
| Date tested | 2026-07-21 |
| 'simpl-sd-ui' | v1.15.0, HEAD '3c95eee' |
| '@simpl/vue-components' | 2.24.2 (resolved from '^2.24.2') |
| Node / npm | v25.8.2 / 11.12.1 |
| Backend | 'sdtooling-api-be' local jar, '--web.mvc.bearerToken.required=false' |
| Auth | Disabled via empty 'PUBLIC_AUTH_KEYCLOAK_*' |

---

## Issue 1: Creation wizard advances past invalid and incomplete steps

**Severity: high. Produces invalid Self-Descriptions.**

### Details

'ResourceDescriptionCreationWizard.vue' renders required-field validation but does not enforce it. The user can advance through every step of the Resource Description creation wizard, and reach the Summary, without completing any required field. Validation errors are displayed on screen and then ignored when navigating.

### Steps to reproduce

1. Open the SD UI and select Create new > 'data'.
2. On step 1 "Data properties", enter 'csv' in the required 'Format' field. Click "Next step".
3. On step 2 "General service properties", leave all four required fields empty ('Name', 'Description', 'Service access point', 'In language'). Click "Next step".
4. On step 3 "Asset properties", select Resource Sharing Method 'MINIO_S3', then Source Template 'Data Template MinioS3'. A MinIO sub-form appears with three required fields, each already displaying the inline error "is a required property". Leave all three empty and click "Next step".
5. Click step "9 SUMMARY" directly in the stepper at the top of the page.

### Actual Result

- Step 3: the wizard advances to step 3. No error is shown anywhere on the page, and step 2's marker in the stepper is not flagged as incomplete or invalid.
- Step 4: the wizard advances to step 4, despite three validation errors being visible on screen at the moment the button is clicked.
- Step 5: the wizard jumps directly to Summary, skipping steps 5 to 8 entirely. The resulting summary document is essentially empty, containing only 'Format: csv'.

### Expected Result

- Clicking "Next step" on a step with unsatisfied required fields does not navigate. The offending fields are marked, and the step is flagged as invalid in the stepper.
- The step list does not permit jumping ahead past incomplete steps.

### Additional Information

There are two separate gaps here, and fixing only one is not sufficient:

1. **The "Next step" handler has no validity guard.** This causes reproduction steps 3 and 4, and is the substantive bug.
2. **'SStepper' is not given the 'linear' prop.** This causes reproduction step 5. 'SStepper' accepts 'linear?: boolean' (see 'dist/components/SStepper/SStepper.vue.d.ts') which restricts free navigation, and the wizard does not pass it.

'SStepper' exposes no validation-related props at all. Its full prop surface is 'id', 'value', 'linear', 'orientation', and it emits only 'update:value'. It is a presentational component, so the validity guard belongs in 'ResourceDescriptionCreationWizard.vue', not in the stepper.

**Prior art in this codebase.** 'simpl-catalogue-client' already does this correctly, in 'src/components/transfer/DataTransferWizard.vue:2':

'''vue
<SStepper id="data-transfer-stepper" :value="step" orientation="horizontal" linear>
'''

The library already supports the guard and another consuming team already uses it. This is an omission in the SD wizard, not a library gap.

**Scope.** Confined to 'simpl-sd-ui'. Of the four frontends depending on '@simpl/vue-components', only 'simpl-sd-ui' and 'simpl-catalogue-client' use 'SStepper', and the latter already passes 'linear'. 'simpl-schema-manager-ui' and 'simpl-vocabulary-manager-ui' do not use the stepper.

---

## Issue 2: "Sign and submit" is disabled with no indication of why

**Severity: medium. Includes an accessibility defect.**

### Details

On the Summary step, the "Sign and submit" button is correctly disabled when the document is incomplete, but nothing tells the user what is missing or which step to return to.

### Steps to reproduce

1. Follow the reproduction steps for Issue 1 to reach step 9 "SUMMARY" with required fields left empty.
2. Observe the "Sign and submit" button in the bottom right.
3. Inspect the button in the browser console:
   '''js
   const b = [...document.querySelectorAll('button')]
     .find(x => /sign and submit/i.test(x.textContent));
   ({ disabled: b.disabled, title: b.title, aria: b.getAttribute('aria-disabled') })
   '''

### Actual Result

The button is greyed out and does nothing. The inspection returns:

'''
{ disabled: true, title: "", aria: null }
'''

There is no tooltip, no 'aria-disabled', and no validation summary anywhere on the Summary step. The only validation messages present in the document are three "is a required property" strings belonging to the step 3 sub-form, which are not visible from the Summary step. The SHACL-derived required fields ('Name', 'Description', 'Service access point', 'In language', 'Contact', 'Signature') produce no message at all.

### Expected Result

The Summary step lists the incomplete steps and fields, with each entry linking to the relevant step. The button carries 'aria-disabled' and an 'aria-describedby' pointing at that summary, so assistive technology can announce why the action is unavailable.

### Additional Information

Using the 'disabled' attribute without 'aria-disabled' and without an associated explanation means assistive technology announces nothing actionable. The control is skipped or reported as unavailable with no reason given.

This compounds Issue 1. Because navigation is unrestricted, a user can walk all nine steps without obstruction and arrive at a dead button that never explains itself. Fixing Issue 1 reduces the severity of this one but does not remove it.

---

## Issue 3: Validation messages are raw ajv fragments with no field name

**Severity: low.**

### Details

Required-field errors in the resource-address sub-form are surfaced as the unmodified ajv/JSON Schema keyword message, with no subject.

### Steps to reproduce

1. Open the SD UI and select Create new > 'data'.
2. Navigate to step 3 "Asset properties".
3. Select Resource Sharing Method 'MINIO_S3', then Source Template 'Data Template MinioS3'.
4. Observe the error text under each of the three generated fields.

### Actual Result

Three identical, stacked messages appear under 'MinIO Endpoint URL', 'Bucket name', and 'Object name':

> is a required property

None of them names its field. Read aloud or read out of context, the message is a sentence fragment with no subject.

### Expected Result

Each message names its field, for example:

> MinIO Endpoint URL is required

### Additional Information

The messages are correct in placement and timing, so only the text mapping needs changing. Map schema validation keywords to messages that interpolate the field's display label.

---

## Issue 4: Backend exception text is rendered verbatim to end users

**Severity: medium. Information disclosure and unusable error messaging.**

### Details

When the SD Tooling API returns an error, the raw 'detail' field of the response is displayed directly in the UI error panel, including Java class names and parser internals.

### Steps to reproduce

1. Start 'sdtooling-api-be' **without** the '--schema-sync-service.repository-path' argument, so that '/v2/schemas' returns a 500.
2. Point the SD UI at that API via 'PUBLIC_CREATION_WIZARD_API_URL'.
3. Open the SD UI landing page.

### Actual Result

The landing page renders an error panel containing the raw backend exception:

> **SD Tooling API: Internal Server Error**
> Cannot deserialize value of type 'eu.europa.ec.simpl.data1.common.model.schemasync.SchemaMetadata' from Array value (token 'JsonToken.START_ARRAY') at [Source: REDACTED ('StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION' disabled); line: 1, column: 1] (SCHEMA_FETCH_ERROR)

A second panel directly below reads "Error details are not available (RESOURCE_DESCRIPTIONS_FETCH_ERROR)", so the same screen simultaneously over-shares and under-explains.

### Expected Result

A stable, human-readable message per error code, phrased for a data provider rather than a Java developer. The raw 'detail' is kept to the browser console or a collapsible technical section.

### Additional Information

This is a shared defect across two repositories. 'simpl-sd-ui' passes the upstream response body through unchanged (see the companion issue filed against that repo, 'safeServiceCall' in 'src/services/util/apiErrorHandler.ts'), and this library renders whatever it receives without sanitising it. Fixing either side alone leaves the hole open. The display side should not assume the 'detail' field is presentable to an end user.

---

## Issue 5: Raw enum constants used as user-facing labels

**Severity: low. UX.**

### Details

Machine identifiers are presented directly to end users in two places in the resource description flow.

### Steps to reproduce

1. Open the SD UI landing page and click "Create new". Observe the menu options.
2. Select 'data', navigate to step 3 "Asset properties", and open the Resource Sharing Method dropdown. Observe the option labels.

### Actual Result

- The "Create new" menu lists 'application', 'data', 'infrastructure', which are the lowercase 'resourceType' values.
- The Resource Sharing Method dropdown lists 'BULK_S3', 'DATASTREAM', 'EDELIVERY', 'HTTPDATA_PUSH', 'IONOS_S3', 'MINIO_S3'.

### Expected Result

Human-readable labels in both places, for example "Data" and "MinIO (S3)".

### Additional Information

The data needed for the first case is already available: '/v2/schemas' returns a 'title' for each schema (for example "DataSchema title"), which the menu ignores in favour of 'resourceType'. The sharing-method enum has no label source in the API response, so it needs a display-label mapping in the component.
