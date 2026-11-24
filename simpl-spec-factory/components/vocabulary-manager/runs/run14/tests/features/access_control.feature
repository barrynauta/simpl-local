Feature: Role-based access control of the Vocabulary Manager API
  Two realm roles (D16): GA_VOCABULARY_VIEWER reads, GA_VOCABULARY_ADMIN reads and writes.
  The service checks the bearer token and the roles itself (D17), before the body (D51, D162).
  Test users of E1: ga-admin, ga-viewer, ga-norole.

  @AC_SIMPL-18196_AC3 @AC_SIMPL-18196_AC4 @AC_SIMPL-18173_AC5 @D17 @D16 @D84 @D197 @E1 @negative @OWASP-02 @ARCH-32 @API-46 @API-47 @BLG-err-problem @SEC-05
  Scenario Outline: Every secured operation answers 401 without a token: <method> <path>
    Given I am not authenticated
    When I send a <method> request to "<path>"
    Then the response is a problem with status 401
    And the problem type is "urn:problem-type:simpl:unauthorized"
    And the problem title is "Unauthorized"
    And the response holds no items

    Examples:
      | method | path                                  |
      | GET    | /vocabularies                         |
      | POST   | /vocabularies                         |
      | POST   | /vocabularies/searches                |
      | GET    | /vocabularies/$Alpha                  |
      | PATCH  | /vocabularies/$Alpha                  |
      | GET    | /vocabularies/$Alpha/versions         |
      | POST   | /vocabularies/$Alpha/versions         |
      | GET    | /vocabularies/$Alpha/versions/1       |
      | PATCH  | /vocabularies/$Alpha/versions/1       |
      | GET    | /content/$Alpha                       |
      | GET    | /content/$Alpha/versions/1            |
      | GET    | /external-vocabularies                |
      | POST   | /external-vocabularies                |
      | POST   | /external-vocabulary/searches         |
      | POST   | /external-vocabularies/content        |
      | GET    | /external-vocabularies/$Ext/content   |
      | GET    | /external-vocabularies/$Ext           |
      | DELETE | /external-vocabularies/$Ext           |

  @AC_SIMPL-18196_AC2 @AC_SIMPL-18162_AC3 @D16 @D17 @D84 @D197 @E1 @negative @OWASP-02 @API-46 @SEC-05
  Scenario Outline: A user without a vocabulary role gets 403 on every secured operation: <method> <path>
    Given a vocabulary "$Alpha" exists
    And an external vocabulary "$Ext" is registered with namespace "$EXT/ac"
    And I am authenticated as "ga-norole"
    When I send a <method> request to "<path>"
    Then the response is a problem with status 403
    And the problem type is "urn:problem-type:simpl:forbidden"
    And the problem title is "Forbidden"
    And the response holds no items

    Examples:
      | method | path                                  |
      | GET    | /vocabularies                         |
      | POST   | /vocabularies                         |
      | POST   | /vocabularies/searches                |
      | GET    | /vocabularies/$Alpha                  |
      | PATCH  | /vocabularies/$Alpha                  |
      | GET    | /vocabularies/$Alpha/versions         |
      | POST   | /vocabularies/$Alpha/versions         |
      | GET    | /vocabularies/$Alpha/versions/1       |
      | PATCH  | /vocabularies/$Alpha/versions/1       |
      | GET    | /content/$Alpha                       |
      | GET    | /content/$Alpha/versions/1            |
      | GET    | /external-vocabularies                |
      | POST   | /external-vocabularies                |
      | POST   | /external-vocabulary/searches         |
      | POST   | /external-vocabularies/content        |
      | GET    | /external-vocabularies/$Ext/content   |
      | GET    | /external-vocabularies/$Ext           |
      | DELETE | /external-vocabularies/$Ext           |

  @AC_SIMPL-18196_AC2 @AC_SIMPL-18173_AC4 @D16 @D17 @D46 @negative @OWASP-02 @API-46
  Scenario Outline: A viewer gets 403 on every write operation: <method> <path>
    Given a vocabulary "$Alpha" exists
    And an external vocabulary "$Ext" is registered with namespace "$EXT/vw"
    And I am authenticated as "ga-viewer"
    When I send a <method> request to "<path>"
    Then the response is a problem with status 403
    And the problem type is "urn:problem-type:simpl:forbidden"
    And the problem title is "Forbidden"
    And vocabulary "$Alpha" has 1 version
    And vocabulary "$Alpha" has vocabulary status "ACTIVE"
    And version 1 of vocabulary "$Alpha" has version status "ACTIVE"
    And the external vocabulary "$Ext" is registered

    Examples:
      | method | path                                  |
      | POST   | /vocabularies                         |
      | PATCH  | /vocabularies/$Alpha                  |
      | POST   | /vocabularies/$Alpha/versions         |
      | PATCH  | /vocabularies/$Alpha/versions/1       |
      | POST   | /external-vocabularies                |
      | DELETE | /external-vocabularies/$Ext           |

  @AC_SIMPL-18196_AC2 @D16 @D17 @D51 @negative @OWASP-02
  Scenario: A viewer who sends a complete and valid new version still gets 403 and nothing is created
    Given a vocabulary "$Alpha" exists
    And I am authenticated as "ga-viewer"
    When I create a new version of "$Alpha"
    Then the response is a problem with status 403
    And the problem type is "urn:problem-type:simpl:forbidden"
    And vocabulary "$Alpha" has 1 version

  @AC_SIMPL-18196_AC2 @D16 @D17 @negative @OWASP-02
  Scenario: A viewer who sends a complete and valid creation gets 403 and nothing is stored
    Given I am authenticated as "ga-viewer"
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 403
    And 0 vocabularies have a name containing "$RUN"

  @AC_SIMPL-18196_AC1 @AC_SIMPL-18176_AC1 @D16 @OWASP-02 @SEC-05
  Scenario Outline: Both roles may use every read operation: <user> <method> <path>
    Given a vocabulary "$Alpha" exists
    And an external vocabulary "$Ext" is registered with namespace "$EXT/rd"
    And I am authenticated as "<user>"
    When I send a <method> request to "<path>"
    Then the response status is 200
    And the response has a Content-Type header

    Examples:
      | user      | method | path                                |
      | ga-viewer | GET    | /vocabularies                       |
      | ga-viewer | GET    | /vocabularies/$Alpha                |
      | ga-viewer | GET    | /vocabularies/$Alpha/versions       |
      | ga-viewer | GET    | /vocabularies/$Alpha/versions/1     |
      | ga-viewer | GET    | /content/$Alpha                     |
      | ga-viewer | GET    | /content/$Alpha/versions/1          |
      | ga-viewer | GET    | /external-vocabularies              |
      | ga-viewer | GET    | /external-vocabularies/$Ext         |
      | ga-viewer | GET    | /external-vocabularies/$Ext/content |
      | ga-admin  | GET    | /vocabularies                       |
      | ga-admin  | GET    | /vocabularies/$Alpha                |
      | ga-admin  | GET    | /vocabularies/$Alpha/versions       |
      | ga-admin  | GET    | /vocabularies/$Alpha/versions/1     |
      | ga-admin  | GET    | /content/$Alpha                     |
      | ga-admin  | GET    | /content/$Alpha/versions/1          |
      | ga-admin  | GET    | /external-vocabularies              |
      | ga-admin  | GET    | /external-vocabularies/$Ext         |
      | ga-admin  | GET    | /external-vocabularies/$Ext/content |

  @AC_SIMPL-18196_AC1 @D16 @D38 @OWASP-02
  Scenario: A viewer may search vocabularies
    Given a vocabulary "$Alpha" exists
    And I am authenticated as "ga-viewer"
    When I send a POST request to "/vocabularies/searches" with JSON:
      """
      {"items": [{"name": "$Alpha"}]}
      """
    Then the response status is 200
    And the response items are named "$Alpha"

  @AC_SIMPL-18196_AC1 @D16 @D38 @OWASP-02
  Scenario: A viewer may search external vocabularies
    Given an external vocabulary "$Ext" is registered with namespace "$EXT/vs"
    And I am authenticated as "ga-viewer"
    When I send a POST request to "/external-vocabulary/searches" with JSON:
      """
      {"namespaceUri": "$EXT/vs"}
      """
    Then the response status is 200
    And the response items are named "$Ext"

  @AC_SIMPL-18196_AC1 @D16 @D41 @OWASP-02
  Scenario: A viewer may download external content by namespace
    Given an external vocabulary "$Ext" is registered with namespace "$EXT/vc"
    And I am authenticated as "ga-viewer"
    When I send a POST request to "/external-vocabularies/content" with JSON:
      """
      {"namespaceUri": "$EXT/vc"}
      """
    Then the response status is 200
    And the response body is the uploaded file of the external vocabulary "$Ext"

  @AC_SIMPL-18196_AC1 @D16 @OWASP-02
  Scenario: The admin may use the write operations
    Given I am authenticated as "ga-admin"
    When I create vocabulary "$Alpha"
    Then the response status is 201
    When I create a new version of "$Alpha"
    Then the response status is 201
    When I send a PATCH request to "/vocabularies/$Alpha" with JSON:
      """
      {"status": "DEPRECATED"}
      """
    Then the response status is 200
    When I send a PATCH request to "/vocabularies/$Alpha/versions/1" with JSON:
      """
      {"status": "ACTIVE"}
      """
    Then the response status is 200

  @AC_SIMPL-18196_AC3 @AC_SIMPL-18196_AC4 @D17 @E5 @E7 @negative @OWASP-08 @OWT-A07
  Scenario: A token without the audience of the service gets 401
    Given I use a "ga-admin" token without the audience of the service
    When I send a GET request to "/vocabularies"
    Then the response is a problem with status 401
    And the problem type is "urn:problem-type:simpl:unauthorized"
    And the problem title is "Unauthorized"

  @AC_SIMPL-18196_AC3 @D17 @E5 @E7 @negative @OWASP-08 @OWT-A07
  Scenario: A token of another issuer gets 401
    Given I use a "ga-admin" token from the other realm
    When I send a GET request to "/vocabularies"
    Then the response is a problem with status 401
    And the problem type is "urn:problem-type:simpl:unauthorized"

  @AC_SIMPL-18196_AC3 @D17 @E8 @negative @OWASP-08 @OWT-A07 @slow
  Scenario: An expired token gets 401
    Given I use a "ga-admin" token that has expired
    When I send a GET request to "/vocabularies"
    Then the response is a problem with status 401
    And the problem type is "urn:problem-type:simpl:unauthorized"

  @AC_SIMPL-18196_AC3 @D17 @negative @OWASP-08 @OWT-A07
  Scenario: A token with a broken signature gets 401, also for a write
    Given I use a "ga-admin" token with a broken signature
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 401
    And the problem type is "urn:problem-type:simpl:unauthorized"
    And 0 vocabularies have a name containing "$RUN"

  @AC_SIMPL-18196_AC3 @D17 @negative @OWASP-08
  Scenario Outline: A malformed authorization header gets 401: <value>
    Given I send the authorization header "<value>"
    When I send a GET request to "/vocabularies"
    Then the response is a problem with status 401
    And the problem type is "urn:problem-type:simpl:unauthorized"

    Examples:
      | value                          |
      | Bearer not-a-jwt               |
      | Bearer                         |
      | Basic Z2EtYWRtaW46UGFzc3dvcmQxIQ== |

  @D17 @D51 @negative
  Scenario: Authentication is checked before the media type and the body
    Given I am not authenticated
    When I send a POST request to "/vocabularies" with the body "not a form" as "text/plain"
    Then the response is a problem with status 401
    And the problem type is "urn:problem-type:simpl:unauthorized"

  @D17 @D51 @negative
  Scenario: The role is checked before the media type and the body
    Given I am authenticated as "ga-viewer"
    When I send a POST request to "/vocabularies" with the body "not a form" as "text/plain"
    Then the response is a problem with status 403
    And the problem type is "urn:problem-type:simpl:forbidden"

  @D162 @D17 @negative
  Scenario: On reads the role is checked before a bad query and before 404
    Given I am authenticated as "ga-norole"
    When I send a GET request to "/vocabularies/$Missing/versions?page=0"
    Then the response is a problem with status 403
    And the problem type is "urn:problem-type:simpl:forbidden"

  @D67 @D84 @D155 @negative @OWASP-02
  Scenario Outline: A path that matches no operation gives 401 without a token and 404 with one: <path>
    Given I am not authenticated
    When I send a GET request to "<path>"
    Then the response is a problem with status 401
    Given I am authenticated as "ga-admin"
    When I send a GET request to "<path>"
    Then the response is a problem with status 404

    Examples:
      | path                         |
      | /unknown-resource            |
      | /actuator                    |
      | /actuator/health             |
      | /actuator/prometheus         |
      | /vocabularies/$Alpha/unknown |

  @D67 @D172 @negative
  Scenario: A method the server does not know gets 405 before the token is checked
    Given I am not authenticated
    When I send a QUERY request to "/vocabularies"
    Then the response is a problem with status 405

  @D172 @D67 @negative
  Scenario: A method the server does not know gets 405 on an unknown path too
    Given I am not authenticated
    When I send a QUERY request to "/no-such-path"
    Then the response is a problem with status 405
    And the response header "Allow" is absent

  @D139 @D67 @negative
  Scenario Outline: A known method the path does not declare gets 405 with Allow: <method> <path> as <user>
    Given I am authenticated as "<user>"
    When I send a <method> request to "<path>"
    Then the response is a problem with status 405
    And the response header "Allow" lists "<allow>"

    Examples:
      | user      | method | path                              | allow       |
      | ga-admin  | PUT    | /vocabularies                     | GET, POST   |
      | ga-admin  | DELETE | /vocabularies/$Alpha              | GET, PATCH  |
      | ga-admin  | PUT    | /external-vocabularies/$Ext       | GET, DELETE |
      | ga-admin  | POST   | /content/$Alpha                   | GET         |
      | ga-norole | PUT    | /vocabularies                     | GET, POST   |

  @D139 @D67 @negative
  Scenario: A known method the path does not declare gets 405 also without a token
    Given I am not authenticated
    When I send a PUT request to "/vocabularies"
    Then the response is a problem with status 405
    And the response header "Allow" lists "GET, POST"

  @D77 @negative
  Scenario Outline: A fixed path is never read as a name: <method> <path>
    Given I am authenticated as "ga-admin"
    When I send a <method> request to "<path>"
    Then the response is a problem with status 405

    Examples:
      | method | path                           |
      | GET    | /vocabularies/searches         |
      | GET    | /external-vocabularies/content |

  @D126 @negative
  Scenario Outline: HEAD and OPTIONS on a declared path get 405 with Allow: <method>
    Given I am authenticated as "ga-admin"
    When I send a <method> request to "/vocabularies"
    Then the response status is 405
    And the response header "Allow" lists "GET, POST"

    Examples:
      | method  |
      | HEAD    |
      | OPTIONS |

  @D172 @negative
  Scenario: HEAD on an unknown path behaves as an unknown path
    Given I am not authenticated
    When I send a HEAD request to "/no-such-path"
    Then the response status is 401
    Given I am authenticated as "ga-admin"
    When I send a HEAD request to "/no-such-path"
    Then the response status is 404

  @D166 @D178 @D122 @negative
  Scenario: TRACE on a declared path gets 405 with Allow
    Given I am authenticated as "ga-admin"
    When I send a TRACE request to "/vocabularies"
    Then the response is a problem with status 405
    And the response header "Allow" lists "GET, POST"

  @D67 @D158 @negative
  Scenario: A path with an encoded slash is refused before the token check
    Given I am not authenticated
    When I send a GET request to "/vocabularies/Abc%2FDef"
    Then the response is a problem with status 400
    And the problem type is "urn:problem-type:belgif:badRequest"
    And the problem has no issues
