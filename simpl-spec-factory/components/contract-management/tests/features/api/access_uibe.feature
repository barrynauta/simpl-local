@uibe @rbac @oidc
Feature: User authentication and roles at the contract UI backend
  User operations need a bearer token of the party's own realm with the role
  contract_manager. Machine operations need the UIBE API key (D66 to D69).

  @openapi-only @D66
  Scenario Outline: <method> <path> without credentials is refused with 401
    Given fresh negotiation ids
    When I send "<method>" to the provider UIBE at "<path>" without credentials
    Then the response is a problem with status 401 and type "unauthorized"

    Examples:
      | method | path                                  |
      | GET    | /contractOffers                       |
      | GET    | /contractOffers/${nid}                |
      | PATCH  | /contractOffers/${nid}/accept         |
      | GET    | /contracts                            |
      | GET    | /contracts/${nid}                     |
      | POST   | /contracts/${nid}/lifecycleActions    |
      | POST   | /contractOffers/${nid}                |
      | POST   | /admin/offerExpiry/run                |

  @openapi-only @D66
  Scenario Outline: <method> <path> with only the API key is refused with 401
    Given fresh negotiation ids
    When I send "<method>" to the provider UIBE at "<path>" with the UIBE API key
    Then the response is a problem with status 401 and type "unauthorized"

    Examples:
      | method | path                                  |
      | GET    | /contractOffers                       |
      | GET    | /contracts                            |
      | GET    | /contracts/${nid}                     |
      | POST   | /contracts/${nid}/lifecycleActions    |
      | PATCH  | /contractOffers/${nid}/reject         |

  @openapi-only @D66
  Scenario: A malformed bearer token is refused with 401
    When I send "GET" to the provider UIBE at "/contracts" with a malformed token
    Then the response is a problem with status 401 and type "unauthorized"

  @SIMPL-30959 @D68 @D74
  Scenario Outline: SIMPL-30959 AC3 - a <party> user without contract_manager is refused with 403
    When I send "GET" to the <party> UIBE at "/contracts" as "<user>"
    Then the response is a problem with status 403 and type "forbidden"

    Examples: provider
      | party    | user            |
      | provider | provider-norole |

    @consumer
    Examples: consumer
      | party    | user            |
      | consumer | consumer-norole |

  @SIMPL-32505 @SIMPL-21928 @D68 @consumer
  Scenario: SIMPL-21928 AC3 - a Consumer user without contract_manager cannot accept and nothing is signed
    Given fresh negotiation ids
    When the consumer user "consumer-norole" sends the offer decision "accept"
    Then the response is a problem with status 403 and type "forbidden"

  @SIMPL-31115 @D68
  Scenario: A Provider user without contract_manager cannot perform lifecycle actions
    Given a contract requested at the provider
    When the provider user "provider-norole" performs "SUSPEND" with reason "x"
    Then the response is a problem with status 403 and type "forbidden"
    And the provider contract stays in status "REQUESTED"

  @openapi-only @D69 @consumer
  Scenario: A token of the other party's realm is refused with 401
    When I send "GET" to the provider UIBE at "/contracts" as "consumer-admin"
    Then the response is a problem with status 401 and type "unauthorized"

  @openapi-only @D69 @consumer
  Scenario: A Provider token at the Consumer UIBE is refused with 401
    When I send "GET" to the consumer UIBE at "/contractOffers" as "provider-admin"
    Then the response is a problem with status 401 and type "unauthorized"

  @SIMPL-30959 @D68
  Scenario: A user with contract_manager only can read the overview
    When I send "GET" to the provider UIBE at "/contracts" as "provider-operator"
    Then the response status is 200
    And the response field "items" is present
