@api @cbe @bp06
Feature: The client backend lists catalogues, shows details and serves schemas
  Catalogue list from the federation configuration (D46). v1 detail is the own catalogue,
  v2 detail routes by catalogueId (D8, D43, D57). Schemas and the quality profile come from
  the stubs (D61). Out-of-scope contract documents answer non-2xx (D10).

  # ------------------------------------------------------------------ catalogues (D46)

  @SIMPL-31102 @SIMPL-31103 @SIMPL-30848 @D46
  Scenario: The catalogue list shows the federated catalogues in file order
    When "consumer-a" sends GET "/catalogues" to "cbe-a-v1"
    Then the status is 200
    And the field "items" has 4 items
    And the field "items/0/id" is "catalogue-b"
    And the field "items/1/id" is "catalogue-c"
    And the field "items/2/id" is "catalogue-slow"
    And the field "items/3/id" is "catalogue-d"
    And the field "items/[id=catalogue-b]/name" is "Data space B catalogue"
    And the field "items/[id=catalogue-b]/authorityURL" is "http://qma-b:8000/v1"
    And the field "items/[id=catalogue-b]/active" is "true"
    And the field "items/[id=catalogue-d]/active" is "false"
    And the field "page" is "1"
    And the field "pageSize" is "20"
    And the field "total" is "4"
    And the response body does not contain "catalogue-a"

  @D46 @D31
  Scenario: The catalogue list is paged
    When "consumer-a" sends GET "/catalogues?page=2&pageSize=3" to "cbe-a-v1"
    Then the status is 200
    And the field "items" has 1 items
    And the field "items/0/id" is "catalogue-d"
    And the field "total" is "4"

  @D31
  Scenario Outline: Invalid paging of the catalogue list is 400 (<query>)
    When "consumer-a" sends GET "/catalogues?<query>" to "cbe-a-v1"
    Then the response is a problem with status 400

    Examples:
      | query      |
      | page=0     |
      | pageSize=0 |

  # ------------------------------------------------------------------ QAR (v1)

  @D55
  Scenario: The client backend returns the stored quality assessment record
    Given "provider-a" has published a "DataOffering" resource description "own"
    When "consumer-a" sends GET "/selfDescriptions/{own.enc}/qualityAssessmentRecord" to "cbe-a-v1"
    Then the status is 200
    And the field "@graph/[@type=QualityAssessmentRecord]/computedOn" is "{own.id}"

  @D55
  Scenario: The record of an unknown resource description is 404
    When "consumer-a" sends GET "/selfDescriptions/did%3Aweb%3Atest.simpl.local%3ADataOffering%3A{token}/qualityAssessmentRecord" to "cbe-a-v1"
    Then the response is a problem "notFound" with status 404

  # ------------------------------------------------------------------ detail v1 and v2

  @SIMPL-31769 @D8 @D57
  Scenario: The v1 detail returns an own resource description with its quality
    Given "provider-a" has published a "DataOffering" resource description "own"
    When "consumer-a" sends GET "/selfDescriptions/{own.enc}" to "cbe-a-v1"
    Then the status is 200
    And the response is the stored resource description "own"
    And the field "qualityAssessment/overall/score" is "100"
    And the field "qualityAssessment/overall/classification" is "Green"
    And the field "qualityAssessment/dimensions" has 3 items

  @D8
  Scenario: The v1 detail of an unknown resource description is 404
    When "consumer-a" sends GET "/selfDescriptions/did%3Aweb%3Atest.simpl.local%3ADataOffering%3A{token}" to "cbe-a-v1"
    Then the response is a problem "notFound" with status 404

  @SIMPL-31769 @SIMPL-33885 @D8 @D57
  Scenario Outline: The v2 detail of an own resource description has its quality (<catalogue>)
    Given "provider-a" has published a "DataOffering" resource description "own"
    When "consumer-a" sends GET "/selfDescriptions/{own.enc}?catalogueId=<catalogue>" to "cbe-a-v2"
    Then the status is 200
    And the response is the stored resource description "own"
    And the field "qualityAssessment/overall/score" is "100"
    And the field "qualityAssessment/overall/classification" is "Green"
    And the field "qualityAssessment/dimensions/[name=Completeness]/classification" is "Green"
    And the response is a JSON object without the member "schema"

    Examples:
      | catalogue   |
      | catalogue-a |
      | local       |

  @quality @SIMPL-31769 @D57
  Scenario: The v2 detail shows per-dimension scores
    Given a "DataOffering" resource description "own" by "provider-a"
    And the quality stub scores "own" with "overall=85, Completeness=95, Accuracy=40, Validity=70"
    And "provider-a" has published "own"
    When "consumer-a" sends GET "/selfDescriptions/{own.enc}?catalogueId=catalogue-a" to "cbe-a-v2"
    Then the status is 200
    And the field "qualityAssessment/overall/score" is "85"
    And the field "qualityAssessment/dimensions/[name=Accuracy]/score" is "40"
    And the field "qualityAssessment/dimensions/[name=Accuracy]/classification" is "Red"
    And the field "qualityAssessment/dimensions/[name=Validity]/classification" is "Yellow"

  @federation @SIMPL-31770 @SIMPL-33885 @D42 @D57
  Scenario: The v2 detail of a federated resource description routes to the Trustor
    Given "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    When "consumer-a" sends GET "/selfDescriptions/{partner.enc}?catalogueId=catalogue-b" to "cbe-a-v2"
    Then the status is 200
    And the response is the stored resource description "partner"
    And the response is a JSON object without the member "qualityAssessment"
    And the response is a JSON object without the member "schema"

  @SIMPL-33885 @SIMPL-31770 @D43
  Scenario Outline: The v2 detail rejects a missing or invalid catalogue id (<case>)
    Given "provider-a" has published a "DataOffering" resource description "own"
    When "consumer-a" sends GET "/selfDescriptions/{own.enc}<query>" to "cbe-a-v2"
    Then the response is a problem with status 400

    Examples:
      | case      | query                     |
      | missing   |                           |
      | empty     | ?catalogueId=             |
      | malformed | ?catalogueId=bad%20id!    |
      | unknown   | ?catalogueId=catalogue-zz |

  @SIMPL-33885 @D43
  Scenario: The v2 detail of an unknown own resource description is 404
    When "consumer-a" sends GET "/selfDescriptions/did%3Aweb%3Atest.simpl.local%3ADataOffering%3A{token}?catalogueId=catalogue-a" to "cbe-a-v2"
    Then the response is a problem "notFound" with status 404

  @D43 @D16
  Scenario: The v2 detail hides a revoked own resource description
    Given "provider-a" has published a "DataOffering" resource description "own"
    And "provider-a" has revoked "own"
    When "consumer-a" sends GET "/selfDescriptions/{own.enc}?catalogueId=catalogue-a" to "cbe-a-v2"
    Then the response is a problem "notFound" with status 404

  @D43 @D30
  Scenario: The v2 detail refuses an own resource description the Consumer may not discover
    Given "provider-a" has published a "DataOffering" resource description "other" discoverable by "OTHER"
    When "consumer-a" sends GET "/selfDescriptions/{other.enc}?catalogueId=catalogue-a" to "cbe-a-v2"
    Then the response is a problem with status 403

  @federation @SIMPL-31770 @SIMPL-33885 @D43
  Scenario: The v2 detail passes on a federated not found
    When "consumer-a" sends GET "/selfDescriptions/did%3Aweb%3Atest.simpl.local%3ADataOffering%3A{token}?catalogueId=catalogue-b" to "cbe-a-v2"
    Then the response is a problem "notFound" with status 404

  @federation @SIMPL-31770 @D43
  Scenario: The v2 detail passes on a federated policy refusal
    Given "provider-b" has published a "DataOffering" resource description "consumer" discoverable by "CONSUMER"
    When "consumer-a" sends GET "/selfDescriptions/{consumer.enc}?catalogueId=catalogue-b" to "cbe-a-v2"
    Then the response is a problem with status 403

  @federation @SIMPL-31770 @D43
  Scenario Outline: The v2 detail reports federation failures (<catalogue>)
    When "consumer-a" sends GET "/selfDescriptions/did%3Aweb%3Atest.simpl.local%3ADataOffering%3A{token}?catalogueId=<catalogue>" to "cbe-a-v2"
    Then the response is a problem with status 502

    Examples:
      | catalogue   |
      | catalogue-c |
      | catalogue-d |

  @federation @SIMPL-31770 @D43 @slow
  Scenario: The v2 detail reports a federation timeout with 504
    When "consumer-a" sends GET "/selfDescriptions/did%3Aweb%3Atest.simpl.local%3ADataOffering%3A{token}?catalogueId=catalogue-slow" to "cbe-a-v2"
    Then the response is a problem with status 504

  # ------------------------------------------------------------------ schemas and profile (D61)

  @SIMPL-35874 @D61
  Scenario: Without a schema type the list has the RESOURCE schemas only
    When "consumer-a" sends GET "/schemas" to "cbe-a-v2"
    Then the status is 200
    And the field "schemas" has 3 items
    And the field "schemas/[schemaId=DataOfferingShape]/resourceType" is "DATA"
    And the field "schemas/[schemaId=DataOfferingShape]/schemaType" is "RESOURCE"
    And the field "schemas/[schemaId=DataOfferingShape]/name" is "DataOfferingShape"
    And the field "schemas/[schemaId=DataOfferingShape]/title" is "Data Offering Shape"
    And the field "schemas/[schemaId=DataOfferingShape]/version" is "1"
    And the field "schemas/[schemaId=ApplicationOfferingShape]/resourceType" is "APPLICATION"
    And the field "schemas/[schemaId=InfrastructureOfferingShape]/resourceType" is "INFRASTRUCTURE"
    And the response body does not contain "FederatedSearchShape"

  @SIMPL-35874 @SIMPL-31815 @D61 @D62
  Scenario: The SEARCH schema type returns the one search schema
    When "consumer-a" sends GET "/schemas?schemaType=SEARCH" to "cbe-a-v2"
    Then the status is 200
    And the field "schemas" has 1 items
    And the field "schemas/0/schemaId" is "FederatedSearchShape"
    And the field "schemas/0/schemaType" is "SEARCH"
    And the field "schemas/0/resourceType" is null

  @D61
  Scenario: The RESOURCE schema type can be filtered by resource type
    When "consumer-a" sends GET "/schemas?schemaType=RESOURCE&resourceType=DATA" to "cbe-a-v2"
    Then the status is 200
    And the field "schemas" has 1 items
    And the field "schemas/0/schemaId" is "DataOfferingShape"

  @D61
  Scenario: An invalid schema type is 400
    When "consumer-a" sends GET "/schemas?schemaType=OTHER" to "cbe-a-v2"
    Then the response is a problem with status 400

  @SIMPL-35874 @D61
  Scenario Outline: The schema content is Turtle (<path>)
    When "consumer-a" sends GET "<path>" to "cbe-a-v2"
    Then the status is 200
    And the response content type is "text/turtle"
    And the response body contains "<shape>"

    Examples:
      | path                                 | shape                |
      | /schemas/DataOfferingShape/content   | DataOfferingShape    |
      | /schemas/DataOfferingShape/1         | DataOfferingShape    |
      | /schemas/FederatedSearchShape/content| FederatedSearchShape |

  @D61
  Scenario Outline: An unknown schema or version is 404 (<path>)
    When "consumer-a" sends GET "<path>" to "cbe-a-v2"
    Then the response is a problem with status 404

    Examples:
      | path                              |
      | /schemas/NoSuch{token}Shape/content |
      | /schemas/DataOfferingShape/99     |

  @D9 @D61
  Scenario: Schema content refuses an Accept it cannot serve
    Given the next request has the header "Accept" set to "application/json"
    When "consumer-a" sends GET "/schemas/DataOfferingShape/content" to "cbe-a-v2"
    Then the response is a problem with status 406

  @SIMPL-29292 @D61 @D53
  Scenario: The active quality profile of a schema comes from the stub
    When "consumer-a" sends GET "/schemas/DataOfferingShape/qualityProfiles/active" to "cbe-a-v2"
    Then the status is 200
    And the response body contains "urn:simpl:quality:classification:Green"
    And the response body contains "urn:simpl:quality:threshold:overall"

  @D61
  Scenario Outline: A schema name that is not PascalCase is 400 (<name>)
    When "consumer-a" sends GET "/schemas/<name>/qualityProfiles/active" to "cbe-a-v2"
    Then the response is a problem with status 400

    Examples:
      | name              |
      | dataOfferingShape |
      | Da                |
      | Data_Offering     |

  @D61
  Scenario: A schema without an active profile is 404
    When "consumer-a" sends GET "/schemas/NoProfileShape/qualityProfiles/active" to "cbe-a-v2"
    Then the response is a problem with status 404

  # ------------------------------------------------------------------ out of scope and access

  @D10 @openapi-only
  Scenario: Contract documents are out of scope
    When "consumer-a" sends GET "/contractClauses/documents/{token}" to "cbe-a-v1"
    Then the status is not a success

  @rbac @D66
  Scenario Outline: Every cbe operation needs a searcher role (<who> <path>)
    When "<who>" sends GET "<path>" to "<service>"
    Then the response is a problem with status <status>

    Examples:
      | who       | service  | path                                         | status |
      | anonymous | cbe-a-v1 | /catalogues                                  | 401    |
      | nobody-a  | cbe-a-v1 | /catalogues                                  | 403    |
      | anonymous | cbe-a-v2 | /schemas                                     | 401    |
      | nobody-a  | cbe-a-v2 | /schemas                                     | 403    |
      | nobody-a  | cbe-a-v2 | /schemas/DataOfferingShape/qualityProfiles/active | 403 |
      | anonymous | cbe-a-v2 | /selfDescriptions/x?catalogueId=catalogue-a  | 401    |
