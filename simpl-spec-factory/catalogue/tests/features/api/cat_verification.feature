@api @cat @bp05b @verification @D25
Feature: catalogue-be verifies a credential without storing it
  POST /verification runs steps 2, 3, 5 and 6 of D14 as enabled by its flags. No ownership
  and no quality check, nothing stored. Authenticated, no role (D67).

  @D22
  Scenario: A valid credential verifies
    Given a "DataOffering" resource description "rd1" by "provider-a"
    When "provider-a" sends "rd1" to "/verification" on "cat-a"
    Then the status is 200
    And the field "verificationTimestamp" is a timestamp
    And the field "lifecycleStatus" is "active"
    And the field "issuer" is the issuer of "rd1"
    And the field "issuedDateTime" is a timestamp
    And the field "issuedDateTime" matches "2026-10-28T10:00:00(\.0+)?(Z|\+00:00)"
    And the field "validatorDids" is an empty list
    And "rd1" is not stored

  @quality
  Scenario: Verification does not check ownership or quality
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" is broken as "other-provider"
    And the quality stub scores "rd1" with "overall=10"
    When "consumer-a" sends "rd1" to "/verification" on "cat-a"
    Then the status is 200
    And "rd1" is not stored

  Scenario: A changed credential fails the signature check
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" is broken as "altered-after-signing"
    When "provider-a" sends "rd1" to "/verification" on "cat-a"
    Then the response is a problem "catalogue:invalidSignature" with status 422

  Scenario: The signature check can be switched off
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" is broken as "altered-after-signing"
    When "provider-a" sends "rd1" to "/verification?verifyVCSignature=false" on "cat-a"
    Then the status is 200

  Scenario: The VP signature flag is ignored
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" is broken as "altered-after-signing"
    When "provider-a" sends "rd1" to "/verification?verifyVPSignature=false" on "cat-a"
    Then the response is a problem "catalogue:invalidSignature" with status 422

  Scenario Outline: A schema or SHACL failure is 422 (<broken>)
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" is broken as "<broken>"
    When "provider-a" sends "rd1" to "/verification" on "cat-a"
    Then the response is a problem "<type>" with status 422

    Examples:
      | broken         | type                         |
      | unknown-schema | catalogue:schemaNotSupported |
      | missing-name   | catalogue:shaclViolation     |

  Scenario Outline: The schema check can be switched off (<broken>)
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" is broken as "<broken>"
    When "provider-a" sends "rd1" to "/verification?verifySchema=false" on "cat-a"
    Then the status is 200

    Examples:
      | broken         |
      | unknown-schema |
      | missing-name   |

  Scenario Outline: A syntax error is 400 (<broken>)
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" is broken as "<broken>"
    When "provider-a" sends "rd1" to "/verification" on "cat-a"
    Then the response is a problem "badRequest" with status 400

    Examples:
      | broken              |
      | not-json            |
      | not-a-vc-type       |
      | missing-issuer      |

  Scenario: A presentation is not accepted
    Given a "DataOffering" resource description "rd1" by "provider-a"
    When "provider-a" sends "rd1" wrapped in a presentation to "/verification" on "cat-a"
    Then the response is a problem "badRequest" with status 400

  @D25
  Scenario: A credential stored as revoked verifies as revoked
    Given "provider-a" has published a "DataOffering" resource description "rd1"
    And "provider-a" has revoked "rd1"
    When "provider-a" sends "rd1" to "/verification" on "cat-a"
    Then the status is 200
    And the field "lifecycleStatus" is "revoked"

  @rbac @D67
  Scenario Outline: Verification needs a valid token but no role (<who>)
    Given a "DataOffering" resource description "rd1" by "provider-a"
    When "<who>" sends "rd1" to "/verification" on "cat-a"
    Then the status is <status>

    Examples:
      | who        | status |
      | anonymous  | 401    |
      | nobody-a   | 200    |
      | consumer-a | 200    |
