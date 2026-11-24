Feature: External vocabularies screen in the UI
  Browser steps in features/steps/ui_steps.py (step 60). Decided by D210 (no story).
  The screen has no filter (D41, D210) and the store is never reset (E4): every step that looks for a
  row of this scenario presses 'Next page' until the row appears, and order checks compare the
  positions across pages.

  @ui @D210 @D41
  Scenario: The screen lists the registered external vocabularies in name order
    Given an external vocabulary "$ExtB" is registered with namespace "$EXT/b"
    And an external vocabulary "$ExtA" is registered with namespace "$EXT/a"
    And I am logged in to the UI as "ga-viewer"
    When I open the menu item "External vocabularies"
    Then I am on the route "/external-vocabularies"
    And the table has the column headers "Name, Namespace URI, Alias URIs, Description, Created by, Date created"
    And "$ExtA" is listed before "$ExtB", paging with "Next page" where needed
    And the row of "$ExtA", found by paging, has the action "Download"

  @ui @D210 @D18
  Scenario: A viewer does not see register or remove
    Given an external vocabulary "$Ext" is registered with namespace "$EXT/v"
    And I am logged in to the UI as "ga-viewer"
    When I open the route "/external-vocabularies"
    Then I do not see the button "Register external vocabulary +"
    And the row of "$Ext", found by paging, has no action "Remove"

  @ui @D210 @D30 @D56
  Scenario: The admin registers an external vocabulary
    Given I am logged in to the UI as "ga-admin"
    When I open the route "/external-vocabularies"
    And I press the button "Register external vocabulary +"
    Then I am on the route "/external-vocabularies/new"
    When I fill "Name" with "$Ext" and "Namespace URI" with "$EXT/reg"
    And I fill "Alias URIs" with two lines "$EXT/a1" and "$EXT/a2"
    And I choose a valid Turtle file for "Vocabulary file"
    And I press the button "Submit"
    Then a status message shows "External vocabulary $Ext was registered."

  @ui @D210 @D209
  Scenario: The admin removes an external vocabulary after confirming
    Given an external vocabulary "$Ext" is registered with namespace "$EXT/rm"
    And I am logged in to the UI as "ga-admin"
    When I open the route "/external-vocabularies"
    And I page with "Next page" until the row of "$Ext" appears
    And I choose "Remove" for "$Ext"
    Then a dialog asks "Do you want to remove '$Ext'?" with focus on "Cancel"
    When I press the button "Confirm"
    Then a status message shows "External vocabulary $Ext was removed."
    And the external vocabulary "$Ext" is not registered

  @ui @D210 @D161
  Scenario: A namespace conflict is shown above the form
    Given an external vocabulary "$Ext" is registered with namespace "$EXT/taken"
    And I am logged in to the UI as "ga-admin"
    When I open the route "/external-vocabularies/new"
    And I fill "Name" with "$Other" and "Namespace URI" with "$EXT/taken"
    And I choose a valid Turtle file for "Vocabulary file"
    And I press the button "Submit"
    Then one alert shows "An external vocabulary with namespace or alias '$EXT/taken' is already registered."
