@api @qma @bp06
Feature: The query mapper adapter is the Governance Authority entry point for local search
  qma validates the request, resolves the caller's identity attributes from the token and calls
  catalogue-be (D4, D28, D69). Local advanced search searches only when the own catalogue id is
  selected (D32). Paging per D31. Participant operations per D16, D19, D21.

  # ------------------------------------------------------------------ quick search

  @SIMPL-31111 @SIMPL-31767 @D31 @D34
  Scenario: Local quick search returns the main properties and paging fields
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    When "consumer-a" sends GET "/selfDescriptions?searchString={token}" to "qma-a"
    Then the status is 200
    And the field "totalCount" is "1"
    And the field "total" is "1"
    And the field "page" is "1"
    And the field "pageSize" is "20"
    And the field "totalPages" is "1"
    And the field "next" is null
    And the result "rd1" has the field "claimsGraphUri" as the list "{rd1.id}"
    And the result "rd1" has the field "name" set to "{token} rd1 data"
    And the result "rd1" has the field "offeringType" set to "data"
    And the result "rd1" has the field "serviceAccessPoint" set to "https://provider.example.org/data"
    And the result "rd1" has the field "overallScore" set to "100"
    And the result "rd1" has the field "overallClassification" set to "Green"

  @D31
  Scenario: Local quick search pages with a relative next link
    Given "provider-a" has published a "DataOffering" resource description "first"
    And "provider-a" has published a "DataOffering" resource description "second"
    And "provider-a" has published a "DataOffering" resource description "third"
    When "consumer-a" sends GET "/selfDescriptions?searchString={token}&page=1&pageSize=2" to "qma-a"
    Then the status is 200
    And the field "totalCount" is "3"
    And the field "total" is "3"
    And the field "totalPages" is "2"
    And the field "pageSize" is "2"
    And the result lists "third, second" in this order
    And the field "next" matches "/selfDescriptions\?.*"
    And the field "next" contains the text "page=2"
    And the field "next" contains the text "searchString={token}"
    When "consumer-a" sends GET "/selfDescriptions?searchString={token}&page=2&pageSize=2" to "qma-a"
    Then the status is 200
    And the field "page" is "2"
    And the field "next" is null
    And the result contains exactly "first" of this scenario

  @D31
  Scenario: A page size above 100 is capped at 100
    When "consumer-a" sends GET "/selfDescriptions?searchString={token}&pageSize=500" to "qma-a"
    Then the status is 200
    And the field "pageSize" is "100"

  @D28
  Scenario Outline: An invalid search string is 400 (<case>)
    When "consumer-a" sends GET "/selfDescriptions?searchString=<value>" to "qma-a"
    Then the response is a problem with status 400

    Examples:
      | case               | value                 |
      | forbidden chars    | %3Cscript%3E          |
      | only blanks        | %20%20%20             |
      | empty              |                       |
      | too long           | {a256}                |

  @D28
  Scenario: A missing search string is 400
    When "consumer-a" sends GET "/selfDescriptions" to "qma-a"
    Then the response is a problem with status 400

  @D28
  Scenario: A search string with the permitted punctuation is accepted
    When "consumer-a" sends GET "/selfDescriptions?searchString={token}%20a.b,c'd-e_f%26g%40h(i)%2Bj%2Fk" to "qma-a"
    Then the status is 200

  @D31
  Scenario Outline: Invalid paging is 400 (<query>)
    When "consumer-a" sends GET "/selfDescriptions?searchString={token}&<query>" to "qma-a"
    Then the response is a problem with status 400

    Examples:
      | query        |
      | page=0       |
      | pageSize=0   |
      | pageSize=abc |

  @SIMPL-31111 @D30
  Scenario: Local quick search applies the discovery policy with the token attributes
    Given "provider-a" has published a "DataOffering" resource description "open" discoverable by "CONSUMER"
    And "provider-a" has published a "DataOffering" resource description "research" discoverable by "RESEARCHER"
    And "provider-a" has published a "DataOffering" resource description "other" discoverable by "OTHER"
    When "consumer-a" sends GET "/selfDescriptions?searchString={token}" to "qma-a"
    Then the status is 200
    And the result contains exactly "open, research" of this scenario

  @rbac @D69
  Scenario: qma ignores an incoming User-Attributes header on local search
    Given "provider-a" has published a "DataOffering" resource description "open" discoverable by "CONSUMER"
    And "provider-a" has published a "DataOffering" resource description "other" discoverable by "OTHER"
    And the next request has the header "User-Attributes" set to "OTHER"
    When "consumer-a" sends GET "/selfDescriptions?searchString={token}" to "qma-a"
    Then the status is 200
    And the result contains exactly "open" of this scenario

  @SIMPL-29292 @D54 @quality
  Scenario Outline: Local quick search filters on quality (<filter>)
    Given "provider-a" has published a "DataOffering" resource description "green"
    And a "DataOffering" resource description "yellow" by "provider-a"
    And the quality stub scores "yellow" with "overall=75"
    And "provider-a" has published "yellow"
    When "consumer-a" sends GET "/selfDescriptions?searchString={token}&<filter>" to "qma-a"
    Then the status is 200
    And the result contains exactly "<expected>" of this scenario

    Examples:
      | filter                       | expected      |
      | minimumScore=80              | green         |
      | minimumClassification=Yellow | green, yellow |
      | minimumClassification=Green  | green         |

  @SIMPL-29292 @D54
  Scenario Outline: Invalid quality filters on local quick search are 400 (<filter>)
    When "consumer-a" sends GET "/selfDescriptions?searchString={token}&<filter>" to "qma-a"
    Then the response is a problem "catalogue:invalidInput" with status 400
    And the problem detail is "<detail>"

    Examples:
      | filter                         | detail                                                                  |
      | minimumScore=-1                | minimumScore must be greater than or equal to 0.                       |
      | minimumClassification=Platinum | minimumClassification is not a supported quality classification value. |

  # ------------------------------------------------------------------ advanced search

  @SIMPL-31112 @SIMPL-31767 @D29 @D32
  Scenario: Local advanced search with the own catalogue id
    Given "provider-a" has published a "DataOffering" resource description "data1"
    And "provider-a" has published a "ApplicationOffering" resource description "app1"
    When "consumer-a" sends POST "/selfDescriptions/advancedSearch" to "qma-a" with the fixture "name-token-local"
    Then the status is 200
    And the result contains exactly "data1, app1" of this scenario
    And the field "page" is "1"
    And the field "total" is "2"

  @D32
  Scenario Outline: Local advanced search without the own catalogue id finds nothing (<fixture>)
    Given "provider-a" has published a "DataOffering" resource description "data1"
    When "consumer-a" sends POST "/selfDescriptions/advancedSearch" to "qma-a" with the fixture "<fixture>"
    Then the status is 200
    And the field "totalCount" is "0"
    And the field "items" is an empty list

    Examples:
      | fixture                     |
      | name-token-other-catalogue  |
      | name-token-no-catalogue     |

  @SIMPL-29293 @D54 @quality
  Scenario: Local advanced search filters on quality
    Given "provider-a" has published a "DataOffering" resource description "green"
    And a "DataOffering" resource description "yellow" by "provider-a"
    And the quality stub scores "yellow" with "overall=75"
    And "provider-a" has published "yellow"
    When "consumer-a" sends POST "/selfDescriptions/advancedSearch" to "qma-a" with the fixture "name-token-local-min-score-80"
    Then the status is 200
    And the result contains exactly "green" of this scenario

  @D29
  Scenario: An empty node on local advanced search is 400 with the contract message
    When "consumer-a" sends POST "/selfDescriptions/advancedSearch" to "qma-a" with the fixture "empty-node-local"
    Then the response is a problem "badRequest" with status 400
    And the problem detail is "No Properties defined in simpl:ContractTemplate node. Please provide at least one property with searched value."

  @D29
  Scenario: Wrong range keys on local advanced search are 400 with the contract message
    When "consumer-a" sends POST "/selfDescriptions/advancedSearch" to "qma-a" with the fixture "wrong-range-keys-local"
    Then the response is a problem "badRequest" with status 400
    And the problem detail is "Wrong property names in node simpl:OfferingPrice. Allowed property names: op1, op2, value1, value2"

  @D9
  Scenario: A body that is not JSON is 415
    When "consumer-a" sends POST "/selfDescriptions/advancedSearch" to "qma-a" with the content type "text/plain"
      """
      name={token}
      """
    Then the response is a problem "unsupportedMediaType" with status 415

  @D9
  Scenario Outline: An Accept header the adapter cannot serve is 406 (<operation>)
    Given the next request has the header "Accept" set to "application/xml"
    When "consumer-a" sends GET "<path>" to "qma-a"
    Then the response is a problem "notAcceptable" with status 406

    Examples:
      | operation          | path                                    |
      | local quick search | /selfDescriptions?searchString={token}  |
      | participant list   | /participants/resourceDescriptions      |

  @D9
  Scenario: A wildcard Accept header is fine
    Given the next request has the header "Accept" set to "*/*"
    When "consumer-a" sends GET "/selfDescriptions?searchString={token}" to "qma-a"
    Then the status is 200

  @rbac @D66
  Scenario Outline: Local search needs a searcher role (<who>)
    When "<who>" sends GET "/selfDescriptions?searchString={token}" to "qma-a"
    Then the status is <status>

    Examples:
      | who             | status |
      | anonymous       | 401    |
      | an invalid token| 401    |
      | nobody-a        | 403    |
      | consumer-a      | 200    |
      | provider-a2     | 200    |

  @rbac @D68
  Scenario: A token of another realm is rejected
    When "consumer-b" sends GET "/selfDescriptions?searchString={token}" to "qma-a"
    Then the response is a problem with status 401

  # ------------------------------------------------------------------ participant operations (5B.1)

  @bp05b @D21
  Scenario: A Provider lists its own active and revoked resource descriptions
    Given "provider-a" has published a "DataOffering" resource description "mine"
    And "provider-a" has published a "DataOffering" resource description "revoked"
    And "provider-a" has revoked "revoked"
    And "provider-a2" has published a "DataOffering" resource description "theirs"
    When "provider-a" sends GET "/participants/resourceDescriptions?pageSize=100" to "qma-a"
    Then the status is 200
    And the result contains "mine"
    And the result contains "revoked"
    And the result does not contain "theirs"
    And the result lists "revoked, mine" in this order

  @bp05b @D21
  Scenario: A Provider reads one of its resource descriptions
    Given "provider-a" has published a "DataOffering" resource description "mine"
    When "provider-a" sends GET "/participants/resourceDescriptions/{mine.enc}" to "qma-a"
    Then the status is 200
    And the response is the stored resource description "mine"

  @rbac @bp05b @D21
  Scenario: A Provider cannot read the resource description of another participant
    Given "provider-a2" has published a "DataOffering" resource description "theirs"
    When "provider-a" sends GET "/participants/resourceDescriptions/{theirs.enc}" to "qma-a"
    Then the response is a problem with status 403

  @bp05b @D21
  Scenario: Reading an unknown participant resource description is 404
    When "provider-a" sends GET "/participants/resourceDescriptions/did%3Aweb%3Atest.simpl.local%3ADataOffering%3A{token}" to "qma-a"
    Then the response is a problem "notFound" with status 404

  @bp05b @D16
  Scenario: A Provider revokes through the adapter, idempotently
    Given "provider-a" has published a "DataOffering" resource description "mine"
    When "provider-a" sends PATCH "/participants/resourceDescriptions/{mine.enc}" to "qma-a"
    Then the status is 204
    And "mine" is stored with the status "revoked"
    When "provider-a" sends PATCH "/participants/resourceDescriptions/{mine.enc}" to "qma-a"
    Then the status is 204

  @rbac @bp05b @D16
  Scenario: A Provider cannot revoke the resource description of another participant
    Given "provider-a2" has published a "DataOffering" resource description "theirs"
    When "provider-a" sends PATCH "/participants/resourceDescriptions/{theirs.enc}" to "qma-a"
    Then the response is a problem with status 403
    And "theirs" is stored with the status "active"

  @bp05b @D16
  Scenario: Revoking an unknown resource description through the adapter is 404
    When "provider-a" sends PATCH "/participants/resourceDescriptions/did%3Aweb%3Atest.simpl.local%3ADataOffering%3A{token}" to "qma-a"
    Then the response is a problem with status 404

  @bp05b @D19
  Scenario: The adapter version history includes the requested version
    Given "provider-a" has published a "DataOffering" resource description "v1"
    And "provider-a" has published "v2" as version 2 of "v1"
    When "provider-a" sends GET "/participants/resourceDescriptions/{v1.enc}/version" to "qma-a"
    Then the status is 200
    And the field "totalCount" is "2"
    And the result contains exactly "v1, v2" of this scenario
    And the result lists "v2, v1" in this order
    And the result "v2" has the field "version" set to "2"
    And the result "v1" has the field "version" set to "1"

  @rbac @bp05b @D19
  Scenario: Only the owner reads the adapter version history
    Given "provider-a" has published a "DataOffering" resource description "v1"
    When "provider-a2" sends GET "/participants/resourceDescriptions/{v1.enc}/version" to "qma-a"
    Then the response is a problem with status 403

  @bp05b @D19
  Scenario: The adapter version history of an unknown id is 404
    When "provider-a" sends GET "/participants/resourceDescriptions/did%3Aweb%3Atest.simpl.local%3ADataOffering%3A{token}/version" to "qma-a"
    Then the response is a problem with status 404

  @rbac @bp05b @D66
  Scenario Outline: Participant operations need a publisher role (<who>)
    When "<who>" sends GET "/participants/resourceDescriptions" to "qma-a"
    Then the status is <status>

    Examples:
      | who         | status |
      | anonymous   | 401    |
      | consumer-a  | 403    |
      | nobody-a    | 403    |
      | provider-a2 | 200    |
