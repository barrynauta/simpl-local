Feature: API key authentication of the contract manager backend
  Every CM operation except the health check needs the CM's X-Api-Key.
  Missing or wrong key: 401. CM never answers 403 (D65).

  Background:
    Given fresh negotiation ids

  @openapi-only @D65 @rbac
  Scenario Outline: <method> <path> without an API key is refused with 401
    When I send "<method>" to the provider CM at "<path>" without an API key
    Then the response is a problem with status 401 and type "unauthorized"

    Examples: OpenAPI operations
      | method | path                                                      |
      | POST   | /credentials/agreements/${aid}/definitions/${did}/issue   |
      | POST   | /credentials/agreements/${aid}/definitions/${did}/create  |
      | PATCH  | /agreements/${aid}/definitions/${did}/status              |
      | GET    | /agreements/${aid}                                        |
      | GET    | /agreements/${aid}/file                                   |
      | POST   | /agreements/${nid}/search                                 |
      | POST   | /agreements/search                                        |
      | PUT    | /verifications/${nid}                                     |
      | POST   | /contractOffers/${nid}                                    |
      | PATCH  | /contractOffers/${nid}/accept                             |
      | PUT    | /contractOffers/${nid}/update                             |

    Examples: operations added by the digest
      | method | path                                  |
      | GET    | /verifications/${nid}/vc              |
      | GET    | /contracts                            |
      | GET    | /contracts/${nid}                     |
      | POST   | /contracts/${nid}/lifecycleActions    |
      | PUT    | /contracts/${nid}/consentStatus       |
      | GET    | /audit/contractStatusCounts           |
      | POST   | /contractClauseDocuments              |

  @openapi-only @D65 @rbac
  Scenario Outline: <method> <path> with a wrong API key is refused with 401, never 403
    When I send "<method>" to the provider CM at "<path>" with the API key "wrong-key"
    Then the response is a problem with status 401 and type "unauthorized"

    Examples:
      | method | path                                                     |
      | POST   | /agreements/${nid}/search                                |
      | GET    | /contracts                                               |
      | PATCH  | /contractOffers/${nid}/reject                            |
      | POST   | /credentials/agreements/${aid}/definitions/${did}/issue  |

  @openapi-only @D65 @rbac
  Scenario: The key of the other party's CM is not accepted
    When I send "GET" to the provider CM at "/contracts" with the API key "test-cm-consumer-key"
    Then the response is a problem with status 401 and type "unauthorized"

  @SIMPL-32509 @D65 @D19 @rbac
  Scenario: SIMPL-32509 AC5 - an unauthenticated existence check is refused with 401
    When I send "POST" to the provider CM at "/agreements/${nid}/search" without an API key
    Then the response is a problem with status 401 and type "unauthorized"

  @SIMPL-32509 @D65 @rbac
  Scenario: SIMPL-32509 AC6 - a caller without the right credentials gets 401 because CM never answers 403
    When I send "POST" to the provider CM at "/agreements/${nid}/search" with the API key "test-uibe-provider-key"
    Then the response is a problem with status 401 and type "unauthorized"

  @SIMPL-35526 @D65 @rbac @consumer
  Scenario: SIMPL-35526 AC2 - an unauthenticated create call is refused with 401
    When I send "POST" to the consumer CM at "/credentials/agreements/${aid}/definitions/${did}/create" without an API key
    Then the response is a problem with status 401 and type "unauthorized"

  @SIMPL-35584 @D65 @D4 @rbac
  Scenario: SIMPL-35584 AC2 - an unauthenticated validate-and-sign call is refused with 401
    When I send "POST" to the provider CM at "/credentials/agreements/${aid}/definitions/${did}/issue" without an API key
    Then the response is a problem with status 401 and type "unauthorized"

  @SIMPL-35660 @D65 @D4 @rbac @consumer
  Scenario: SIMPL-35660 AC2 - an unauthenticated offer decision is refused with 401
    When I send "PATCH" to the consumer CM at "/contractOffers/${nid}/accept" without an API key
    Then the response is a problem with status 401 and type "unauthorized"

  @SIMPL-35677 @D65 @D4 @rbac @consumer
  Scenario: SIMPL-35677 AC2 - an unauthenticated verification call is refused with 401
    When I send "PUT" to the consumer CM at "/verifications/${nid}" without an API key
    Then the response is a problem with status 401 and type "unauthorized"

  @SIMPL-35440 @D65 @D5 @rbac
  Scenario: SIMPL-35440 AC3 - an unauthenticated VC retrieval is refused with 401
    When I send "GET" to the provider CM at "/verifications/${nid}/vc" without an API key
    Then the response is a problem with status 401 and type "unauthorized"

  @SIMPL-21928 @D65 @rbac @consumer @stub
  Scenario: SIMPL-21928 AC3 - an unauthorised signing trigger is rejected and nothing is signed
    Given an offer created at the consumer
    When I send "PATCH" to the consumer CM at "/contractOffers/${nid}/accept" with the API key "wrong-key"
    Then the response is a problem with status 401 and type "unauthorized"
    And the consumer contract stays in status "REQUESTED"
    And the consumer contract field "consumerSignatureDate" is null
