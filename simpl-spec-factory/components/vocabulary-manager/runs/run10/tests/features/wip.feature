@wip
Feature: Criteria and targets that cannot be checked from the API
  Kept as scenarios so they stay visible in the reports; they never run in ./run.sh api.

  @AC_SIMPL-18162_AC4 @D24 @negative
  Scenario: An internal error while listing gives 500 with the problem detail and no list
    # A black-box suite cannot force an internal error that is not a store outage (that is 503, D44).
    Given the user is not signed in to the UI
    Then the user sees the text "not testable from outside"

  @D92 @negative
  Scenario: A fifth simultaneous upload on one instance gets 503 Too many uploads
    # Needs four uploads held open on the same instance at once; with two instances behind the proxy (D102)
    # and fast uploads the outcome depends on timing, so it is not checked here.
    Given the user is not signed in to the UI
    Then the user sees the text "timing dependent"

  @Q3 @NFR-06
  Scenario: Backup and restore round trip
    # Q3 and D89 are verified by the build's own scripts in step 70, not through the API.
    Given the user is not signed in to the UI
    Then the user sees the text "verified by step 70"
