Feature: Access control (Tier 1) on schema operations
  SIMPL-17066 with the access matrix of D5, the 401 rule of D3 and the role
  names of D4. Webhook operations need only an authenticated caller (D7,
  SIMPL-24175 in part).

  Background:
    Given the RESOURCE schema "$A" exists for resource type "$RT"

  @SIMPL-17066 @SIMPL-13343 @SIMPL-13345 @D4 @D5 @rbac
  Scenario Outline: Viewers and admins may read
    Given I am "<user>"
    When I call the operation "<operation>" on schema "$A"
    Then the response status is 200

    Examples:
      | user      | operation          |
      | ga-viewer | list schemas       |
      | ga-viewer | get latest content |
      | ga-viewer | list versions      |
      | ga-viewer | get version        |
      | ga-admin  | list schemas       |
      | ga-admin  | get latest content |
      | ga-admin  | list versions      |
      | ga-admin  | get version        |

  @SIMPL-17066 @SIMPL-13343 @SIMPL-32258 @SIMPL-13345 @D5 @rbac
  Scenario Outline: A user without a schema role may not read
    Given I am "ga-norole"
    When I call the operation "<operation>" on schema "$A"
    Then the response is a "forbidden" problem with status 403

    Examples:
      | operation          |
      | list schemas       |
      | get latest content |
      | list versions      |
      | get version        |

  @SIMPL-17066 @SIMPL-13360 @SIMPL-20529 @SIMPL-14712 @D5 @rbac
  Scenario Outline: Only admins may change schemas
    Given I am "<user>"
    When I call the operation "<operation>" on schema "$A"
    Then the response is a "forbidden" problem with status 403
    And schema "$A" has status "PUBLISHED"
    And schema "$A" has the versions "1"

    Examples:
      | user      | operation      |
      | ga-viewer | create schema  |
      | ga-viewer | create version |
      | ga-viewer | revoke schema  |
      | ga-viewer | publish schema |
      | ga-norole | create schema  |
      | ga-norole | create version |
      | ga-norole | revoke schema  |
      | ga-norole | publish schema |

  @SIMPL-17066 @D5 @rbac
  Scenario: A schema refused with 403 is not created
    Given I am "ga-viewer"
    When I create the RESOURCE schema "$B" for resource type "$RT2"
    Then the response is a "forbidden" problem with status 403
    And listing "resourceType=$RT2" is empty

  @SIMPL-17066 @D5 @D23 @rbac
  Scenario: Authorisation is checked before validation
    Given I am "ga-viewer"
    When I create the RESOURCE schema "$B" for resource type "data" from "syntax-error.ttl"
    Then the response is a "forbidden" problem with status 403

  @SIMPL-17066 @D5 @D23 @rbac
  Scenario: Authorisation is checked before the existence check
    Given I am "ga-viewer"
    When I add a new version to the unknown schema "NonExistent$A" with schema type "RESOURCE"
    Then the response is a "forbidden" problem with status 403

  @SIMPL-17066 @SIMPL-20529 @D4 @D5 @rbac
  Scenario: An admin may revoke and publish
    Given I am "ga-admin"
    When I call the operation "revoke schema" on schema "$A"
    Then the response status is 200
    When I call the operation "publish schema" on schema "$A"
    Then the response status is 200

  @SIMPL-24175 @SIMPL-19992 @D7 @rbac
  Scenario Outline: Any authenticated caller may manage webhook subscriptions
    Given I am "<user>"
    When I subscribe the webhook "hook" to "SchemaPublished"
    Then the response status is 201
    When I list the webhooks
    Then the response status is 200
    And the webhook list contains "hook" with the events "SchemaPublished"
    When I delete the webhook "hook"
    Then the response status is 204

    Examples:
      | user      |
      | ga-norole |
      | ga-viewer |
      | ga-admin  |

  @SIMPL-13343 @SIMPL-32258 @SIMPL-17066 @D3
  Scenario Outline: Without a token every operation answers 401
    Given I am not authenticated
    When I call the operation "<operation>" on schema "$A"
    Then the response is a "unauthorized" problem with status 401

    Examples:
      | operation          |
      | list schemas       |
      | get latest content |
      | list versions      |
      | get version        |
      | create schema      |
      | create version     |
      | revoke schema      |
      | publish schema     |

  @SIMPL-24175 @D3 @D7
  Scenario Outline: Without a token the webhook operations answer 401
    Given I am not authenticated
    When I call the operation "<operation>" on schema "$A"
    Then the response is a "unauthorized" problem with status 401

    Examples:
      | operation      |
      | list webhooks  |
      | create webhook |
      | delete webhook |

  @SIMPL-13343 @D3
  Scenario: A malformed token answers 401
    Given I send the malformed bearer token "not-a-jwt"
    When I call the operation "list schemas" on schema "$A"
    Then the response is a "unauthorized" problem with status 401

  @SIMPL-13343 @SIMPL-17066 @D3 @D59 @needs-idp
  Scenario Outline: A token that the realm did not sign answers 401, whatever roles it claims
    Given <token>
    When I call the operation "<operation>" on schema "$A"
    Then the response is a "unauthorized" problem with status 401

    Examples:
      | token                                                        | operation     |
      | I am "ga-admin" with a token whose signature was altered     | list schemas  |
      | I am "ga-admin" with an unsigned token                       | list schemas  |
      | I am "ga-admin" with a token signed by another issuer        | list schemas  |
      | I am "ga-admin" with an expired token                        | list schemas  |
      | I am "ga-admin" with a token whose signature was altered     | create schema |
      | I am "ga-admin" with an unsigned token                       | revoke schema |
      | I am "ga-admin" with a token signed by another issuer        | list webhooks |
