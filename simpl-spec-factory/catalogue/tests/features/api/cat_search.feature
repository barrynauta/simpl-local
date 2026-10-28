@api @cat @search @bp06
Feature: catalogue-be executes quick and advanced search with policy and quality filters
  BP06 step 02 on the catalogue itself. Matching per D27 and D29, discovery policy per D30,
  paging and order per D31, quality filters per D54. The caller's attributes come from the
  User-Attributes header when present, else from the token (D69).

  # ------------------------------------------------------------------ quick search (D27)

  @SIMPL-31111 @D27 @D34
  Scenario: Quick search finds an active resource description with its main properties
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    When "consumer-a" sends GET "/selfDescriptions/quickSearch?searchTerm={token}" to "cat-a"
    Then the status is 200
    And the field "totalCount" is "1"
    And the result contains "rd1"
    And the result "rd1" has the field "claimsGraphUri" as the list "{rd1.id}"
    And the result "rd1" has the field "offeringType" set to "data"
    And the result "rd1" has the field "name" set to "{token} rd1 data"
    And the result "rd1" has the field "inLanguage" set to "en"
    And the result "rd1" has the field "description" set to "{token} description of rd1"
    And the result "rd1" has the field "serviceAccessPoint" set to "https://provider.example.org/data"
    And the result "rd1" has the field "overallScore" set to "100"
    And the result "rd1" has the field "overallClassification" set to "Green"

  @D27
  Scenario Outline: Quick search matches terms case-insensitively anywhere in the subject (<term>)
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    When "consumer-a" sends GET "/selfDescriptions/quickSearch?searchTerm=<term>" to "cat-a"
    Then the status is 200
    And the result contains "rd1"

    Examples:
      | term                         |
      | {token}                      |
      | {TOKEN}                      |
      | nomatchzz%20{TOKEN}          |

  @D27
  Scenario: Quick search matches a nested literal such as the contact
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" has the field "credentialSubject/simpl:providerInformation/simpl:contact" set to "contact-{token}@example.org"
    And "rd1" has the field "credentialSubject/simpl:generalServiceProperties/simpl:name" set to "plain name"
    And "rd1" has the field "credentialSubject/simpl:generalServiceProperties/simpl:description" set to "plain description"
    And "provider-a" has published "rd1"
    When "consumer-a" sends GET "/selfDescriptions/quickSearch?searchTerm=contact-{token}" to "cat-a"
    Then the status is 200
    And the result contains "rd1"

  @D27
  Scenario: Quick search does not match inside the service policy
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" has the field "credentialSubject/simpl:generalServiceProperties/simpl:name" set to "plain name"
    And "rd1" has the field "credentialSubject/simpl:generalServiceProperties/simpl:description" set to "plain description"
    And "rd1" has the field "credentialSubject/simpl:servicePolicy/simpl:usage-policy" set to "policy-{token}"
    And "provider-a" has published "rd1"
    When "consumer-a" sends GET "/selfDescriptions/quickSearch?searchTerm=policy-{token}" to "cat-a"
    Then the status is 200
    And the result does not contain "rd1"

  @D27
  Scenario: Quick search compares numbers as their decimal text
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" has the price 4242.4242
    And "provider-a" has published "rd1"
    When "consumer-a" sends GET "/selfDescriptions/quickSearch?searchTerm=4242.4242&pageSize=100" to "cat-a"
    Then the status is 200
    And the result contains "rd1"

  @D16 @D27
  Scenario: Quick search excludes revoked resource descriptions
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    And "provider-a" has published a "DataOffering" resource description "rd2"
    And "provider-a" has revoked "rd2"
    When "consumer-a" sends GET "/selfDescriptions/quickSearch?searchTerm={token}" to "cat-a"
    Then the status is 200
    And the result contains exactly "rd1" of this scenario

  # ------------------------------------------------------------------ discovery policy (D30)

  @SIMPL-31111 @D30 @D69
  Scenario: Quick search returns only what the caller's attributes may discover
    Given "provider-a" has published a "DataOffering" resource description "open" discoverable by "CONSUMER"
    And "provider-a" has published a "DataOffering" resource description "research" discoverable by "RESEARCHER"
    And "provider-a" has published a "DataOffering" resource description "other" discoverable by "OTHER"
    And "provider-a" has published a "DataOffering" resource description "both" discoverable by "OTHER, CONSUMER"
    When "consumer-a" sends GET "/selfDescriptions/quickSearch?searchTerm={token}" to "cat-a"
    Then the status is 200
    And the result contains exactly "open, research, both" of this scenario

  @D30 @D69
  Scenario: The User-Attributes header replaces the token attributes on catalogue-be
    Given "provider-a" has published a "DataOffering" resource description "open" discoverable by "CONSUMER"
    And "provider-a" has published a "DataOffering" resource description "other" discoverable by "OTHER"
    And the next request has the header "User-Attributes" set to "OTHER"
    When "consumer-a" sends GET "/selfDescriptions/quickSearch?searchTerm={token}" to "cat-a"
    Then the status is 200
    And the result contains exactly "other" of this scenario

  @D30
  Scenario: A resource description without a valid policy is not discoverable
    Given a "DataOffering" resource description "nopolicy" by "provider-a"
    And "nopolicy" has no access policy
    And "provider-a" has published "nopolicy"
    And a "DataOffering" resource description "badpolicy" by "provider-a"
    And "badpolicy" has an access policy that is not JSON
    And "provider-a" has published "badpolicy"
    And "provider-a" has published a "DataOffering" resource description "open"
    When "consumer-a" sends GET "/selfDescriptions/quickSearch?searchTerm={token}" to "cat-a"
    Then the status is 200
    And the result contains exactly "open" of this scenario

  @D30
  Scenario: A policy needs the search action
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" has an access policy with the action "http://simpl.eu/odrl/actions/use" for "CONSUMER"
    And "provider-a" has published "rd1"
    When "consumer-a" sends GET "/selfDescriptions/quickSearch?searchTerm={token}" to "cat-a"
    Then the status is 200
    And the result does not contain "rd1"

  @rbac @D30
  Scenario: The owner gets no exception in search
    Given "provider-a" has published a "DataOffering" resource description "rd1" discoverable by "CONSUMER"
    When "provider-a" sends GET "/selfDescriptions/quickSearch?searchTerm={token}" to "cat-a"
    Then the status is 200
    And the result does not contain "rd1"

  # ------------------------------------------------------------------ paging and order (D31)

  @D31
  Scenario: Search results are ordered by upload time, newest first, and paged
    Given "provider-a" has published a "DataOffering" resource description "first"
    And "provider-a" has published a "DataOffering" resource description "second"
    And "provider-a" has published a "DataOffering" resource description "third"
    When "consumer-a" sends GET "/selfDescriptions/quickSearch?searchTerm={token}&page=1&pageSize=2" to "cat-a"
    Then the status is 200
    And the field "totalCount" is "3"
    And the result has 2 items
    And the result lists "third, second" in this order
    When "consumer-a" sends GET "/selfDescriptions/quickSearch?searchTerm={token}&page=2&pageSize=2" to "cat-a"
    Then the status is 200
    And the field "totalCount" is "3"
    And the result contains exactly "first" of this scenario

  @D31
  Scenario: A page size above 100 is capped, not rejected
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    When "consumer-a" sends GET "/selfDescriptions/quickSearch?searchTerm={token}&pageSize=500" to "cat-a"
    Then the status is 200
    And the result contains "rd1"

  @D31
  Scenario Outline: Invalid paging is 400 (<query>)
    When "consumer-a" sends GET "/selfDescriptions/quickSearch?searchTerm={token}&<query>" to "cat-a"
    Then the response is a problem with status 400

    Examples:
      | query        |
      | page=0       |
      | pageSize=0   |
      | page=-1      |
      | pageSize=abc |
      | page=1.5     |

  # ------------------------------------------------------------------ quality filters (D54)

  @SIMPL-29292 @D54 @D53 @quality
  Scenario Outline: Quick search filters on quality (<filter>)
    Given "provider-a" has published a "DataOffering" resource description "green"
    And a "DataOffering" resource description "yellow" by "provider-a"
    And the quality stub scores "yellow" with "overall=75"
    And "provider-a" has published "yellow"
    And "provider-a" has published a "DataOffering" resource description "red"
    And "provider-a" has replaced the quality assessment record of "red" with the score 30 of 100
    When "consumer-a" sends GET "/selfDescriptions/quickSearch?searchTerm={token}&<filter>" to "cat-a"
    Then the status is 200
    And the result contains exactly "<expected>" of this scenario

    Examples:
      | filter                                           | expected           |
      | minimumScore=75                                  | green, yellow      |
      | minimumScore=75.5                                | green              |
      | minimumScore=0                                   | green, yellow, red |
      | minimumClassification=Yellow                     | green, yellow      |
      | minimumClassification=green                      | green              |
      | minimumClassification=Red                        | green, yellow, red |
      | minimumScore=90&minimumClassification=Yellow     | green              |
      | minimumScore=&minimumClassification=             | green, yellow, red |

  @SIMPL-29292 @D54
  Scenario Outline: Invalid quality filters are 400 (<filter>)
    When "consumer-a" sends GET "/selfDescriptions/quickSearch?searchTerm={token}&<filter>" to "cat-a"
    Then the response is a problem "catalogue:invalidInput" with status 400
    And the problem detail is "<detail>"

    Examples:
      | filter                         | detail                                                                |
      | minimumScore=-1                | minimumScore must be greater than or equal to 0.                     |
      | minimumClassification=Platinum | minimumClassification is not a supported quality classification value. |

  # ------------------------------------------------------------------ advanced search (D29)

  @SIMPL-31112 @D29
  Scenario Outline: Advanced search combines node criteria with AND (<fixture>)
    Given "provider-a" has published a "DataOffering" resource description "data1"
    And "provider-a" has published a "ApplicationOffering" resource description "app1"
    When "consumer-a" sends POST "/selfDescriptions/advancedSearch" to "cat-a" with the fixture "<fixture>"
    Then the status is 200
    And the result contains exactly "<expected>" of this scenario

    Examples:
      | fixture                        | expected    |
      | name-token                     | data1, app1 |
      | name-token-price-in-range      | data1, app1 |
      | name-token-price-out-of-range  |             |
      | name-token-price-equal         | data1, app1 |
      | name-token-and-currency        | data1, app1 |
      | name-token-and-wrong-currency  |             |
      | name-token-and-application-type| app1        |
      | name-token-missing-node        |             |

  @SIMPL-29293 @D54 @quality
  Scenario Outline: Advanced search filters on quality from the body (<fixture>)
    Given "provider-a" has published a "DataOffering" resource description "green"
    And a "DataOffering" resource description "yellow" by "provider-a"
    And the quality stub scores "yellow" with "overall=75"
    And "provider-a" has published "yellow"
    When "consumer-a" sends POST "/selfDescriptions/advancedSearch" to "cat-a" with the fixture "<fixture>"
    Then the status is 200
    And the result contains exactly "green" of this scenario

    Examples:
      | fixture                    |
      | name-token-min-score-80    |
      | name-token-min-class-green |

  @D29
  Scenario Outline: An invalid advanced search body is 400 (<fixture>)
    When "consumer-a" sends POST "/selfDescriptions/advancedSearch" to "cat-a" with the fixture "<fixture>"
    Then the response is a problem with status 400

    Examples:
      | fixture                |
      | empty-node             |
      | wrong-range-keys       |
      | unknown-operator       |
      | non-numeric-range      |
      | not-an-object          |
      | unknown-classification |
      | negative-score         |

  @D29
  Scenario: An empty node is rejected with the contract message
    When "consumer-a" sends POST "/selfDescriptions/advancedSearch" to "cat-a" with the fixture "empty-node"
    Then the response is a problem with status 400
    And the problem detail is "No Properties defined in simpl:ContractTemplate node. Please provide at least one property with searched value."

  @D29
  Scenario: Wrong range keys are rejected with the contract message
    When "consumer-a" sends POST "/selfDescriptions/advancedSearch" to "cat-a" with the fixture "wrong-range-keys"
    Then the response is a problem with status 400
    And the problem detail is "Wrong property names in node simpl:OfferingPrice. Allowed property names: op1, op2, value1, value2"

  @D30
  Scenario: Advanced search applies the discovery policy too
    Given "provider-a" has published a "DataOffering" resource description "open" discoverable by "CONSUMER"
    And "provider-a" has published a "DataOffering" resource description "other" discoverable by "OTHER"
    When "consumer-a" sends POST "/selfDescriptions/advancedSearch" to "cat-a" with the fixture "name-token"
    Then the status is 200
    And the result contains exactly "open" of this scenario

  @rbac @D66
  Scenario Outline: Search on catalogue-be needs a searcher role (<who>)
    When "<who>" sends GET "/selfDescriptions/quickSearch?searchTerm={token}" to "cat-a"
    Then the status is <status>

    Examples:
      | who            | status |
      | anonymous      | 401    |
      | nobody-a       | 403    |
      | schema-admin-a | 403    |
      | consumer-a     | 200    |
      | provider-a2    | 200    |
