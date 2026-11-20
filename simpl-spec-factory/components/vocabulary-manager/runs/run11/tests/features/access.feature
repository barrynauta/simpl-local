Feature: Access control on the Vocabulary Management API
  Every secured operation checks the bearer token and the realm roles itself (D16, D17).
  GA_VOCABULARY_VIEWER may read, GA_VOCABULARY_ADMIN may read and write.

  @AC_SIMPL-18196_AC3 @AC_SIMPL-18196_AC4 @AC_SIMPL-18173_AC5 @D17 @D16 @D84 @D39 @D79 @negative @ARCH-32 @OWASP-02 @OWASP-08 @API-46 @BLG-err-problem
  Scenario Outline: A request without a token gets 401 on every secured operation: <operation>
    Given I am not authenticated
    When I call the operation "<operation>"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And the problem title is "Unauthorized"

    Examples:
      | operation                                  |
      | GET /vocabularies                          |
      | POST /vocabularies                         |
      | POST /vocabularies/searches                |
      | GET /vocabularies/${A}                     |
      | PATCH /vocabularies/${A}                   |
      | GET /vocabularies/${A}/versions            |
      | POST /vocabularies/${A}/versions           |
      | GET /vocabularies/${A}/versions/1          |
      | PATCH /vocabularies/${A}/versions/1        |
      | GET /content/${A}                          |
      | GET /content/${A}/versions/1               |
      | GET /external-vocabularies                 |
      | POST /external-vocabularies                |
      | POST /external-vocabulary/searches         |
      | POST /external-vocabularies/content        |
      | GET /external-vocabularies/${X}/content    |
      | GET /external-vocabularies/${X}            |
      | DELETE /external-vocabularies/${X}         |

  @AC_SIMPL-18196_AC2 @AC_SIMPL-18162_AC3 @AC_SIMPL-18173_AC4 @D16 @D17 @D84 @E1 @D40 @negative @SEC-05 @OWASP-02 @API-46 @BLG-err-problem
  Scenario Outline: A user without a vocabulary role gets 403 on every secured operation: <operation>
    Given I am a user without a vocabulary role
    When I call the operation "<operation>"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And the problem title is "Forbidden"

    Examples:
      | operation                                  |
      | GET /vocabularies                          |
      | POST /vocabularies                         |
      | POST /vocabularies/searches                |
      | GET /vocabularies/${A}                     |
      | PATCH /vocabularies/${A}                   |
      | GET /vocabularies/${A}/versions            |
      | POST /vocabularies/${A}/versions           |
      | GET /vocabularies/${A}/versions/1          |
      | PATCH /vocabularies/${A}/versions/1        |
      | GET /content/${A}                          |
      | GET /content/${A}/versions/1               |
      | GET /external-vocabularies                 |
      | POST /external-vocabularies                |
      | POST /external-vocabulary/searches         |
      | POST /external-vocabularies/content        |
      | GET /external-vocabularies/${X}/content    |
      | GET /external-vocabularies/${X}            |
      | DELETE /external-vocabularies/${X}         |

  @AC_SIMPL-18196_AC2 @AC_SIMPL-18173_AC4 @D16 @D17 @D46 @negative @SEC-05 @OWASP-02
  Scenario Outline: A viewer gets 403 on the write operation <operation>
    Given a vocabulary "A" exists
    And an external vocabulary "X" is registered with namespace "${URI:X}"
    And I am a vocabulary viewer
    When I call the operation "<operation>"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And the problem title is "Forbidden"

    Examples:
      | operation                           |
      | POST /vocabularies                  |
      | PATCH /vocabularies/${A}            |
      | POST /vocabularies/${A}/versions    |
      | PATCH /vocabularies/${A}/versions/1 |
      | POST /external-vocabularies         |
      | DELETE /external-vocabularies/${X}  |

  @AC_SIMPL-18196_AC1 @AC_SIMPL-18176_AC1 @D16 @SEC-05
  Scenario Outline: A viewer may use the read operation <operation>
    Given a vocabulary "A" exists
    And an external vocabulary "X" is registered with namespace "${URI:X}"
    And I am a vocabulary viewer
    When I call the operation "<operation>"
    Then the response status is 200

    Examples:
      | operation                               |
      | GET /vocabularies                       |
      | POST /vocabularies/searches             |
      | GET /vocabularies/${A}                  |
      | GET /vocabularies/${A}/versions         |
      | GET /vocabularies/${A}/versions/1       |
      | GET /content/${A}                       |
      | GET /content/${A}/versions/1            |
      | GET /external-vocabularies              |
      | POST /external-vocabulary/searches      |
      | POST /external-vocabularies/content     |
      | GET /external-vocabularies/${X}/content |
      | GET /external-vocabularies/${X}         |

  @AC_SIMPL-18196_AC1 @D16 @E1 @D40
  Scenario Outline: An administrator may use the operation <operation>
    Given a vocabulary "A" exists
    And an external vocabulary "X" is registered with namespace "${URI:X}"
    And I am a vocabulary administrator
    When I call the operation "<operation>"
    Then the response status is <status>

    Examples:
      | operation                               | status |
      | GET /vocabularies                       | 200    |
      | POST /vocabularies                      | 201    |
      | POST /vocabularies/searches             | 200    |
      | GET /vocabularies/${A}                  | 200    |
      | PATCH /vocabularies/${A}                | 200    |
      | GET /vocabularies/${A}/versions         | 200    |
      | POST /vocabularies/${A}/versions        | 201    |
      | GET /vocabularies/${A}/versions/1       | 200    |
      | PATCH /vocabularies/${A}/versions/1     | 200    |
      | GET /content/${A}                       | 200    |
      | GET /content/${A}/versions/1            | 200    |
      | GET /external-vocabularies              | 200    |
      | POST /external-vocabularies             | 201    |
      | POST /external-vocabulary/searches      | 200    |
      | POST /external-vocabularies/content     | 200    |
      | GET /external-vocabularies/${X}/content | 200    |
      | GET /external-vocabularies/${X}         | 200    |
      | DELETE /external-vocabularies/${X}      | 204    |

  @AC_SIMPL-18196_AC3 @D17 @E5 @E7 @D132 @negative @OWT-A07 @OWASP-08
  Scenario Outline: An invalid token gets 401: <token>
    Given <token>
    When I send a GET request to "/vocabularies"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And the problem title is "Unauthorized"

    Examples:
      | token                                                                      |
      | I use a malformed bearer token                                             |
      | I use an administrator token with a broken signature                       |
      | I use an administrator token without the vocabulary-manager-be audience   |
      | I use an administrator token from another issuer                           |

  @AC_SIMPL-18173_AC5 @D17 @E5 @E7 @negative @OWT-A07
  Scenario: A token without the audience cannot create a version
    Given a vocabulary "A" exists
    And a new version request for vocabulary "A"
    And I use an administrator token without the vocabulary-manager-be audience
    When I submit the request
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And vocabulary "A" has 1 version

  @D17 @D51 @negative
  Scenario: Authentication is checked before the request body
    Given a create vocabulary request for "A"
    And the request field "name" is "lowercase"
    And the request field "description" is removed
    And I am not authenticated
    When I submit the request
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"

  @D17 @D51 @negative
  Scenario: The role is checked before the request body
    Given a create vocabulary request for "A"
    And the request field "name" is "lowercase"
    And the request field "description" is removed
    And I am a vocabulary viewer
    When I submit the request
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"

  @D17 @D51 @negative
  Scenario: The role is checked before the existence of the vocabulary
    Given I am a vocabulary viewer
    When I send a PATCH request to "/vocabularies/${Missing}" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"

  @AC_SIMPL-18196_AC2 @D16 @D46 @D8 @negative @OWASP-02
  Scenario: A viewer who tries to create a vocabulary stores nothing
    Given a create vocabulary request for "A"
    And I am a vocabulary viewer
    When I submit the request
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And no vocabulary "A" is stored

  @AC_SIMPL-18196_AC2 @D46 @D16 @negative
  Scenario: A viewer who tries to create a version changes nothing
    Given a vocabulary "A" exists
    And a new version request for vocabulary "A"
    And I am a vocabulary viewer
    When I submit the request
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And vocabulary "A" has 1 version
    And version 1 of vocabulary "A" has status "ACTIVE"

  @D16 @negative
  Scenario: A viewer who tries to change a status changes nothing
    Given a vocabulary "A" exists
    And I am a vocabulary viewer
    When I send a PATCH request to "/vocabularies/${A}" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And vocabulary "A" has status "ACTIVE"

  @D16 @D41 @negative
  Scenario: A viewer who tries to delete an external vocabulary removes nothing
    Given an external vocabulary "X" is registered with namespace "${URI:X}"
    And I am a vocabulary viewer
    When I send a DELETE request to "/external-vocabularies/${X}"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And the stored external vocabulary "X" has namespace "${URI:X}"

  @D17 @D44 @D108 @MACH-08 @BLG-hlth-res @NFR-07
  Scenario: The health operation needs no token
    Given I am not authenticated
    When I send a GET request to "/health"
    Then the response status is 200
    And the response field "status" is "UP"

  @D67 @D84 @negative
  Scenario: An unknown path gets 401 without a token
    Given I am not authenticated
    When I send a GET request to "/no-such-resource"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"

  @D67 @D84 @negative
  Scenario: An unknown path gets 404 with a token
    Given I am a vocabulary administrator
    When I send a GET request to "/no-such-resource"
    Then the response is a problem with status 404

  @D67 @negative @OWT-A02
  Scenario Outline: Actuator paths do not exist on the API port: <path>
    Given I am <who>
    When I send a GET request to "<path>"
    Then the response is a problem with status <status>

    Examples:
      | who                         | path                   | status |
      | not authenticated           | //actuator/health      | 401    |
      | a vocabulary administrator  | //actuator/health      | 404    |
      | not authenticated           | //actuator/prometheus  | 401    |
      | a vocabulary administrator  | //actuator/prometheus  | 404    |
      | not authenticated           | /actuator/health       | 401    |
      | a vocabulary administrator  | /actuator/health       | 404    |
