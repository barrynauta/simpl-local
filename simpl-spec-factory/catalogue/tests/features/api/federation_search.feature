@api @qma @federation @bp06
Feature: Federated search across data spaces A, B and C
  Topology of D35: A is the Trusted GA, B a Trustor that trusts A, C trusts nobody,
  catalogue-slow answers after 10 s, catalogue-d is inactive. GA-to-GA authentication with
  client-credentials tokens (D36). Attribute mapping at B: CONSUMER becomes PARTNER_CONSUMER,
  FEDERATED is added, other codes are dropped (D37, 6.3). Single hop (D44). Federated
  results carry no quality (D41).

  # ------------------------------------------------------------------ Trustor side: qma-b inbound

  @SIMPL-31138 @SIMPL-31114 @SIMPL-31106 @D37 @D7
  Scenario: The Trustor answers a federated quick search with the mapped attributes
    Given "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    And "provider-b" has published a "DataOffering" resource description "federated" discoverable by "FEDERATED"
    And "provider-b" has published a "DataOffering" resource description "consumer" discoverable by "CONSUMER"
    And "provider-b" has published a "DataOffering" resource description "researcher" discoverable by "RESEARCHER"
    And the federated request headers for "CONSUMER,RESEARCHER"
    When "client qma-a-federation" sends GET "/federated/selfDescriptions?catalogueIds=catalogue-b&searchString={token}" to "qma-b"
    Then the status is 200
    And the result contains exactly "partner, federated" of this scenario
    And the response header "Correlation-Id" is "{correlation}"
    And the field "totalCount" equals the field "total"

  @SIMPL-31140 @D41
  Scenario: The Trustor returns no quality on federated requests
    Given "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    And the federated request headers for "CONSUMER"
    When "client qma-a-federation" sends GET "/federated/selfDescriptions?catalogueIds=catalogue-b&searchString={token}" to "qma-b"
    Then the status is 200
    And the result "partner" has the field "overallScore" null
    And the result "partner" has the field "overallClassification" null
    And the result "partner" has the field "name" set to "{token} partner data"

  @SIMPL-31138 @D37
  Scenario: An empty attribute header leaves only the additional attributes
    Given "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    And "provider-b" has published a "DataOffering" resource description "federated" discoverable by "FEDERATED"
    And the federated request headers for ""
    When "client qma-a-federation" sends GET "/federated/selfDescriptions?catalogueIds=catalogue-b&searchString={token}" to "qma-b"
    Then the status is 200
    And the result contains exactly "federated" of this scenario

  @SIMPL-31138 @D44
  Scenario: The Trustor searches only its own catalogue
    Given "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    And the federated request headers for "CONSUMER"
    When "client qma-a-federation" sends GET "/federated/selfDescriptions?catalogueIds=catalogue-a&searchString={token}" to "qma-b"
    Then the status is 200
    And the field "totalCount" is "0"
    And the field "items" is an empty list

  @SIMPL-31138
  Scenario: A federated quick search without matches returns an empty list
    Given the federated request headers for "CONSUMER"
    When "client qma-a-federation" sends GET "/federated/selfDescriptions?catalogueIds=catalogue-b&searchString={token}" to "qma-b"
    Then the status is 200
    And the field "totalCount" is "0"
    And the field "items" is an empty list

  @SIMPL-31139 @SIMPL-31114 @D37
  Scenario: The Trustor answers a federated advanced search with the mapped attributes
    Given "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    And "provider-b" has published a "DataOffering" resource description "consumer" discoverable by "CONSUMER"
    And the federated request headers for "CONSUMER"
    When "client qma-a-federation" sends POST "/federated/selfDescriptions/advancedSearch" to "qma-b" with the fixture "trustor-b-name-token"
    Then the status is 200
    And the result contains exactly "partner" of this scenario
    And the response header "Correlation-Id" is "{correlation}"

  @SIMPL-31139 @D44
  Scenario: The Trustor advanced search ignores other catalogue ids
    Given "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    And the federated request headers for "CONSUMER"
    When "client qma-a-federation" sends POST "/federated/selfDescriptions/advancedSearch" to "qma-b" with the fixture "trustor-other-name-token"
    Then the status is 200
    And the field "totalCount" is "0"

  @SIMPL-31139 @D9
  Scenario: A federated advanced search body that is not JSON is 415
    Given the federated request headers for "CONSUMER"
    When "client qma-a-federation" sends POST "/federated/selfDescriptions/advancedSearch" to "qma-b" with the content type "text/plain"
      """
      name={token}
      """
    Then the response is a problem "unsupportedMediaType" with status 415

  @rbac @SIMPL-31138 @D36
  Scenario Outline: The Trustor rejects quick searches from callers that are not a trusted GA (<who>)
    Given the federated request headers for "CONSUMER"
    When "<who>" sends GET "/federated/selfDescriptions?catalogueIds=catalogue-b&searchString={token}" to "qma-b"
    Then the response is a problem with status <status>

    Examples:
      | who                     | status |
      | anonymous               | 401    |
      | an invalid token        | 401    |
      | client qma-c-federation | 403    |
      | consumer-a              | 403    |
      | consumer-b              | 403    |

  @rbac @SIMPL-31139 @D36
  Scenario Outline: The Trustor rejects advanced searches from callers that are not a trusted GA (<who>)
    Given the federated request headers for "CONSUMER"
    When "<who>" sends POST "/federated/selfDescriptions/advancedSearch" to "qma-b" with the fixture "trustor-b-name-token"
    Then the response is a problem with status <status>

    Examples:
      | who                     | status |
      | anonymous               | 401    |
      | client qma-c-federation | 403    |
      | consumer-a              | 403    |

  @SIMPL-31138 @D37 @D48
  Scenario Outline: A federated search without a required header is 400 (<missing>)
    Given the federated request headers for "CONSUMER"
    And the next request has no header "<missing>"
    When "client qma-a-federation" sends GET "/federated/selfDescriptions?catalogueIds=catalogue-b&searchString={token}" to "qma-b"
    Then the response is a problem with status 400

    Examples:
      | missing         |
      | User-Attributes |
      | Correlation-Id  |

  @SIMPL-31138
  Scenario: A federated quick search without catalogue ids is 400
    Given the federated request headers for "CONSUMER"
    When "client qma-a-federation" sends GET "/federated/selfDescriptions?searchString={token}" to "qma-b"
    Then the response is a problem with status 400

  # ------------------------------------------------------------------ Trusted side: qma-a forwards

  @SIMPL-31113 @SIMPL-31768 @SIMPL-31114 @D37
  Scenario: The Trusted GA forwards a quick search with the Consumer's attributes
    Given "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    And "provider-b" has published a "DataOffering" resource description "consumer" discoverable by "CONSUMER"
    And "provider-b" has published a "DataOffering" resource description "federated" discoverable by "FEDERATED"
    When "consumer-a" sends GET "/governanceAuthority/selfDescriptions?catalogueIds=catalogue-b&searchString={token}" to "qma-a"
    Then the status is 200
    And the result contains exactly "partner, federated" of this scenario
    And the result "partner" has the field "overallScore" null
    And the result "partner" has the field "name" set to "{token} partner data"

  @rbac @SIMPL-31113 @D37
  Scenario: The mapping depends on the Consumer's own attributes
    Given "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    And "provider-b" has published a "DataOffering" resource description "federated" discoverable by "FEDERATED"
    When "provider-a" sends GET "/governanceAuthority/selfDescriptions?catalogueIds=catalogue-b&searchString={token}" to "qma-a"
    Then the status is 200
    And the result contains exactly "federated" of this scenario

  @SIMPL-31113 @SIMPL-31767 @SIMPL-31768 @D39
  Scenario: Own and federated catalogues in one call are concatenated and the totals summed
    Given "provider-a" has published a "DataOffering" resource description "own"
    And "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    When "consumer-a" sends GET "/governanceAuthority/selfDescriptions?catalogueIds=catalogue-a&catalogueIds=catalogue-b&searchString={token}" to "qma-a"
    Then the status is 200
    And the result contains exactly "own, partner" of this scenario
    And the field "total" is "2"
    And the result "own" has the field "overallScore" set to "100"
    And the result "partner" has the field "overallScore" null

  @SIMPL-31113 @D40
  Scenario: The reserved id local selects the own catalogue
    Given "provider-a" has published a "DataOffering" resource description "own"
    When "consumer-a" sends GET "/governanceAuthority/selfDescriptions?catalogueIds=local&searchString={token}" to "qma-a"
    Then the status is 200
    And the result contains exactly "own" of this scenario

  @SIMPL-31140 @D41 @quality
  Scenario: Quality filters are not forwarded to the Trustor
    Given "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    When "consumer-a" sends GET "/governanceAuthority/selfDescriptions?catalogueIds=catalogue-b&searchString={token}&minimumScore=90" to "qma-a"
    Then the status is 200
    And the result contains exactly "partner" of this scenario

  @SIMPL-31113 @D36 @D39
  Scenario: A Trustor that does not trust the GA gives 502
    When "consumer-a" sends GET "/governanceAuthority/selfDescriptions?catalogueIds=catalogue-c&searchString={token}" to "qma-a"
    Then the response is a problem "catalogue:federationUnauthenticated" with status 502

  @SIMPL-31113 @D40 @D39
  Scenario: An inactive catalogue is not called and gives 502
    When "consumer-a" sends GET "/governanceAuthority/selfDescriptions?catalogueIds=catalogue-d&searchString={token}" to "qma-a"
    Then the response is a problem "catalogue:federationUnavailable" with status 502

  @SIMPL-31113 @D39 @D47 @slow
  Scenario: A slow Trustor gives 504 after the federation timeout
    When "consumer-a" sends GET "/governanceAuthority/selfDescriptions?catalogueIds=catalogue-slow&searchString={token}" to "qma-a"
    Then the response is a problem "catalogue:federationTimeout" with status 504
    And the request took less than 8 seconds

  @SIMPL-31113 @SIMPL-31140 @D39
  Scenario: With several catalogues the failed ones are skipped and named
    Given "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    When "consumer-a" sends GET "/governanceAuthority/selfDescriptions?catalogueIds=catalogue-b&catalogueIds=catalogue-c&catalogueIds=catalogue-d&searchString={token}" to "qma-a"
    Then the status is 200
    And the result contains exactly "partner" of this scenario
    And the response header "Simpl-Failed-Catalogues" contains "catalogue-c"
    And the response header "Simpl-Failed-Catalogues" contains "catalogue-d"
    And the response header "Simpl-Failed-Catalogues" does not contain "catalogue-b"

  @SIMPL-31113 @D40
  Scenario: A catalogue id that is not configured is 400
    When "consumer-a" sends GET "/governanceAuthority/selfDescriptions?catalogueIds=catalogue-zz&searchString={token}" to "qma-a"
    Then the response is a problem "catalogue:invalidInput" with status 400

  @SIMPL-31113
  Scenario Outline: An invalid catalogue selection is 400 with the contract message (<case>)
    When "consumer-a" sends GET "<path>" to "qma-a"
    Then the response is a problem "catalogue:invalidInput" with status 400
    And the problem detail is "<detail>"

    Examples:
      | case     | path                                                              | detail                                                                              |
      | missing  | /governanceAuthority/selfDescriptions?searchString={token}        | Required query parameter 'catalogueIds' is missing; select at least one catalogue. |
      | empty id | /governanceAuthority/selfDescriptions?catalogueIds=&searchString={token} | Each catalogueIds value must be a non-empty string.                          |

  @SIMPL-31113
  Scenario Outline: Other invalid input on the GA quick search is 400 (<case>)
    When "consumer-a" sends GET "/governanceAuthority/selfDescriptions?catalogueIds=catalogue-b&<query>" to "qma-a"
    Then the response is a problem "catalogue:invalidInput" with status 400
    And the problem detail is "<detail>"

    Examples:
      | case          | query                                    | detail                                                                                                       |
      | search string | searchString=%3Cscript%3E                | searchString is required, must not be blank, must be at most 255 characters, and must contain only permitted characters. |
      | score         | searchString={token}&minimumScore=-1     | minimumScore must be greater than or equal to 0.                                                             |
      | paging        | searchString={token}&page=0              | page and pageSize must be integers greater than 0.                                                           |

  @SIMPL-31139 @SIMPL-31113 @D6
  Scenario: The Trusted GA forwards an advanced search
    Given "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    And "provider-b" has published a "DataOffering" resource description "consumer" discoverable by "CONSUMER"
    When "consumer-a" sends POST "/governanceAuthority/selfDescriptions/advancedSearch" to "qma-a" with the fixture "fed-b-name-token"
    Then the status is 200
    And the result contains exactly "partner" of this scenario

  @SIMPL-31139 @D6 @D39
  Scenario: The Trusted GA advanced search over own and federated catalogues
    Given "provider-a" has published a "DataOffering" resource description "own"
    And "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    When "consumer-a" sends POST "/governanceAuthority/selfDescriptions/advancedSearch" to "qma-a" with the fixture "fed-a-b-name-token"
    Then the status is 200
    And the result contains exactly "own, partner" of this scenario

  @SIMPL-31139 @D6 @D39
  Scenario: The Trusted GA advanced search to an untrusting Trustor gives 502
    When "consumer-a" sends POST "/governanceAuthority/selfDescriptions/advancedSearch" to "qma-a" with the fixture "fed-c-name-token"
    Then the response is a problem "catalogue:federationUnauthenticated" with status 502

  @SIMPL-31139 @D6 @D40
  Scenario: The Trusted GA advanced search with an unknown catalogue id is 400
    When "consumer-a" sends POST "/governanceAuthority/selfDescriptions/advancedSearch" to "qma-a" with the fixture "fed-unknown-name-token"
    Then the response is a problem with status 400

  @rbac @D66
  Scenario Outline: The GA search needs a searcher role (<who>)
    When "<who>" sends GET "/governanceAuthority/selfDescriptions?catalogueIds=local&searchString={token}" to "qma-a"
    Then the status is <status>

    Examples:
      | who        | status |
      | anonymous  | 401    |
      | nobody-a   | 403    |
      | consumer-a | 200    |
