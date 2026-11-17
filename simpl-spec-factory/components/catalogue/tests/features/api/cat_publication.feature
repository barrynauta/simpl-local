@api @cat @bp05b @publication
Feature: catalogue-be publishes signed resource descriptions
  BP05b steps 04 to 07 through the API. The pipeline of D14: token and role, syntax,
  signature (D22, D23), ownership, schema (D59), SHACL (D63), duplicates and versions,
  quality gate, store. The first failure wins.

  @D11 @D12 @D14 @D22
  Scenario: A Provider publishes a valid data offering
    Given a "DataOffering" resource description "rd1" by "provider-a"
    When "provider-a" publishes "rd1"
    Then the status is 201
    And the response header "Location" points to "rd1"
    And the field "id" is the id of "rd1"
    And the field "sdHash" is the sdHash of "rd1"
    And the field "status" is "active"
    And the field "issuer" is the issuer of "rd1"
    And the field "validatorDids" is an empty list
    And the field "uploadDatetime" is a timestamp
    And the field "statusDatetime" is a timestamp
    And the field "resourceKey" is the id of "rd1"
    And the field "currentVersion" is "1"
    And the field "qualityAssessment/rules" exists
    And the field "qualityAssessment/dimensions" exists
    And the field "qualityAssessment/overall" exists

  @D12 @D14
  Scenario Outline: A Provider publishes each resource type through both create operations
    Given a "<type>" resource description "rd1" by "provider-a"
    When "provider-a" publishes "rd1" through "<path>"
    Then the status is 201
    And the response header "Location" points to "rd1"
    And the field "id" is the id of "rd1"
    And the field "sdHash" is the sdHash of "rd1"
    And the field "resourceKey" is the id of "rd1"
    And the field "currentVersion" is "1"
    And the field "qualityAssessment/overall" exists
    And "rd1" is stored with the status "active"

    Examples:
      | type                   | path                         |
      | DataOffering           | /self-descriptions           |
      | ApplicationOffering    | /self-descriptions           |
      | InfrastructureOffering | /self-descriptions           |
      | DataOffering           | /self-descriptions/resource  |
      | ApplicationOffering    | /self-descriptions/resource  |
      | InfrastructureOffering | /self-descriptions/resource  |

  @D15
  Scenario: Publication is synchronous and never answers 202
    Given a "DataOffering" resource description "rd1" by "provider-a"
    When "provider-a" publishes "rd1"
    Then the status is 201
    And the response header "retry-after" is absent

  @D11
  Scenario: A published resource description can be read back by its id
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    When "consumer-a" sends GET "/self-descriptions/{rd1.enc}" to "cat-a"
    Then the status is 200
    And the response is the stored resource description "rd1"

  @D2
  Scenario: Reading an unknown resource description is a problem 404
    When "consumer-a" sends GET "/self-descriptions/did%3Aweb%3Atest.simpl.local%3ADataOffering%3A{token}" to "cat-a"
    Then the response is a problem "resourceNotFound" with status 404

  @D14 @D24
  Scenario Outline: A syntax error is rejected with 400 before anything else (<broken>)
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" is broken as "<broken>"
    When "provider-a" publishes "rd1"
    Then the response is a problem "badRequest" with status 400
    And "rd1" is not stored

    Examples:
      | broken                |
      | not-json              |
      | wrong-first-context   |
      | not-a-vc-type         |
      | missing-issuer        |
      | missing-issuance-date |
      | missing-proof         |
      | remote-context        |

  @D14
  Scenario: A credential subject without an id is rejected with 400
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" is broken as "missing-subject-id"
    When "provider-a" publishes "rd1"
    Then the response is a problem "badRequest" with status 400

  @D14 @D22
  Scenario Outline: A wrong signature is rejected with 422 (<broken>)
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" is broken as "<broken>"
    When "provider-a" publishes "rd1"
    Then the response is a problem "catalogue:invalidSignature" with status 422
    And "rd1" is not stored

    Examples:
      | broken                         |
      | altered-after-signing          |
      | wrong-signing-key              |
      | verification-method-not-issuer |
      | garbage-jws                    |
      | wrong-jws-header               |

  @D22
  Scenario Outline: A signature check covers every part of the credential (<type>)
    Given a "<type>" resource description "rd1" by "provider-a"
    And "rd1" is broken as "altered-after-signing"
    When "provider-a" publishes "rd1"
    Then the response is a problem "catalogue:invalidSignature" with status 422

    Examples:
      | type                   |
      | ApplicationOffering    |
      | InfrastructureOffering |

  @rbac @D14 @D23
  Scenario Outline: An ownership mismatch is rejected with 403 (<broken>)
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" is broken as "<broken>"
    When "provider-a" publishes "rd1"
    Then the response is a problem "catalogue:providerMismatch" with status 403
    And "rd1" is not stored

    Examples:
      | broken                   |
      | other-provider           |
      | unregistered-key         |
      | key-of-other-participant |

  @rbac @D23
  Scenario: A Provider of data space B cannot publish in catalogue A
    Given a "DataOffering" resource description "rd1" by "provider-b"
    When "provider-b" publishes "rd1" to "cat-a"
    Then the response is a problem with status 401

  @D14 @D59
  Scenario Outline: A resource description against an unsupported schema is rejected with 422 (<broken>)
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" is broken as "<broken>"
    When "provider-a" publishes "rd1"
    Then the response is a problem "catalogue:schemaNotSupported" with status 422
    And "rd1" is not stored

    Examples:
      | broken               |
      | unknown-schema       |
      | wrong-schema-version |
      | missing-conforms-to  |

  @D14 @D63
  Scenario Outline: A SHACL violation is rejected with 422 and names the path (<broken>)
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" is broken as "<broken>"
    When "provider-a" publishes "rd1"
    Then the response is a problem "catalogue:shaclViolation" with status 422
    And the problem detail contains "<path>"
    And "rd1" is not stored

    Examples:
      | broken               | path               |
      | missing-name         | name               |
      | two-names            | name               |
      | bad-offering-type    | offeringType       |
      | untyped-access-point | serviceAccessPoint |
      | string-price         | price              |

  @D14 @D63
  Scenario Outline: The SHACL check applies to every resource type (<type>)
    Given a "<type>" resource description "rd1" by "provider-a"
    And "rd1" is broken as "missing-name"
    When "provider-a" publishes "rd1"
    Then the response is a problem "catalogue:shaclViolation" with status 422

    Examples:
      | type                   |
      | ApplicationOffering    |
      | InfrastructureOffering |

  @D59
  Scenario: A resource description is validated only against the shape it names
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" conforms to the shape "ApplicationOfferingShape"
    When "provider-a" publishes "rd1"
    Then the status is 201

  @D14
  Scenario: Publishing the same credential subject id twice is a conflict
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    And a "DataOffering" resource description "rd2" by "provider-a"
    And "rd2" has the field "credentialSubject/@id" set to "{rd1.id}"
    When "provider-a" publishes "rd2"
    Then the response is a problem "catalogue:alreadyExists" with status 409

  @D14
  Scenario: The pipeline checks the syntax before the signature
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" is broken as "missing-issuance-date"
    And "rd1" is broken as "altered-after-signing"
    When "provider-a" publishes "rd1"
    Then the response is a problem "badRequest" with status 400

  @D14
  Scenario: The pipeline checks the signature before the schema
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" is broken as "unknown-schema"
    And "rd1" is broken as "altered-after-signing"
    When "provider-a" publishes "rd1"
    Then the response is a problem "catalogue:invalidSignature" with status 422

  @rbac @D14
  Scenario: The pipeline checks ownership before the schema and SHACL
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" is broken as "other-provider"
    And "rd1" is broken as "missing-name"
    When "provider-a" publishes "rd1"
    Then the response is a problem "catalogue:providerMismatch" with status 403

  @D14
  Scenario: The pipeline checks the schema before SHACL
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" is broken as "missing-name"
    And "rd1" is broken as "wrong-schema-version"
    When "provider-a" publishes "rd1"
    Then the response is a problem "catalogue:schemaNotSupported" with status 422

  @D14
  Scenario: The pipeline checks SHACL before duplicates
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    And a "DataOffering" resource description "rd2" by "provider-a"
    And "rd2" has the field "credentialSubject/@id" set to "{rd1.id}"
    And "rd2" is broken as "missing-name"
    When "provider-a" publishes "rd2"
    Then the response is a problem "catalogue:shaclViolation" with status 422

  @rbac @D14 @D66
  Scenario Outline: Publication needs a token and a publisher role (<who>)
    Given a "DataOffering" resource description "rd1" by "provider-a"
    When "<who>" publishes "rd1" to "cat-a"
    Then the response is a problem with status <status>
    And "rd1" is not stored

    Examples:
      | who             | status |
      | anonymous       | 401    |
      | an invalid token| 401    |
      | consumer-a      | 403    |
      | nobody-a        | 403    |
      | schema-admin-a  | 403    |

  @rbac @D66
  Scenario: A Provider with only the data publisher role can publish
    Given "provider-a2" has published a "DataOffering" resource description "rd1"
    Then "rd1" is stored with the status "active"
