Feature: Aggregated audit data
  CM produces the number of usage contracts per status at a given moment
  through an endpoint (SIMPL-34633, D62, A.6).

  @SIMPL-34633 @D62
  Scenario: SIMPL-34633 AC1 - the audit counts list every status with its count
    When I send "GET" to the provider CM at "/audit/contractStatusCounts"
    Then the response status is 200
    And the response field "participantId" is "${providerId}"
    And the response field "generatedAt" is a date-time
    And the response counts every contract status
    And the response field "total" equals the sum of the counts

  @SIMPL-34633 @D62 @D81 @serial
  Scenario: SIMPL-34633 AC1 - a new usage contract is counted
    Given I remember the provider contract status counts
    And a contract requested at the provider
    Then the provider count of "REQUESTED" has grown by at least 1

  @SIMPL-34633 @D62 @D65 @rbac
  Scenario: The audit counts need the API key
    When I send "GET" to the provider CM at "/audit/contractStatusCounts" without an API key
    Then the response is a problem with status 401 and type "unauthorized"
