Feature: Accessibility of the main UI pages
  One axe-core check (WCAG 2.1 A and AA rules) per main page. Violations are listed in
  the failure message and in reports/a11y-<page>.json.

  @ui @a11y
  Scenario Outline: The <page> page has no WCAG 2.1 AA violations
    Given a vocabulary "Health" exists with 2 versions
    And I sign in to the UI as "ga-admin"
    When I open the <page> page for the accessibility check
    Then the page has no WCAG 2.1 AA violations

    Examples:
      | page               |
      | vocabulary list    |
      | vocabulary details |
      | creation form      |
      | new version form   |
