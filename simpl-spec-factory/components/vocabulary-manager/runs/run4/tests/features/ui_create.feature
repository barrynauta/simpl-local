Feature: Create a vocabulary and a new version in the UI
  Forms: D13, D14, D32, D33, D37, D46. Server rules are the same as the API.

  @ui @AC_SIMPL-18169_AC1 @D32
  Scenario: The creation form has name, description and file, but no version or change log
    Given I sign in to the UI as "ga-admin"
    When I open the vocabulary list
    And I click "New Vocabulary +"
    Then the form shows the fields "Name, Description, File upload"
    And the form does not show the fields "Version, Change log"

  @ui @AC_SIMPL-18169_AC2 @D37 @negative
  Scenario Outline: The creation form reports invalid input (<case>)
    Given a vocabulary "Taken" exists
    And I sign in to the UI as "ga-admin"
    When I open the vocabulary list
    And I click "New Vocabulary +"
    And I fill in "Name" with "<name>"
    And I fill in "Description" with "<description>"
    And I choose the file "<file>"
    And I submit the form
    Then I see "<message>"
    And the form stays open

    Examples:
      | case             | name          | description | file            | message                                     |
      | name format      | lower         | A desc      | valid-skos.ttl  | Name                                        |
      | name not unique  | ${name:Taken} | A desc      | valid-skos.ttl  | Vocabulary with name '${name:Taken}' already exists. |
      | unsupported file | ${run}New     | A desc      | not-turtle.json | line 1                                      |

  @ui @AC_SIMPL-18186_AC1 @AC_SIMPL-18189_AC3 @D6 @D14 @D32
  Scenario: A valid new vocabulary is published as ACTIVE version 1
    Given I sign in to the UI as "ga-admin"
    When I open the vocabulary list
    And I click "New Vocabulary +"
    And I fill in "Name" with "${run}Fresh"
    And I fill in "Description" with "A new vocabulary."
    And I choose the file "valid-skos.ttl"
    And I submit the form
    Then I see "Version 1 of ${run}Fresh was created."
    And the row of vocabulary "Fresh" shows "Active"

  @ui @AC_SIMPL-18172_AC1 @D33
  Scenario: The update form is pre-filled with the latest version
    Given a vocabulary "Health" exists with:
      | field       | value             |
      | description | Current wording.  |
    And I sign in to the UI as "ga-admin"
    When I open the vocabulary list
    And I click "Update" for vocabulary "Health"
    Then the field "Description" holds "Current wording." in the form
    And the file "${name:Health}.ttl (current content)" is attached in the form
    And the field "Change log" is empty in the form
    And I see "Submitting creates version 2 and deprecates every older ACTIVE version."

  @ui @AC_SIMPL-18172_AC2 @AC_SIMPL-18189_AC1 @AC_SIMPL-18189_AC2 @AC_SIMPL-18189_AC3 @AC_SIMPL-18191_AC1 @D4 @D7 @D14 @D33
  Scenario: Submitting the update form creates the next version and deprecates the older ones
    Given a vocabulary "Health" exists
    And I sign in to the UI as "ga-admin"
    When I open the vocabulary list
    And I click "Update" for vocabulary "Health"
    Then the form does not show the fields "Version"
    When I fill in "Change log" with "Clarified labels."
    And I choose the file "cpsv-like.ttl"
    And I submit the form
    Then I see "Version 2 of ${name:Health} was created."
    When I open the details of vocabulary "Health"
    Then the row of version 2 shows "Active"
    And the row of version 1 shows "Deprecated"

  @ui @D33 @D46
  Scenario: A description-only change creates a new version
    Given a vocabulary "Health" exists
    And I sign in to the UI as "ga-admin"
    When I open the vocabulary list
    And I click "Update" for vocabulary "Health"
    And I fill in "Description" with "Only the description changed."
    And I fill in "Change log" with "Description reworded."
    And I submit the form
    Then I see "Version 2 of ${name:Health} was created."

  @ui @D46 @negative
  Scenario: Resubmitting the latest version unchanged shows the conflict above the form
    Given a vocabulary "Health" exists
    And I sign in to the UI as "ga-admin"
    When I open the vocabulary list
    And I click "Update" for vocabulary "Health"
    And I fill in "Change log" with "Nothing really changed."
    And I submit the form
    Then I see "Version 1 of vocabulary '${name:Health}' already has this content and description."
    And the form stays open

  @ui @AC_SIMPL-18172_AC3 @D24 @D44 @negative
  Scenario Outline: An unexpected error during version creation is reported (<code>)
    Given a vocabulary "Health" exists
    And the API is stubbed to answer <code> to "createVocabularyVersion"
    And I sign in to the UI as "ga-admin"
    When I open the vocabulary list
    And I click "Update" for vocabulary "Health"
    And I fill in "Change log" with "Will fail."
    And I choose the file "cpsv-like.ttl"
    And I submit the form
    Then I see "retry or contact support"
    And the form stays open

    Examples:
      | code |
      | 500  |
      | 503  |

  @ui @D15 @negative
  Scenario: A file over 5 MB is refused in the browser
    Given I sign in to the UI as "ga-admin"
    When I open the vocabulary list
    And I click "New Vocabulary +"
    And I choose the file "<bytes:5242881>"
    Then I see "The maximum allowed upload size is 5 MB"
    And the button "Submit" is disabled
