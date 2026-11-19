@ui
Feature: Vocabulary management UI
  Browser steps (Playwright) are in features/steps/ui_steps.py; run them
  with ./run.sh ui. Texts, labels,
  routes and accessible names come from D18, D33 to D36, D50, D60, D62, D64
  and D72 to D76. Data is set up through the API with unique names.

  # ---- list (SIMPL-18161, 18163, 18178) ----------------------------------------

  @AC_SIMPL-18161_AC1 @D23 @D31 @D35 @D60 @D72 @D75
  Scenario: The vocabulary list shows each vocabulary with its columns and row actions
    Given a vocabulary "Alpha" exists
    When I open the UI as "ga-admin"
    And I go to "/vocabularies"
    Then the menu item "Vocabularies" is shown
    And the vocabulary table includes the columns "Name,Latest version,Status,Date created,Last updated"
    And the vocabulary table shows a row for "Alpha" with status "Active"
    And the row of "Alpha" shows its dates as "YYYY-MM-DD HH:mm UTC"
    When I open the actions menu "Actions for ${Alpha}"
    Then the menu offers "View vocabulary details,History,Download vocabulary,Update,Deprecate"

  @AC_SIMPL-18161_AC2 @D24 @D34 @D72
  Scenario Outline: A list load error is reported with the problem detail
    Given the API is stubbed to answer <code> to GET "/vocabularies"
    When I open the UI as "ga-admin"
    And I go to "/vocabularies"
    Then I see the alert "The vocabulary list could not be loaded."
    And the alert shows the problem detail

    Examples:
      | code |
      | 500  |
      | 503  |

  @D34
  Scenario: An empty list says so
    Given the API is stubbed to answer an empty list to GET "/vocabularies"
    When I open the UI as "ga-admin"
    And I go to "/vocabularies"
    Then I see the text "No vocabularies found."

  @AC_SIMPL-18163_AC1 @D19 @D72
  Scenario: Filters narrow the list
    Given a vocabulary "Alpha" exists
    And a vocabulary "Beta" exists
    And the vocabulary status of "Beta" is set to "DEPRECATED"
    When I open the UI as "ga-viewer"
    And I go to "/vocabularies"
    And I set the filter "Name contains" to "${uid}"
    And I set the filter "Vocabulary status" to "Deprecated"
    Then the vocabulary table shows only "Beta"

  @AC_SIMPL-18163_AC2 @D23 @D50
  Scenario: A click on a sortable column sorts it, a click on the active column reverses it
    Given a vocabulary "Alpha" exists
    And a vocabulary "Beta" exists
    When I open the UI as "ga-viewer"
    And I go to "/vocabularies"
    And I set the filter "Name contains" to "${uid}"
    Then the vocabulary table is ordered "Alpha,Beta"
    When I click the column header "Name"
    Then the vocabulary table is ordered "Beta,Alpha"
    When I click the column header "Date created"
    Then the vocabulary table is sorted by "Date created" ascending
    And the column headers "Description,Latest version,Creator" are not sortable

  @AC_SIMPL-18178_AC1 @D36 @D72
  Scenario: A DEPRECATED vocabulary shows a warning in the list and on its details page
    Given a vocabulary "Old" exists
    And the vocabulary status of "Old" is set to "DEPRECATED"
    When I open the UI as "ga-viewer"
    And I go to "/vocabularies"
    Then the row of "Old" has a warning icon named "This vocabulary is deprecated. Its use is discouraged."
    When I go to "/vocabularies/${Old}"
    Then I see the text "This vocabulary is deprecated. Its use is discouraged."

  # ---- details, history and download (18165, 18167, 18176, 18452, 18559) ---------

  @AC_SIMPL-18167_AC1 @AC_SIMPL-18176_AC2 @D35 @D72
  Scenario: History opens the details page with every version newest first
    Given a vocabulary "Alpha" exists with 3 versions
    When I open the UI as "ga-viewer"
    And I go to "/vocabularies"
    And I open the actions menu "Actions for ${Alpha}"
    And I choose "History"
    Then I am on the route "/vocabularies/${Alpha}"
    And the header shows the name "${Alpha}" and the status "Active"
    And the version table has the columns "Version,Status,Date created,Description,Change log,Actions"
    And the version table lists the versions "3,2,1"
    And the version rows "2,1" show the status "Deprecated"

  @AC_SIMPL-18176_AC1 @AC_SIMPL-18176_AC3 @AC_SIMPL-18452_AC1 @D35 @D75
  Scenario: Selecting a deprecated version shows all its metadata
    Given a vocabulary "Alpha" exists with 2 versions
    When I open the UI as "ga-viewer"
    And I go to "/vocabularies/${Alpha}"
    And I select version 1
    Then the region "Version 1" shows "Version,Status,Date created,Creator,Description,Change log"
    And the region "Version 1" shows the creator "${email:ga-admin}"
    And the region "Version 1" offers "Download"

  @AC_SIMPL-18452_AC2 @AC_SIMPL-18165_AC1 @D29
  Scenario: Download gives the stored Turtle of the chosen version
    Given a vocabulary "Alpha" exists with 2 versions
    When I open the UI as "ga-viewer"
    And I go to "/vocabularies/${Alpha}"
    And I open the actions menu "Actions for version 1"
    And I choose "Download"
    Then a file "${Alpha}-v1.ttl" is downloaded with the content of version 1 of "Alpha"

  @AC_SIMPL-18165_AC1 @D29 @D35
  Scenario: Download vocabulary from the list gives the latest version
    Given a vocabulary "Alpha" exists with 2 versions
    When I open the UI as "ga-viewer"
    And I go to "/vocabularies"
    And I open the actions menu "Actions for ${Alpha}"
    And I choose "Download vocabulary"
    Then a file "${Alpha}.ttl" is downloaded with the content of version 2 of "Alpha"
    When I choose "View vocabulary details"
    Then I am on the route "/vocabularies/${Alpha}"

  @AC_SIMPL-18559_AC1 @D36 @D64
  Scenario: A deprecated version links to the latest ACTIVE version
    Given a vocabulary "Alpha" exists with 2 versions
    When I open the UI as "ga-viewer"
    And I go to "/vocabularies/${Alpha}"
    Then the version row 1 shows "This version is deprecated."
    And the version row 1 links "Latest ACTIVE version: 2" to version 2
    And the version row 2 shows no deprecation warning

  @AC_SIMPL-18559_AC2 @D36
  Scenario: Without an ACTIVE version only the warning is shown
    Given a vocabulary "Alpha" exists
    And the version status of version 1 of "Alpha" is set to "DEPRECATED"
    When I open the UI as "ga-viewer"
    And I go to "/vocabularies/${Alpha}"
    Then the version row 1 shows "This version is deprecated."
    And the version row 1 shows no "Latest ACTIVE version" line

  @D64 @D36
  Scenario: A version of a DEPRECATED vocabulary has no latest ACTIVE version line
    Given a vocabulary "Alpha" exists with 2 versions
    And the vocabulary status of "Alpha" is set to "DEPRECATED"
    When I open the UI as "ga-viewer"
    And I go to "/vocabularies/${Alpha}"
    Then the version row 1 shows "This version is deprecated."
    And the version row 2 shows "This version is deprecated."
    And no version row shows a "Latest ACTIVE version" line

  @D35 @D72 @D24
  Scenario Outline: A history load error is reported with the problem detail
    Given a vocabulary "Alpha" exists
    And the API is stubbed to answer <code> to GET "/vocabularies/${Alpha}/versions"
    When I open the UI as "ga-viewer"
    And I go to "/vocabularies/${Alpha}"
    Then I see the alert "The version history could not be loaded."
    And the alert shows the problem detail

    Examples:
      | code |
      | 500  |
      | 503  |

  @D18 @D24
  Scenario: Any 403 from the API shows the authorization message
    Given a vocabulary "Alpha" exists
    And the API is stubbed to answer 403 to POST "/vocabularies/${Alpha}/versions"
    When I open the UI as "ga-admin"
    And I go to "/vocabularies/${Alpha}/versions/new"
    And I fill in "Change log" with "Refused"
    And I click "Submit"
    Then I see the alert "You are not authorized to perform this action."

  @D76 @D37
  Scenario: A 413 from the API appears above the form
    Given the API is stubbed to answer 413 to POST "/vocabularies"
    When I open the UI as "ga-admin"
    And I go to "/vocabularies/new"
    And I fill in "Name" with "${Big}"
    And I fill in "Description" with "Too big on the server"
    And I attach the fixture "w3c_namespaces_only.ttl" as "Vocabulary file"
    And I click "Submit"
    Then the alert shows the problem detail

  @D60 @D74
  Scenario: Both forms offer Submit and Cancel; Cancel returns without creating anything
    Given a vocabulary "Alpha" exists
    When I open the UI as "ga-admin"
    And I go to "/vocabularies/${Alpha}/versions/new"
    Then I see the button "Submit"
    And I see the button "Cancel"
    When I click "Cancel"
    Then vocabulary "Alpha" has 1 version
    When I go to "/vocabularies/new"
    Then I see the button "Submit"
    And I see the button "Cancel"

  @D35 @D60
  Scenario: An unknown vocabulary shows a not-found message with a link back
    When I open the UI as "ga-viewer"
    And I go to "/vocabularies/${Ghost}"
    Then I see the text "Vocabulary not found."
    And I see the link "Back to vocabulary list"

  # ---- create (18169, 18180, 18182, 18186, 18193) ---------------------------------

  @AC_SIMPL-18169_AC1 @D32 @D60 @D74
  Scenario: The creation form asks for name, description and file only
    When I open the UI as "ga-admin"
    And I go to "/vocabularies"
    And I click "New Vocabulary +"
    Then I am on the route "/vocabularies/new"
    And the form has the fields "Name,Description,Vocabulary file"
    And the form has no field "Change log"
    And the form has no field "Version"

  @AC_SIMPL-18169_AC2 @D37 @D62
  Scenario: Field errors show under the fields and Submit moves focus to the first one
    When I open the UI as "ga-admin"
    And I go to "/vocabularies/new"
    And I fill in "Name" with "lower"
    And I click "Submit"
    Then the field "Name" shows an error
    And the field "Description" shows an error
    And the focus is on the field "Name"
    And no request was sent to POST "/vocabularies"

  @AC_SIMPL-18169_AC2 @D37 @D15
  Scenario: A duplicate name is reported above the form
    Given a vocabulary "Alpha" exists
    When I open the UI as "ga-admin"
    And I go to "/vocabularies/new"
    And I fill in "Name" with "${Alpha}"
    And I fill in "Description" with "Duplicate"
    And I attach the fixture "w3c_namespaces_only.ttl" as "Vocabulary file"
    And I click "Submit"
    Then I see the alert "Vocabulary with name '${Alpha}' already exists."

  @AC_SIMPL-18180_AC1 @AC_SIMPL-18180_AC5 @AC_SIMPL-18180_AC8 @D13 @D60 @D62
  Scenario: A file with a syntax error is reported on upload and blocks Submit
    When I open the UI as "ga-admin"
    And I go to "/vocabularies/new"
    And I attach the fixture "syntax_error.ttl" as "Vocabulary file"
    Then I see a syntax error at line 3
    And the button "Submit" is disabled

  @AC_SIMPL-18180_AC1 @AC_SIMPL-18180_AC4 @D13
  Scenario: A file without syntax errors passes the upload check
    When I open the UI as "ga-admin"
    And I go to "/vocabularies/new"
    And I attach the fixture "w3c_namespaces_only.ttl" as "Vocabulary file"
    Then I see the text "Syntax check passed."
    And the button "Submit" is enabled

  @D76 @D62
  Scenario: A file over 5 MB is refused in the browser
    When I open the UI as "ga-admin"
    And I go to "/vocabularies/new"
    And I attach a file of 5242881 bytes as "Vocabulary file"
    Then the field "Vocabulary file" shows the error "The file is larger than 5 MB."
    And the button "Submit" is disabled

  @AC_SIMPL-18180_AC2 @AC_SIMPL-18180_AC6 @AC_SIMPL-18180_AC7 @AC_SIMPL-18182_AC1 @AC_SIMPL-18182_AC3 @AC_SIMPL-18193_AC2 @D14 @D75
  Scenario: Semantic errors on submit appear in the validation report and the form stays open
    When I open the UI as "ga-admin"
    And I go to "/vocabularies/new"
    And I fill in "Name" with "${Sem}"
    And I fill in "Description" with "Semantic errors"
    And I attach the fixture "three_semantic_errors.ttl" as "Vocabulary file"
    And I click "Submit"
    Then the region "Validation report" lists 3 issues with detail and location
    And I am on the route "/vocabularies/new"

  @AC_SIMPL-18186_AC1 @AC_SIMPL-18193_AC1 @AC_SIMPL-18182_AC2 @AC_SIMPL-18180_AC4 @D6 @D14 @D32 @D72
  Scenario: A valid new vocabulary is published and its details page opens
    When I open the UI as "ga-admin"
    And I go to "/vocabularies/new"
    And I fill in "Name" with "${Fresh}"
    And I fill in "Description" with "Created in the UI"
    And I attach the fixture "w3c_namespaces_only.ttl" as "Vocabulary file"
    And I click "Submit"
    Then I am on the route "/vocabularies/${Fresh}"
    And I see the status message "Version 1 of ${Fresh} was created."
    And the header shows the name "${Fresh}" and the status "Active"
    And version 1 of "Fresh" has version status "ACTIVE"

  @D14 @D72 @D75
  Scenario: Warnings of a created version are listed after creation
    When I open the UI as "ga-admin"
    And I go to "/vocabularies/new"
    And I fill in "Name" with "${Warn}"
    And I fill in "Description" with "With an external reference"
    And I attach the fixture "external_reference.ttl" as "Vocabulary file"
    And I click "Submit"
    Then I see the status message "Version 1 of ${Warn} was created."
    And the region "Validation warnings" lists 1 warning with description and location

  # ---- new version (18172, 18180, 18184, 18189, 18191) --------------------------

  @AC_SIMPL-18172_AC1 @D33 @D46 @D74
  Scenario: Update opens a form pre-filled with the latest version
    Given a vocabulary "Alpha" exists with 2 versions
    When I open the UI as "ga-admin"
    And I go to "/vocabularies"
    And I open the actions menu "Actions for ${Alpha}"
    And I choose "Update"
    Then I am on the route "/vocabularies/${Alpha}/versions/new"
    And the field "Description" contains the description of version 2 of "Alpha"
    And the field "Vocabulary file" shows "${Alpha}.ttl (current content)"
    And the field "Change log" is empty
    And I see the text "Submitting creates version 3 and deprecates every older ACTIVE version."
    And the form has no field "Version"

  @AC_SIMPL-18172_AC2 @AC_SIMPL-18189_AC1 @AC_SIMPL-18189_AC2 @AC_SIMPL-18189_AC3 @AC_SIMPL-18191_AC1 @AC_SIMPL-18180_AC3 @D14 @D33 @D46
  Scenario: Submitting the update form creates the next version and deprecates the older ones
    Given a vocabulary "Alpha" exists
    When I open the UI as "ga-admin"
    And I go to "/vocabularies/${Alpha}/versions/new"
    And I fill in "Description" with "Second version"
    And I fill in "Change log" with "Changed the description"
    And I click "Submit"
    Then I see the status message "Version 2 of ${Alpha} was created."
    And version 2 of "Alpha" has version status "ACTIVE"
    And version 1 of "Alpha" has version status "DEPRECATED"

  @AC_SIMPL-18172_AC3 @D24 @D60
  Scenario Outline: An unexpected error on version creation advises to retry or contact support
    Given a vocabulary "Alpha" exists
    And the API is stubbed to answer <code> to POST "/vocabularies/${Alpha}/versions"
    When I open the UI as "ga-admin"
    And I go to "/vocabularies/${Alpha}/versions/new"
    And I fill in "Change log" with "Will fail"
    And I click "Submit"
    Then I see the alert "Something went wrong. Retry later or contact support."
    And the alert shows the problem detail

    Examples:
      | code |
      | 500  |
      | 503  |

  @AC_SIMPL-18184_AC4 @D11 @D14 @D47
  Scenario: Referencing a deprecated version shows a warning that names the latest ACTIVE version
    Given a vocabulary "Ref" exists with 2 versions
    And a vocabulary "Main" exists
    When I open the UI as "ga-admin"
    And I go to "/vocabularies/${Main}/versions/new"
    And I attach a file that references version 1 of "Ref" as "Vocabulary file"
    And I fill in "Change log" with "Uses Ref"
    And I click "Submit"
    Then the region "Validation warnings" shows "Referenced internal vocabulary '${Ref}' version 1 is deprecated. Use version 2, the latest ACTIVE version."

  # ---- status change (18174) --------------------------------------------------------

  @AC_SIMPL-18174_AC1 @D60 @D72
  Scenario: Deprecate and reactivate a vocabulary from the list
    Given a vocabulary "Alpha" exists
    When I open the UI as "ga-admin"
    And I go to "/vocabularies"
    And I open the actions menu "Actions for ${Alpha}"
    And I choose "Deprecate"
    Then I see the status message "Status of ${Alpha} changed to Deprecated."
    And the vocabulary table shows a row for "Alpha" with status "Deprecated"
    When I open the actions menu "Actions for ${Alpha}"
    And I choose "Activate"
    Then I see the status message "Status of ${Alpha} changed to Active."

  @AC_SIMPL-18174_AC2 @D24 @D72
  Scenario: A vocabulary that is no longer available is reported and nothing changes
    Given a vocabulary "Alpha" exists
    And the API is stubbed to answer 404 to PATCH "/vocabularies/${Alpha}"
    When I open the UI as "ga-admin"
    And I go to "/vocabularies"
    And I open the actions menu "Actions for ${Alpha}"
    And I choose "Deprecate"
    Then I see the alert "Vocabulary not found. The status was not changed."

  @AC_SIMPL-18174_AC3 @D24 @D60 @D72
  Scenario Outline: A failed status change keeps the shown status
    Given a vocabulary "Alpha" exists
    And the API is stubbed to answer <code> to PATCH "/vocabularies/${Alpha}"
    When I open the UI as "ga-admin"
    And I go to "/vocabularies"
    And I open the actions menu "Actions for ${Alpha}"
    And I choose "Deprecate"
    Then I see the alert "The vocabulary status could not be updated."
    And I see the alert "Something went wrong. Retry later or contact support."
    And the alert shows the problem detail
    And the vocabulary table shows a row for "Alpha" with status "Active"

    Examples:
      | code |
      | 500  |
      | 503  |

  # ---- access (18195) -------------------------------------------------------------

  @AC_SIMPL-18195_AC1 @D18
  Scenario: An admin sees the create, update and status actions
    Given a vocabulary "Alpha" exists
    When I open the UI as "ga-admin"
    And I go to "/vocabularies"
    Then I see the button "New Vocabulary +"
    When I open the actions menu "Actions for ${Alpha}"
    Then the menu offers "Update,Deprecate"

  @AC_SIMPL-18195_AC2 @D18
  Scenario Outline: Without the admin role restricted actions are hidden and their forms refuse access
    Given a vocabulary "Alpha" exists
    When I open the UI as "<user>"
    And I go to "/vocabularies"
    Then I do not see the button "New Vocabulary +"
    When I go to "/vocabularies/new"
    Then I see the text "You are not authorized to perform this action."
    When I go to "/vocabularies/${Alpha}/versions/new"
    Then I see the text "You are not authorized to perform this action."

    Examples:
      | user      |
      | ga-viewer |
      | ga-norole |

  @AC_SIMPL-18195_AC3 @D18
  Scenario: A user without a token is sent to the login page
    When I open the UI without being logged in
    And I go to "/vocabularies"
    Then I see the Keycloak login page

  # ---- accessibility (Q6, NFR-10) ---------------------------------------------------

  @Q6 @NFR-10 @a11y
  Scenario Outline: Pages have no axe violations and every action is reachable by keyboard
    Given a vocabulary "Alpha" exists with 2 versions
    When I open the UI as "ga-admin"
    And I go to "<route>"
    Then the page has no axe violations
    And every action on the page is reachable by keyboard

    Examples:
      | route                               |
      | /vocabularies                       |
      | /vocabularies/${Alpha}              |
      | /vocabularies/new                   |
      | /vocabularies/${Alpha}/versions/new |
