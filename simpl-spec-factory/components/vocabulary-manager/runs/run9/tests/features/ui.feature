@ui
Feature: Vocabulary Management UI
  Gherkin only. Step 60 adds the browser steps ('the user ...', 'the UI ...'); the data set-up steps
  use the API. Labels and texts are the decided ones (D60, D72, D75). Error cases stub the API (D24).

  @AC_SIMPL-18161_AC1 @D31 @D34 @D72 @D75
  Scenario: The vocabulary list shows every vocabulary with its columns and actions
    Given a vocabulary "Health" exists
    And the user is signed in as "ga-viewer"
    When the user opens the vocabulary list filtered on "${run}"
    Then the UI shows a table with the column headers "Name, Description, Latest version, Status, Date created, Last updated, Creator"
    And the UI shows a row for "${Health}" with the version "1" and the status "Active"
    And the UI shows the button "Actions for ${Health}" with the items "View vocabulary details, History, Download vocabulary"

  @AC_SIMPL-18161_AC2 @D34 @D72 @D24 @negative
  Scenario Outline: A failure to load the list is shown with the problem detail (<status>)
    Given the user is signed in as "ga-viewer"
    And the UI API stub answers <status> on "GET /vocabularies" with the detail "Stubbed failure"
    When the user opens the vocabulary list
    Then the UI shows an alert containing "The vocabulary list could not be loaded."
    And the UI shows an alert containing "Stubbed failure"

    Examples:
      | status |
      | 500    |
      | 503    |

  @D34
  Scenario: An empty list says that no vocabularies were found
    Given the user is signed in as "ga-viewer"
    When the user filters the vocabulary list on the name "${run}"
    Then the UI shows the text "No vocabularies found."

  @AC_SIMPL-18163_AC1 @D19 @D72
  Scenario: Filtering the list by status and by name
    Given a vocabulary "Alpha" exists
    And a vocabulary "Beta" exists
    And the vocabulary "Beta" was set to "DEPRECATED"
    And the user is signed in as "ga-viewer"
    When the user filters the vocabulary list on the name "${run}"
    And the user chooses "Deprecated" in the filter "Vocabulary status"
    Then the UI shows only the rows "${Beta}"

  @AC_SIMPL-18163_AC2 @D23 @D50
  Scenario: Clicking sortable column headers sorts ascending and reverses the active sort
    Given a vocabulary "Alpha" exists
    And a vocabulary "Beta" exists
    And the user is signed in as "ga-viewer"
    When the user filters the vocabulary list on the name "${run}"
    And the user clicks the column header "Name"
    Then the UI shows the rows in the order "${Beta}, ${Alpha}"
    When the user clicks the column header "Date created"
    Then the UI shows the rows in the order "${Alpha}, ${Beta}"

  @AC_SIMPL-18165_AC1 @D29 @D35
  Scenario: Downloading the newest version from the list and reviewing its details
    Given a vocabulary "Health" exists with 2 versions
    And the user is signed in as "ga-viewer"
    When the user filters the vocabulary list on the name "${Health}"
    And the user chooses "Download vocabulary" in the menu "Actions for ${Health}"
    Then the UI downloads the file "${Health}.ttl"
    When the user chooses "View vocabulary details" in the menu "Actions for ${Health}"
    Then the UI shows the details page of "${Health}" with the latest version "2"

  @AC_SIMPL-18167_AC1 @AC_SIMPL-18176_AC2 @D35 @D72
  Scenario: The history opens the details page with all versions newest first
    Given a vocabulary "Health" exists with 3 versions
    And the user is signed in as "ga-viewer"
    When the user filters the vocabulary list on the name "${Health}"
    And the user chooses "History" in the menu "Actions for ${Health}"
    Then the UI shows the route "/vocabularies/${Health}"
    And the UI shows the heading "${Health}"
    And the UI shows a table with the column headers "Version, Status, Date created, Description, Change log, Actions"
    And the UI shows the versions in the order "3, 2, 1"
    And the UI shows the status "Deprecated" for the versions "2, 1"

  @AC_SIMPL-18176_AC1 @D16 @D35
  Scenario: A viewer may see deprecated versions
    Given a vocabulary "Health" exists with 2 versions
    And the user is signed in as "ga-viewer"
    When the user opens the details page of "${Health}"
    Then the UI shows the versions in the order "2, 1"

  @AC_SIMPL-18176_AC3 @AC_SIMPL-18452_AC1 @D35 @D75
  Scenario: Selecting a version shows all its metadata and a download
    Given a vocabulary "Health" exists with 2 versions
    And the user is signed in as "ga-viewer"
    When the user opens the details page of "${Health}"
    And the user selects the version "1"
    Then the UI shows the region "Version 1" with "Version, Status, Date created, Creator, Description, Change log"
    And the UI shows the button "Download" in the region "Version 1"

  @AC_SIMPL-18452_AC2 @D29
  Scenario: Downloading a selected version gives the file of that version
    Given a vocabulary "Health" exists with 2 versions
    And the user is signed in as "ga-viewer"
    When the user opens the details page of "${Health}"
    And the user chooses "Download" in the menu "Actions for version 1"
    Then the UI downloads the file "${Health}-v1.ttl"

  @D35 @negative
  Scenario: A details page of an unknown vocabulary says it was not found
    Given the user is signed in as "ga-viewer"
    When the user opens the details page of "${Ghost}"
    Then the UI shows an alert "Vocabulary not found."
    And the UI shows the link "Back to vocabulary list"

  @AC_SIMPL-18169_AC1 @D32 @D60 @D74
  Scenario: The creation form has name, description and file, and no version or change log field
    Given the user is signed in as "ga-admin"
    When the user clicks the button "New Vocabulary +"
    Then the UI shows the route "/vocabularies/new"
    And the UI shows the fields "Name, Description, Vocabulary file"
    And the UI shows no field "Change log"
    And the UI shows no field "Version"

  @AC_SIMPL-18169_AC2 @D37 @D62 @negative
  Scenario: Field errors are shown under the fields and nothing is sent
    Given the user is signed in as "ga-admin"
    When the user opens the creation form
    And the user fills "Name" with "lower${run}"
    And the user clicks the button "Submit"
    Then the UI sent no request "POST /vocabularies"
    And the UI shows a field error under "Name"
    And the UI shows a field error under "Description"
    And the UI shows a field error under "Vocabulary file"
    And the UI has the focus on the field "Name"
    And no vocabulary of this scenario is stored

  @AC_SIMPL-18169_AC2 @D37 @D15 @negative
  Scenario: A duplicate name is reported above the form
    Given a vocabulary "Twin" exists
    And the user is signed in as "ga-admin"
    When the user creates a vocabulary named "${Twin}" with the description "Again" and the file "valid"
    Then the UI shows an alert containing "Vocabulary with name '${Twin}' already exists."

  @AC_SIMPL-18169_AC2 @D76 @D62 @negative
  Scenario: A file over 5 MB is refused in the browser
    Given the user is signed in as "ga-admin"
    When the user opens the creation form
    And the user attaches a file of 5242881 bytes to "Vocabulary file"
    Then the UI shows the field error "The file is larger than 5 MB." under "Vocabulary file"
    And the UI has the button "Submit" disabled

  @AC_SIMPL-18186_AC1 @AC_SIMPL-18193_AC1 @D14 @D72
  Scenario: A valid new vocabulary is published as Active and the details page opens
    Given the user is signed in as "ga-admin"
    When the user creates a vocabulary named "${Fresh}" with the description "Fresh" and the file "valid"
    Then the UI shows the route "/vocabularies/${Fresh}"
    And the UI shows a status message "Version 1 of ${Fresh} was created."
    And the UI shows the status "Active" for the versions "1"

  @AC_SIMPL-18172_AC1 @D33 @D46 @D74
  Scenario: The Update form is pre-filled with the latest version
    Given a vocabulary "Health" exists with the description "Current text."
    And the user is signed in as "ga-admin"
    When the user filters the vocabulary list on the name "${Health}"
    And the user chooses "Update" in the menu "Actions for ${Health}"
    Then the UI shows the route "/vocabularies/${Health}/versions/new"
    And the UI shows the field "Description" with "Current text."
    And the UI shows the text "${Health}.ttl (current content)"
    And the UI shows the field "Change log" empty
    And the UI shows the text "Submitting creates version 2 and deprecates every older ACTIVE version."

  @AC_SIMPL-18172_AC2 @AC_SIMPL-18189_AC2 @AC_SIMPL-18189_AC3 @AC_SIMPL-18191_AC1 @D14 @D33
  Scenario: Submitting the Update form creates the next version and confirms its number
    Given a vocabulary "Health" exists
    And the user is signed in as "ga-admin"
    When the user filters the vocabulary list on the name "${Health}"
    And the user chooses "Update" in the menu "Actions for ${Health}"
    And the user fills "Description" with "New text."
    And the user fills "Change log" with "Changed the text."
    And the user clicks the button "Submit"
    Then the UI shows a status message "Version 2 of ${Health} was created."
    When the user opens the details page of "${Health}"
    Then the UI shows the status "Active" for the versions "2"
    And the UI shows the status "Deprecated" for the versions "1"

  @AC_SIMPL-18189_AC1 @D7
  Scenario: The new version gets the next number without manual input
    Given a vocabulary "Health" exists with 2 versions
    And the user is signed in as "ga-admin"
    When the user creates a new version of "${Health}" with the change log "Third." and the file "valid"
    Then the UI shows a status message "Version 3 of ${Health} was created."

  @AC_SIMPL-18172_AC3 @D24 @D60 @negative
  Scenario Outline: An unexpected error during version creation advises to retry or contact support (<status>)
    Given a vocabulary "Health" exists
    And the user is signed in as "ga-admin"
    And the UI API stub answers <status> on "POST /vocabularies/${Health}/versions" with the detail "Stubbed failure"
    When the user creates a new version of "${Health}" with the change log "Change." and the file "valid"
    Then the UI shows an alert containing "Something went wrong. Retry later or contact support."
    And the UI shows an alert containing "Stubbed failure"

    Examples:
      | status |
      | 500    |
      | 503    |

  @AC_SIMPL-18174_AC1 @D60 @D72
  Scenario: Deprecating and reactivating a vocabulary from the list
    Given a vocabulary "Health" exists
    And the user is signed in as "ga-admin"
    When the user filters the vocabulary list on the name "${Health}"
    And the user chooses "Deprecate" in the menu "Actions for ${Health}"
    Then the UI shows a status message "Status of ${Health} changed to Deprecated."
    And the UI shows a row for "${Health}" with the version "1" and the status "Deprecated"
    When the user chooses "Activate" in the menu "Actions for ${Health}"
    Then the UI shows a status message "Status of ${Health} changed to Active."

  @AC_SIMPL-18174_AC2 @D24 @D72 @negative
  Scenario: Changing the status of a vocabulary that is no longer available
    Given a vocabulary "Health" exists
    And the user is signed in as "ga-admin"
    And the UI API stub answers 404 on "PATCH /vocabularies/${Health}" with the detail "Not found"
    When the user filters the vocabulary list on the name "${Health}"
    And the user chooses "Deprecate" in the menu "Actions for ${Health}"
    Then the UI shows an alert "Vocabulary not found. The status was not changed."
    And the UI shows a row for "${Health}" with the version "1" and the status "Active"

  @AC_SIMPL-18174_AC3 @D24 @D72 @negative
  Scenario: A failed status change leaves the status unchanged
    Given a vocabulary "Health" exists
    And the user is signed in as "ga-admin"
    And the UI API stub answers 500 on "PATCH /vocabularies/${Health}" with the detail "Stubbed failure"
    When the user filters the vocabulary list on the name "${Health}"
    And the user chooses "Deprecate" in the menu "Actions for ${Health}"
    Then the UI shows an alert containing "The vocabulary status could not be updated."
    And the UI shows an alert containing "Stubbed failure"
    And the UI shows a row for "${Health}" with the version "1" and the status "Active"

  @AC_SIMPL-18178_AC1 @D36 @D72
  Scenario: A deprecated vocabulary shows a warning in the list and on its details page
    Given a vocabulary "Health" exists
    And the vocabulary "Health" was set to "DEPRECATED"
    And the user is signed in as "ga-viewer"
    When the user filters the vocabulary list on the name "${Health}"
    Then the UI shows a warning icon named "This vocabulary is deprecated. Its use is discouraged." in the row "${Health}"
    When the user opens the details page of "${Health}"
    Then the UI shows the banner "This vocabulary is deprecated. Its use is discouraged."

  @AC_SIMPL-18559_AC1 @D36 @D64
  Scenario: A deprecated version points to the latest ACTIVE version
    Given a vocabulary "Health" exists with 2 versions
    And the user is signed in as "ga-viewer"
    When the user opens the details page of "${Health}"
    Then the UI shows for the version "1" the warning "This version is deprecated."
    And the UI shows for the version "1" the link "Latest ACTIVE version: 2"
    And the UI shows for the version "2" no warning

  @AC_SIMPL-18559_AC2 @D36 @D64
  Scenario: A deprecated version without any ACTIVE version shows only the warning
    Given a vocabulary "Health" exists
    And version 1 of "Health" was set to "DEPRECATED"
    And the user is signed in as "ga-viewer"
    When the user opens the details page of "${Health}"
    Then the UI shows for the version "1" the warning "This version is deprecated."
    And the UI shows for the version "1" no link "Latest ACTIVE version"

  @D64 @D36
  Scenario: A version of a DEPRECATED vocabulary recommends no version
    Given a vocabulary "Health" exists with 2 versions
    And the vocabulary "Health" was set to "DEPRECATED"
    And the user is signed in as "ga-viewer"
    When the user opens the details page of "${Health}"
    Then the UI shows for the version "1" the warning "This version is deprecated."
    And the UI shows for the version "1" no link "Latest ACTIVE version"

  @AC_SIMPL-18180_AC1 @AC_SIMPL-18180_AC4 @D13
  Scenario: Choosing a valid file runs the syntax check in the browser
    Given the user is signed in as "ga-admin"
    When the user opens the creation form
    And the user attaches the file "valid" to "Vocabulary file"
    Then the UI shows the text "Syntax check passed."
    And the UI has the button "Submit" enabled

  @AC_SIMPL-18180_AC1 @AC_SIMPL-18180_AC5 @AC_SIMPL-18180_AC8 @AC_SIMPL-18193_AC2 @D13 @D60 @D62 @negative
  Scenario: Choosing a file with a syntax error shows the line and blocks submit
    Given the user is signed in as "ga-admin"
    When the user opens the creation form
    And the user attaches the file "syntax-error.ttl" to "Vocabulary file"
    Then the UI shows a field error containing "line 3" under "Vocabulary file"
    And the UI has the button "Submit" disabled

  @AC_SIMPL-18180_AC2 @AC_SIMPL-18180_AC6 @AC_SIMPL-18180_AC7 @AC_SIMPL-18182_AC1 @AC_SIMPL-18182_AC3 @AC_SIMPL-18193_AC2 @D9 @D14 @D90 @negative
  Scenario: Semantic errors on submit of a new vocabulary appear in the validation report
    Given the user is signed in as "ga-admin"
    When the user creates a vocabulary named "${Sem}" with the description "Semantic errors" and the file "multi-semantic.ttl"
    Then the UI shows the region "Validation report" with an alert "3 errors found"
    And the UI shows in the region "Validation report" the texts "Literal value for object property, Duplicate preferred label for the same language, Resource is an instance of disjoint classes"
    And the UI shows the route "/vocabularies/new"

  @AC_SIMPL-18180_AC3 @AC_SIMPL-18182_AC1 @AC_SIMPL-18182_AC3 @AC_SIMPL-18193_AC2 @D9 @D14 @negative
  Scenario: Semantic errors on submit of a new version appear in the validation report
    Given a vocabulary "Health" exists
    And the user is signed in as "ga-admin"
    When the user creates a new version of "${Health}" with the change log "Broken." and the file "s5-disjoint.ttl"
    Then the UI shows the region "Validation report" with an alert "1 errors found"
    And the UI shows in the region "Validation report" the texts "Resource is an instance of disjoint classes"

  @AC_SIMPL-18182_AC2 @AC_SIMPL-18193_AC1 @D14
  Scenario: A semantically valid submit succeeds without a validation report
    Given the user is signed in as "ga-admin"
    When the user creates a vocabulary named "${Fine}" with the description "Valid" and the file "valid"
    Then the UI shows a status message "Version 1 of ${Fine} was created."
    And the UI shows no region "Validation report"

  @AC_SIMPL-18184_AC4 @D11 @D14 @D47 @D90
  Scenario: A reference to a deprecated version shows a warning that recommends the latest ACTIVE version
    Given a vocabulary "Ref" exists with 2 versions
    And a vocabulary "Health" exists
    And the user is signed in as "ga-admin"
    When the user creates a new version of "${Health}" referencing version 1 of "${Ref}"
    Then the UI shows a status message "Version 2 of ${Health} was created."
    And the UI shows in the region "Validation warnings" the texts "Referenced internal vocabulary '${Ref}' version 1 is deprecated. Use version 2, the latest ACTIVE version."

  @AC_SIMPL-18195_AC1 @D16 @D18
  Scenario: An admin sees all vocabulary management actions
    Given a vocabulary "Health" exists
    And the user is signed in as "ga-admin"
    When the user filters the vocabulary list on the name "${Health}"
    Then the UI shows the button "New Vocabulary +"
    And the UI shows the button "Actions for ${Health}" with the items "Update, Deprecate"

  @AC_SIMPL-18195_AC2 @D16 @D18 @negative
  Scenario: A viewer does not see restricted actions and is refused on the form URL
    Given a vocabulary "Health" exists
    And the user is signed in as "ga-viewer"
    When the user filters the vocabulary list on the name "${Health}"
    Then the UI shows no button "New Vocabulary +"
    And the UI shows the button "Actions for ${Health}" without the items "Update, Deprecate, Activate"
    When the user opens the route "/vocabularies/new"
    Then the UI shows an alert "You are not authorized to perform this action."

  @AC_SIMPL-18195_AC2 @D18 @negative
  Scenario: Any 403 from the API shows the authorisation message
    Given a vocabulary "Health" exists
    And the user is signed in as "ga-admin"
    And the UI API stub answers 403 on "PATCH /vocabularies/${Health}" with the detail "Forbidden"
    When the user filters the vocabulary list on the name "${Health}"
    And the user chooses "Deprecate" in the menu "Actions for ${Health}"
    Then the UI shows an alert "You are not authorized to perform this action."

  @AC_SIMPL-18195_AC3 @D18 @negative
  Scenario: A user without a token is sent to the Keycloak login page
    Given the user is not signed in
    When the user opens the route "/vocabularies"
    Then the UI shows the Keycloak login page

  @Q6 @NFR-10
  Scenario Outline: The <page> has no axe violations (WCAG 2.1 AA)
    Given a vocabulary "Health" exists
    And the user is signed in as "ga-admin"
    When the user opens the route "<route>"
    Then the UI has no axe violations at level WCAG 2.1 AA

    Examples:
      | page              | route                                |
      | vocabulary list   | /vocabularies                        |
      | details page      | /vocabularies/${Health}              |
      | creation form     | /vocabularies/new                    |
      | new version form  | /vocabularies/${Health}/versions/new |

  @D35 @D72 @D24 @negative
  Scenario Outline: A failure to load the version history is shown with the problem detail (<status>)
    Given a vocabulary "Health" exists
    And the user is signed in as "ga-viewer"
    And the UI API stub answers <status> on "GET /vocabularies/${Health}/versions" with the detail "Stubbed failure"
    When the user opens the details page of "${Health}"
    Then the UI shows an alert containing "The version history could not be loaded."
    And the UI shows an alert containing "Stubbed failure"

    Examples:
      | status |
      | 500    |
      | 503    |

  @D18 @D74 @negative
  Scenario: A viewer who opens the new version form URL is refused
    Given a vocabulary "Health" exists
    And the user is signed in as "ga-viewer"
    When the user opens the route "/vocabularies/${Health}/versions/new"
    Then the UI shows an alert "You are not authorized to perform this action."

  @D60
  Scenario: Dates are shown in UTC and the menu has the item Vocabularies
    Given a vocabulary "Health" exists
    And the user is signed in as "ga-viewer"
    When the user filters the vocabulary list on the name "${Health}"
    Then the UI shows the menu item "Vocabularies"
    And the UI shows in the row "${Health}" a date matching "^\d{4}-\d{2}-\d{2} \d{2}:\d{2} UTC$" under "Date created"

  @D34
  Scenario: The list pages on the server with 20 rows per page
    Given 21 vocabularies of this scenario exist
    And the user is signed in as "ga-viewer"
    When the user filters the vocabulary list on the name "${run}"
    Then the UI shows 20 rows
    And the UI shows the page controls
    When the user goes to the next page
    Then the UI shows 1 rows

  @Q6 @NFR-10
  Scenario: Every action is reachable by keyboard
    Given a vocabulary "Health" exists
    And the user is signed in as "ga-admin"
    When the user filters the vocabulary list on the name "${Health}"
    Then the UI lets the user reach with the keyboard only "New Vocabulary +, Actions for ${Health}, Update, Deprecate, View vocabulary details, History, Download vocabulary"
