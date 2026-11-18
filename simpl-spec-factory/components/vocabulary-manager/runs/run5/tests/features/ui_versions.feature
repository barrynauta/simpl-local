Feature: Create a new version of a vocabulary in the UI
  The 'Update' action opens a form pre-filled with the latest version (D33); submitting
  creates the next version and deprecates every older ACTIVE version (D4, D46).

  @ui @AC_SIMPL-18172_AC1 @D33 @D46 @D60
  Scenario: The update form is pre-filled with the latest version
    Given a vocabulary "{Alpha}" exists with 2 versions
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    When I open the UI page "/vocabularies"
    And I click "Update" in the actions menu of "{Alpha}"
    Then the field "Description" contains "Description 1 of run {run}."
    And I see the text "{Alpha}.ttl (current content)"
    And the field "Change log" is empty
    And I see the text "Submitting creates version 3 and deprecates every older ACTIVE version."
    And I do not see the form fields ["Version"]

  @ui @AC_SIMPL-18172_AC2 @AC_SIMPL-18189_AC1 @AC_SIMPL-18189_AC2 @AC_SIMPL-18189_AC3 @AC_SIMPL-18191_AC1 @AC_SIMPL-18180_AC3 @D4 @D7 @D14
  Scenario: Submitting a new file and change log creates the next version
    Given a vocabulary "{Alpha}" exists
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    When I open the UI page "/vocabularies"
    And I click "Update" in the actions menu of "{Alpha}"
    And I fill in "Change log" with "Added public services."
    And I choose the fixture "cpsv-like.ttl" as the vocabulary file
    And I submit
    Then a success notification says "Version 2 of {Alpha} was created."
    And version 2 of "{Alpha}" has status "ACTIVE"
    And version 1 of "{Alpha}" has status "DEPRECATED"

  @ui @AC_SIMPL-18172_AC2 @D33
  Scenario: Changing only the description creates a new version with the current content
    Given a vocabulary "{Alpha}" exists
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    When I open the UI page "/vocabularies"
    And I click "Update" in the actions menu of "{Alpha}"
    And I fill in "Description" with "Better words."
    And I fill in "Change log" with "Description only."
    And I submit
    Then a success notification says "Version 2 of {Alpha} was created."
    And version 2 of "{Alpha}" has the field "description" equal to "Better words."

  @ui @D46 @D37 @negative
  Scenario: Submitting the latest version unchanged shows the conflict above the form
    Given a vocabulary "{Alpha}" exists
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    When I open the UI page "/vocabularies"
    And I click "Update" in the actions menu of "{Alpha}"
    And I fill in "Change log" with "No real change."
    And I submit
    Then an error message says "Version 1 of vocabulary '{Alpha}' already has this content and description."
    And the vocabulary "{Alpha}" has 1 version

  @ui @AC_SIMPL-18172_AC3 @D24 @D44 @D60 @negative
  Scenario Outline: An unexpected error on submit creates no version and advises a retry (<status>)
    Given a vocabulary "{Alpha}" exists
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    And the API answers "POST" "/vocabularies/{Alpha}/versions" with status <status> for the UI
    When I open the UI page "/vocabularies"
    And I click "Update" in the actions menu of "{Alpha}"
    And I fill in "Change log" with "Will fail."
    And I choose the fixture "cpsv-like.ttl" as the vocabulary file
    And I submit
    Then an error message says "Something went wrong. Retry later or contact support."
    And the vocabulary "{Alpha}" has 1 version

    Examples:
      | status |
      | 500    |
      | 503    |

  @ui @AC_SIMPL-18184_AC4 @D11 @D14 @D47
  Scenario: A reference to a deprecated version shows a warning that names the latest ACTIVE version
    Given a vocabulary "{Base}" exists with 2 versions
    And a vocabulary "{Alpha}" exists
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    When I open the UI page "/vocabularies"
    And I click "Update" in the actions menu of "{Alpha}"
    And I fill in "Change log" with "Uses a deprecated version."
    And I choose the fixture "references-base-v1.ttl" as the vocabulary file
    And I submit
    Then a success notification says "Version 2 of {Alpha} was created."
    And the warnings panel lists "Referenced internal vocabulary '{Base}' version 1 is deprecated. Use version 2, the latest ACTIVE version."
