Feature: UI access control, accessibility and security headers
  Restricted actions D18, login D18, accessibility Q6, Content-Security-Policy D123.
  Gherkin only, except the CSP header scenario, which uses HTTP steps; step 60 runs all @ui.

  @ui @AC_SIMPL-18195_AC1 @D16 @D18
  Scenario: The admin sees every management action
    Given a vocabulary "Alpha" exists
    And I am signed in to the UI as "ga-admin"
    When I open the UI page "/vocabularies"
    And I fill in "Name contains" with "{tok}"
    Then I see the button "New Vocabulary +"
    When I open the actions menu "Actions for {Alpha}"
    Then I see the text "Update"
    And I see the text "Deprecate"

  @ui @AC_SIMPL-18195_AC2 @AC_SIMPL-18176_AC1 @D16 @D18
  Scenario: The viewer does not see the restricted actions but can read
    Given a vocabulary "Alpha" exists
    And I am signed in to the UI as "ga-viewer"
    When I open the UI page "/vocabularies"
    And I fill in "Name contains" with "{tok}"
    Then I do not see the button "New Vocabulary +"
    When I open the actions menu "Actions for {Alpha}"
    Then I see the text "View vocabulary details"
    And I see the text "Download vocabulary"
    And I do not see the text "Update"
    And I do not see the text "Deprecate"

  @ui @AC_SIMPL-18195_AC2 @D18 @D74 @negative
  Scenario Outline: Opening a form without the admin role shows the authorisation message: <route>
    Given a vocabulary "Alpha" exists
    And I am signed in to the UI as "<user>"
    When I open the UI page "<route>"
    Then I see an alert containing "You are not authorized to perform this action."

    Examples:
      | user      | route                               |
      | ga-viewer | /vocabularies/new                   |
      | ga-viewer | /vocabularies/{Alpha}/versions/new  |
      | ga-norole | /vocabularies/new                   |

  @ui @AC_SIMPL-18195_AC2 @D18 @negative
  Scenario: A 403 from the API shows the authorisation message
    Given a vocabulary "Alpha" exists
    And I am signed in to the UI as "ga-admin"
    And the API answers PATCH "/vocabularies/{Alpha}" with the status 403
    When I open the UI page "/vocabularies"
    And I fill in "Name contains" with "{tok}"
    And I open the actions menu "Actions for {Alpha}"
    And I choose the menu item "Deprecate"
    Then I see an alert containing "You are not authorized to perform this action."

  @ui @AC_SIMPL-18195_AC3 @D18 @negative
  Scenario: A user without a session is sent to the identity provider login
    Given I am not signed in to the UI
    When I open the UI page "/vocabularies"
    Then I am on the identity provider login page

  @ui @Q6 @NFR-10
  Scenario Outline: No accessibility violations on <page>
    Given a vocabulary "Alpha" exists with 2 versions
    And I am signed in to the UI as "ga-admin"
    When I open the UI page "<route>"
    Then there are no axe violations on the page

    Examples:
      | page                 | route                              |
      | the vocabulary list  | /vocabularies                      |
      | the details page     | /vocabularies/{Alpha}              |
      | the create form      | /vocabularies/new                  |
      | the new version form | /vocabularies/{Alpha}/versions/new |

  @ui @Q6 @NFR-10
  Scenario: Every action on the list is reachable by keyboard
    Given a vocabulary "Alpha" exists
    And I am signed in to the UI as "ga-admin"
    When I open the UI page "/vocabularies"
    And I press Tab until every action has had the focus
    Then every action had the focus

  @D123
  Scenario: The UI sends the decided Content-Security-Policy
    When I request the UI start page
    Then the response header "Content-Security-Policy" contains "default-src 'self'"
    And the response header "Content-Security-Policy" contains "frame-ancestors 'none'"
    And the Content-Security-Policy allows connections to 'self' and the identity provider
    And the response header "Content-Security-Policy" does not contain "unsafe-inline"
