@api @qma @detail @bp06
Feature: Resource description details through the Consumer's GA, own and federated
  qma-a GET /governanceAuthority/resourceDescriptions/{id} (6.1) and the Trustor operation
  GET /federated/resourceDescriptions/{id} on qma-b. Errors per D43. No schema in any detail
  response (D42). Quality only for the own catalogue (D57).

  # ------------------------------------------------------------------ Trustor side

  @SIMPL-31150 @SIMPL-31151 @SIMPL-31152 @D42 @D48 @federation
  Scenario: The Trustor returns an authorised resource description without a schema
    Given "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    And the federated request headers for "CONSUMER"
    When "client qma-a-federation" sends GET "/federated/resourceDescriptions/{partner.enc}" to "qma-b"
    Then the status is 200
    And the response members are exactly "resourceDescription"
    And the field "resourceDescription" is the resource description "partner"
    And the field "resourceDescription/credentialSubject/dct:conformsTo/dct:schemaName" is "DataOfferingShape"
    And the response header "Correlation-Id" is "{correlation}"

  @SIMPL-31150 @D30 @D37 @federation
  Scenario: The Trustor refuses when the mapped attributes fail the policy
    Given "provider-b" has published a "DataOffering" resource description "consumer" discoverable by "CONSUMER"
    And the federated request headers for "CONSUMER"
    When "client qma-a-federation" sends GET "/federated/resourceDescriptions/{consumer.enc}" to "qma-b"
    Then the response is a problem "catalogue:discoveryPolicyDenied" with status 403
    And the response body does not contain "{token}"
    And the response header "Correlation-Id" is "{correlation}"

  @SIMPL-31150 @D37 @federation
  Scenario: The additional attribute alone can satisfy the policy
    Given "provider-b" has published a "DataOffering" resource description "federated" discoverable by "FEDERATED"
    And the federated request headers for ""
    When "client qma-a-federation" sends GET "/federated/resourceDescriptions/{federated.enc}" to "qma-b"
    Then the status is 200
    And the field "resourceDescription" is the resource description "federated"

  @SIMPL-31150 @federation
  Scenario: The Trustor answers 404 for an unknown resource description
    Given the federated request headers for "CONSUMER"
    When "client qma-a-federation" sends GET "/federated/resourceDescriptions/did%3Aweb%3Atest.simpl.local%3ADataOffering%3A{token}" to "qma-b"
    Then the response is a problem "notFound" with status 404
    And the response header "Correlation-Id" is "{correlation}"

  @SIMPL-31150 @D37 @D48 @federation
  Scenario Outline: The Trustor refuses a request without a required header (<missing>)
    Given "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    And the federated request headers for "CONSUMER"
    And the next request has no header "<missing>"
    When "client qma-a-federation" sends GET "/federated/resourceDescriptions/{partner.enc}" to "qma-b"
    Then the response is a problem with status 400
    And the response body does not contain "{token}"

    Examples:
      | missing         |
      | User-Attributes |
      | Correlation-Id  |

  @rbac @SIMPL-31150 @D36 @federation
  Scenario Outline: The Trustor refuses an unauthenticated or untrusted caller (<who>)
    Given "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    And the federated request headers for "CONSUMER"
    When "<who>" sends GET "/federated/resourceDescriptions/{partner.enc}" to "qma-b"
    Then the response is a problem with status <status>
    And the response body does not contain "{token}"

    Examples:
      | who                     | status |
      | anonymous               | 401    |
      | an invalid token        | 401    |
      | client qma-c-federation | 403    |
      | consumer-a              | 403    |
      | provider-b              | 403    |

  @D9 @federation
  Scenario: The Trustor detail answers 406 for an Accept it cannot serve
    Given "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    And the federated request headers for "CONSUMER"
    And the next request has the header "Accept" set to "application/xml"
    When "client qma-a-federation" sends GET "/federated/resourceDescriptions/{partner.enc}" to "qma-b"
    Then the response is a problem "notAcceptable" with status 406

  # ------------------------------------------------------------------ Trusted side, own catalogue

  @SIMPL-31769 @SIMPL-31143 @D57 @D7
  Scenario Outline: The GA returns an own resource description with its quality (<catalogue>)
    Given "provider-a" has published a "DataOffering" resource description "own"
    When "consumer-a" sends GET "/governanceAuthority/resourceDescriptions/{own.enc}?catalogueId=<catalogue>" to "qma-a"
    Then the status is 200
    And the field "catalogueId" is "catalogue-a"
    And the field "resourceDescription" is the resource description "own"
    And the field "qualityAssessment/overall/score" is "100"
    And the field "qualityAssessment/overall/classification" is "Green"
    And the field "qualityAssessment/dimensions/[name=Completeness]/score" is "100"
    And the field "qualityAssessment/dimensions/[name=Completeness]/classification" is "Green"
    And the response is a JSON object without the member "schema"

    Examples:
      | catalogue   |
      | catalogue-a |
      | local       |

  @D43 @D30
  Scenario: The GA refuses an own resource description the Consumer may not discover
    Given "provider-a" has published a "DataOffering" resource description "other" discoverable by "OTHER"
    When "consumer-a" sends GET "/governanceAuthority/resourceDescriptions/{other.enc}?catalogueId=catalogue-a" to "qma-a"
    Then the response is a problem "catalogue:discoveryPolicyDenied" with status 403

  @D43 @D16
  Scenario: The GA does not show a revoked own resource description to Consumers
    Given "provider-a" has published a "DataOffering" resource description "own"
    And "provider-a" has revoked "own"
    When "consumer-a" sends GET "/governanceAuthority/resourceDescriptions/{own.enc}?catalogueId=catalogue-a" to "qma-a"
    Then the response is a problem "notFound" with status 404

  @D43
  Scenario: The GA answers 404 for an unknown own resource description
    When "consumer-a" sends GET "/governanceAuthority/resourceDescriptions/did%3Aweb%3Atest.simpl.local%3ADataOffering%3A{token}?catalogueId=catalogue-a" to "qma-a"
    Then the response is a problem "notFound" with status 404

  @SIMPL-33885 @D43
  Scenario Outline: The GA rejects a missing, malformed or unknown catalogue id (<case>)
    Given "provider-a" has published a "DataOffering" resource description "own"
    When "consumer-a" sends GET "/governanceAuthority/resourceDescriptions/{own.enc}<query>" to "qma-a"
    Then the response is a problem with status 400

    Examples:
      | case      | query                                |
      | missing   |                                      |
      | empty     | ?catalogueId=                        |
      | malformed | ?catalogueId=bad%20id!               |
      | too long  | ?catalogueId={a256}                  |
      | unknown   | ?catalogueId=catalogue-zz            |

  # ------------------------------------------------------------------ Trusted side, federated catalogue

  @SIMPL-31153 @SIMPL-31149 @SIMPL-31770 @D42 @D57 @federation
  Scenario: The GA forwards a detail request to the Trustor and returns the result
    Given "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    When "consumer-a" sends GET "/governanceAuthority/resourceDescriptions/{partner.enc}?catalogueId=catalogue-b" to "qma-a"
    Then the status is 200
    And the field "catalogueId" is "catalogue-b"
    And the field "resourceDescription" is the resource description "partner"
    And the field "qualityAssessment" does not exist
    And the response is a JSON object without the member "schema"

  @SIMPL-31153 @D43 @federation
  Scenario: The GA passes on the Trustor's policy refusal
    Given "provider-b" has published a "DataOffering" resource description "consumer" discoverable by "CONSUMER"
    When "consumer-a" sends GET "/governanceAuthority/resourceDescriptions/{consumer.enc}?catalogueId=catalogue-b" to "qma-a"
    Then the response is a problem "catalogue:discoveryPolicyDenied" with status 403

  @SIMPL-31153 @D43 @federation
  Scenario: The GA passes on the Trustor's not found
    When "consumer-a" sends GET "/governanceAuthority/resourceDescriptions/did%3Aweb%3Atest.simpl.local%3ADataOffering%3A{token}?catalogueId=catalogue-b" to "qma-a"
    Then the response is a problem "notFound" with status 404

  @SIMPL-31153 @D43 @federation
  Scenario: A revoked resource description at the Trustor is not found
    Given "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    And "provider-b" has revoked "partner"
    When "consumer-a" sends GET "/governanceAuthority/resourceDescriptions/{partner.enc}?catalogueId=catalogue-b" to "qma-a"
    Then the response is a problem "notFound" with status 404

  @SIMPL-31153 @SIMPL-31150 @D43 @D36 @federation
  Scenario: A Trustor that does not trust the GA gives 502
    When "consumer-a" sends GET "/governanceAuthority/resourceDescriptions/did%3Aweb%3Atest.simpl.local%3ADataOffering%3A{token}?catalogueId=catalogue-c" to "qma-a"
    Then the response is a problem "catalogue:federationUnauthenticated" with status 502

  @SIMPL-31153 @D43 @federation
  Scenario: An inactive catalogue gives 502
    When "consumer-a" sends GET "/governanceAuthority/resourceDescriptions/did%3Aweb%3Atest.simpl.local%3ADataOffering%3A{token}?catalogueId=catalogue-d" to "qma-a"
    Then the response is a problem "catalogue:federationUnavailable" with status 502

  @SIMPL-31153 @SIMPL-31770 @D43 @D47 @slow @federation
  Scenario: A slow Trustor gives 504
    When "consumer-a" sends GET "/governanceAuthority/resourceDescriptions/did%3Aweb%3Atest.simpl.local%3ADataOffering%3A{token}?catalogueId=catalogue-slow" to "qma-a"
    Then the response is a problem "catalogue:federationTimeout" with status 504
    And the request took less than 8 seconds

  @rbac @D66
  Scenario Outline: The GA detail needs a searcher role (<who>)
    Given "provider-a" has published a "DataOffering" resource description "own"
    When "<who>" sends GET "/governanceAuthority/resourceDescriptions/{own.enc}?catalogueId=catalogue-a" to "qma-a"
    Then the status is <status>

    Examples:
      | who        | status |
      | anonymous  | 401    |
      | nobody-a   | 403    |
      | consumer-a | 200    |
