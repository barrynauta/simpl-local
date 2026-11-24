Feature: Accessibility of the UI (Q6, NFR-10)
  Browser steps in features/steps/ui_steps.py (step 60); axe runs with the CSP bypass of D123.
  The last two rows cover the external vocabularies screen of D210, a main page added after step 40.

  @ui @a11y @Q6 @NFR-10 @D123
  Scenario Outline: No axe violations on <page>
    Given a vocabulary "$Apple" exists
    And I am logged in to the UI as "ga-admin"
    When I open the route "<route>"
    Then axe reports no WCAG 2.1 AA violations

    Examples:
      | page                  | route                             |
      | the vocabulary list   | /vocabularies                     |
      | the vocabulary details | /vocabularies/$Apple             |
      | the creation form     | /vocabularies/new                 |
      | the new version form  | /vocabularies/$Apple/versions/new |
      | the external vocabularies screen | /external-vocabularies  |
      | the external registration form   | /external-vocabularies/new |

  @ui @Q6 @NFR-10 @D209
  Scenario: Every action on the list is reachable by keyboard
    Given a vocabulary "$Apple" exists
    And I am logged in to the UI as "ga-admin"
    When I open the route "/vocabularies"
    And I filter "Name contains" with "$Apple"
    Then I can reach "New Vocabulary +", the sort buttons and "Actions for $Apple" with the Tab key
    And I can open "Actions for $Apple" and choose "Deprecate" with the keyboard
    And the dialog "Update vocabulary status" can be cancelled with the Escape key
