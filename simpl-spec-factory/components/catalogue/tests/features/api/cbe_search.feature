@api @cbe @bp06 @search
Feature: The client backend groups search results per catalogue
  cbe v1 GET /selfDescriptions and POST /selfDescriptions/advanced return the grouped shape of
  6.2 (D38): flat totalCount and items plus groups, paging per group, groups in the order of
  catalogueIds with the own catalogue first. Partial failure becomes a group status (D39).
  Missing catalogueIds means the own catalogue; local is its alias (D32). Quality filters
  apply to the own group only (D41).

  @SIMPL-31767 @SIMPL-31141 @D38 @D34
  Scenario: Quick search without catalogue ids searches the own catalogue
    Given "provider-a" has published a "DataOffering" resource description "own"
    When "consumer-a" sends GET "/selfDescriptions?q={token}" to "cbe-a-v1"
    Then the status is 200
    And the groups are "catalogue-a" in this order
    And the group "catalogue-a" contains "own"
    And the group "catalogue-a" has the field "catalogueName" set to "Data space A catalogue"
    And the group "catalogue-a" has the field "governanceAuthorityId" set to "ga-a"
    And the group "catalogue-a" has the field "authorityURL" null
    And the group "catalogue-a" has the field "local" set to "true"
    And the group "catalogue-a" has the field "status" set to "OK"
    And the group "catalogue-a" has the field "error" null
    And the group "catalogue-a" has the field "page" set to "1"
    And the group "catalogue-a" has the field "pageSize" set to "20"
    And the group "catalogue-a" has the field "total" set to "1"
    And the group "catalogue-a" has the field "totalPages" set to "1"
    And the group "catalogue-a" result "own" has the field "catalogueId" set to "catalogue-a"
    And the group "catalogue-a" result "own" has the field "name" set to "{token} own data"
    And the group "catalogue-a" result "own" has the field "offeringType" set to "data"
    And the group "catalogue-a" result "own" has the field "inLanguage" set to "en"
    And the group "catalogue-a" result "own" has the field "serviceAccessPoint" set to "https://provider.example.org/data"
    And the group "catalogue-a" result "own" has the field "qualityScore" set to "100"
    And the group "catalogue-a" result "own" has the field "qualityClassification" set to "Green"
    And the top-level totals and items add up over the groups

  @D32
  Scenario: The id local is an alias of the own catalogue
    Given "provider-a" has published a "DataOffering" resource description "own"
    When "consumer-a" sends GET "/selfDescriptions?q={token}&catalogueIds=local" to "cbe-a-v1"
    Then the status is 200
    And the groups are "catalogue-a" in this order
    And the group "catalogue-a" contains "own"

  @federation @SIMPL-31140 @SIMPL-31142 @SIMPL-31768 @D38 @D41
  Scenario: Own and federated results come back in separate groups
    Given "provider-a" has published a "DataOffering" resource description "own"
    And "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    When "consumer-a" sends GET "/selfDescriptions?q={token}&catalogueIds=local&catalogueIds=catalogue-b" to "cbe-a-v1"
    Then the status is 200
    And the groups are "catalogue-a, catalogue-b" in this order
    And the group "catalogue-a" contains "own"
    And the group "catalogue-a" does not contain "partner"
    And the group "catalogue-b" contains "partner"
    And the group "catalogue-b" does not contain "own"
    And the group "catalogue-b" has the field "catalogueName" set to "Data space B catalogue"
    And the group "catalogue-b" has the field "governanceAuthorityId" set to "ga-b"
    And the group "catalogue-b" has the field "authorityURL" set to "http://qma-b:8000/v1"
    And the group "catalogue-b" has the field "local" set to "false"
    And the group "catalogue-b" has the field "status" set to "OK"
    And the group "catalogue-b" result "partner" has the field "catalogueId" set to "catalogue-b"
    And the group "catalogue-b" result "partner" has the field "qualityScore" null
    And the group "catalogue-b" result "partner" has the field "qualityClassification" null
    And the group "catalogue-a" result "own" has the field "qualityScore" set to "100"
    And the top-level totals and items add up over the groups

  @federation @SIMPL-31140 @D38
  Scenario: The own catalogue group comes first, then the order as sent
    When "consumer-a" sends GET "/selfDescriptions?q={token}&catalogueIds=catalogue-b&catalogueIds=local" to "cbe-a-v1"
    Then the status is 200
    And the groups are "catalogue-a, catalogue-b" in this order

  @federation @SIMPL-31140 @D39
  Scenario: A Trustor that does not trust the GA gives an empty UNAUTHENTICATED group
    Given "provider-a" has published a "DataOffering" resource description "own"
    When "consumer-a" sends GET "/selfDescriptions?q={token}&catalogueIds=local&catalogueIds=catalogue-c" to "cbe-a-v1"
    Then the status is 200
    And the groups are "catalogue-a, catalogue-c" in this order
    And the group "catalogue-a" has the field "status" set to "OK"
    And the group "catalogue-a" contains "own"
    And the group "catalogue-c" has the field "status" set to "UNAUTHENTICATED"
    And the group "catalogue-c" has the field "error/type" set to "urn:problem-type:simpl:catalogue:federationUnauthenticated"
    And the group "catalogue-c" has the field "total" set to "0"
    And the group "catalogue-c" has 0 items
    And the top-level totals and items add up over the groups

  @federation @SIMPL-31140 @D39 @D40
  Scenario: An inactive catalogue gives an empty UNAVAILABLE group
    When "consumer-a" sends GET "/selfDescriptions?q={token}&catalogueIds=local&catalogueIds=catalogue-d" to "cbe-a-v1"
    Then the status is 200
    And the group "catalogue-d" has the field "status" set to "UNAVAILABLE"
    And the group "catalogue-d" has the field "error/type" set to "urn:problem-type:simpl:catalogue:federationUnavailable"
    And the group "catalogue-d" has 0 items

  @federation @SIMPL-31140 @D39 @slow
  Scenario: A slow catalogue gives an empty UNAVAILABLE group and the others are returned
    Given "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    When "consumer-a" sends GET "/selfDescriptions?q={token}&catalogueIds=catalogue-b&catalogueIds=catalogue-slow" to "cbe-a-v1"
    Then the status is 200
    And the group "catalogue-b" contains "partner"
    And the group "catalogue-slow" has the field "status" set to "UNAVAILABLE"
    And the group "catalogue-slow" has 0 items

  @federation @SIMPL-31140
  Scenario: A Trustor without matches gives an empty OK group
    When "consumer-a" sends GET "/selfDescriptions?q={token}&catalogueIds=catalogue-b" to "cbe-a-v1"
    Then the status is 200
    And the group "catalogue-b" has the field "status" set to "OK"
    And the group "catalogue-b" has the field "total" set to "0"
    And the group "catalogue-b" has 0 items

  @SIMPL-31140 @SIMPL-31142 @D38
  Scenario: Paging applies per group
    Given "provider-a" has published a "DataOffering" resource description "first"
    And "provider-a" has published a "DataOffering" resource description "second"
    And "provider-a" has published a "DataOffering" resource description "third"
    When "consumer-a" sends GET "/selfDescriptions?q={token}&catalogueIds=local&pageSize=2" to "cbe-a-v1"
    Then the status is 200
    And the group "catalogue-a" has 2 items
    And the group "catalogue-a" has the field "total" set to "3"
    And the group "catalogue-a" has the field "totalPages" set to "2"
    And the group "catalogue-a" has the field "pageSize" set to "2"
    And the group "catalogue-a" contains "third"
    And the group "catalogue-a" contains "second"
    When "consumer-a" sends GET "/selfDescriptions?q={token}&catalogueIds=local&pageSize=2&page=2" to "cbe-a-v1"
    Then the status is 200
    And the group "catalogue-a" has the field "page" set to "2"
    And the group "catalogue-a" has 1 items
    And the group "catalogue-a" contains "first"

  @federation @SIMPL-31140 @D38
  Scenario: Paging one group in a federated search uses the page size per group
    Given "provider-a" has published a "DataOffering" resource description "first"
    And "provider-a" has published a "DataOffering" resource description "second"
    And "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    When "consumer-a" sends GET "/selfDescriptions?q={token}&catalogueIds=local&catalogueIds=catalogue-b&pageSize=1" to "cbe-a-v1"
    Then the status is 200
    And the group "catalogue-a" has 1 items
    And the group "catalogue-a" has the field "total" set to "2"
    And the group "catalogue-b" has 1 items
    And the group "catalogue-b" has the field "total" set to "1"
    And the field "totalCount" is "3"

  @federation @SIMPL-29292 @SIMPL-31140 @D41 @quality
  Scenario: Quality filters apply to the own group only
    Given "provider-a" has published a "DataOffering" resource description "green"
    And a "DataOffering" resource description "yellow" by "provider-a"
    And the quality stub scores "yellow" with "overall=75"
    And "provider-a" has published "yellow"
    And "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    When "consumer-a" sends GET "/selfDescriptions?q={token}&catalogueIds=local&catalogueIds=catalogue-b&minimumScore=80" to "cbe-a-v1"
    Then the status is 200
    And the group "catalogue-a" contains "green"
    And the group "catalogue-a" does not contain "yellow"
    And the group "catalogue-b" contains "partner"

  @SIMPL-29292 @D54 @quality
  Scenario Outline: Quick search through cbe filters on quality (<filter>)
    Given "provider-a" has published a "DataOffering" resource description "green"
    And a "DataOffering" resource description "yellow" by "provider-a"
    And the quality stub scores "yellow" with "overall=75"
    And "provider-a" has published "yellow"
    When "consumer-a" sends GET "/selfDescriptions?q={token}&<filter>" to "cbe-a-v1"
    Then the status is 200
    And the group "catalogue-a" <green> "green"
    And the group "catalogue-a" <yellow> "yellow"

    Examples:
      | filter                                     | green            | yellow           |
      | minimumScore=80                            | contains         | does not contain |
      | minimumClassification=Yellow               | contains         | contains         |
      | minimumClassification=Green                | contains         | does not contain |
      | minimumScore=&minimumClassification=       | contains         | contains         |
      | minimumScore=70&minimumClassification=Green | contains        | does not contain |

  @D28 @D54 @D40
  Scenario Outline: Invalid search input fails the whole request with 400 (<case>)
    When "consumer-a" sends GET "/selfDescriptions?<query>" to "cbe-a-v1"
    Then the response is a problem with status 400

    Examples:
      | case                  | query                                          |
      | missing q             | catalogueIds=local                             |
      | empty q               | q=                                             |
      | bad characters        | q=%3Cscript%3E                                 |
      | score above 100       | q={token}&minimumScore=101                     |
      | negative score        | q={token}&minimumScore=-1                      |
      | score not a number    | q={token}&minimumScore=high                    |
      | unknown class         | q={token}&minimumClassification=Platinum       |
      | unknown catalogue     | q={token}&catalogueIds=catalogue-zz            |
      | page zero             | q={token}&page=0                               |
      | page size zero        | q={token}&pageSize=0                           |

  # ------------------------------------------------------------------ advanced search

  @SIMPL-31767 @SIMPL-31141 @D38 @D29
  Scenario: Advanced search through cbe on the own catalogue
    Given "provider-a" has published a "DataOffering" resource description "own"
    When "consumer-a" sends POST "/selfDescriptions/advanced" to "cbe-a-v1" with the fixture "cbe-local-name-token"
    Then the status is 200
    And the groups are "catalogue-a" in this order
    And the group "catalogue-a" contains "own"
    And the group "catalogue-a" result "own" has the field "qualityScore" set to "100"

  @D32
  Scenario: Advanced search without catalogue ids searches the own catalogue
    Given "provider-a" has published a "DataOffering" resource description "own"
    When "consumer-a" sends POST "/selfDescriptions/advanced" to "cbe-a-v1" with the fixture "name-token"
    Then the status is 200
    And the groups are "catalogue-a" in this order
    And the group "catalogue-a" contains "own"

  @federation @SIMPL-31142 @SIMPL-31768 @D38
  Scenario: Advanced search through cbe on own and federated catalogues
    Given "provider-a" has published a "DataOffering" resource description "own"
    And "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    When "consumer-a" sends POST "/selfDescriptions/advanced" to "cbe-a-v1" with the fixture "cbe-local-b-name-token"
    Then the status is 200
    And the groups are "catalogue-a, catalogue-b" in this order
    And the group "catalogue-a" contains "own"
    And the group "catalogue-b" contains "partner"
    And the group "catalogue-b" result "partner" has the field "qualityScore" null
    And the top-level totals and items add up over the groups

  @federation @SIMPL-31140 @D39
  Scenario: Advanced search to an untrusting Trustor gives an UNAUTHENTICATED group
    When "consumer-a" sends POST "/selfDescriptions/advanced" to "cbe-a-v1" with the fixture "cbe-local-c-name-token"
    Then the status is 200
    And the group "catalogue-c" has the field "status" set to "UNAUTHENTICATED"
    And the group "catalogue-a" has the field "status" set to "OK"

  @federation @SIMPL-29293 @D41 @quality
  Scenario: Advanced quality filters apply to the own group only
    Given "provider-a" has published a "DataOffering" resource description "green"
    And a "DataOffering" resource description "yellow" by "provider-a"
    And the quality stub scores "yellow" with "overall=75"
    And "provider-a" has published "yellow"
    And "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    When "consumer-a" sends POST "/selfDescriptions/advanced" to "cbe-a-v1" with the fixture "cbe-local-b-min-score-80"
    Then the status is 200
    And the group "catalogue-a" contains "green"
    And the group "catalogue-a" does not contain "yellow"
    And the group "catalogue-b" contains "partner"

  @D40
  Scenario: Advanced search with an unknown catalogue id is 400
    When "consumer-a" sends POST "/selfDescriptions/advanced" to "cbe-a-v1" with the fixture "cbe-unknown-name-token"
    Then the response is a problem with status 400

  @D29
  Scenario: Advanced search with an empty body is 400
    When "consumer-a" sends POST "/selfDescriptions/advanced" to "cbe-a-v1"
    Then the response is a problem with status 400

  @D29
  Scenario: Advanced search with an empty node is 400
    When "consumer-a" sends POST "/selfDescriptions/advanced" to "cbe-a-v1" with the fixture "empty-node"
    Then the response is a problem with status 400

  @rbac @D66
  Scenario Outline: cbe search needs a searcher role (<who>)
    When "<who>" sends GET "/selfDescriptions?q={token}" to "cbe-a-v1"
    Then the status is <status>

    Examples:
      | who             | status |
      | anonymous       | 401    |
      | an invalid token| 401    |
      | nobody-a        | 403    |
      | consumer-a      | 200    |
