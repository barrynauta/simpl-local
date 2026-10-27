@ui
Feature: Vocabulary details, history and download in the UI

  Background:
    Given I am signed in to the UI as "admin"

  @SIMPL-18167
  Scenario: The history shows all versions in order
    Given vocabulary "{Alpha}" has 3 versions
    When I open the history of "{Alpha}"
    Then I am on the details page of "{Alpha}" with its name in the header
    And I see 3 versions in chronological order, each with: download action, version number, status, date created, description, change log

  @SIMPL-18176 @SIMPL-18559 @D25
  Scenario: A deprecated version is labelled and points to the active version
    Given vocabulary "{Alpha}" has 2 versions
    When I open the history of "{Alpha}"
    Then version 1 is labelled "Deprecated" with a clear warning
    And the warning links to version 2, the latest active version
    When I open version 1
    Then I see its metadata: version, date created, creator, change log

  @SIMPL-18559
  Scenario: A deprecated version without any active version still shows the warning
    Given vocabulary "{Alpha}" has 2 versions
    And vocabulary "{Alpha}" is deprecated
    When I open the history of "{Alpha}"
    Then every version shows a clear deprecation warning
    And no link to an active version is shown

  @SIMPL-18165 @D24
  Scenario: Download the newest version
    Given vocabulary "{Alpha}" has 2 versions
    When I open the Vocabulary Management page
    And I choose the download action for "{Alpha}"
    Then a Turtle file of version 2 is downloaded

  @SIMPL-18452 @D24
  Scenario: Download a selected older version
    Given vocabulary "{Alpha}" has 2 versions
    When I open the history of "{Alpha}"
    And I download version 1
    Then a Turtle file of version 1 is downloaded
