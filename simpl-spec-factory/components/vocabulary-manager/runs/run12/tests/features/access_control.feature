Feature: Role-based access control on every operation
  The service checks the bearer token and the realm roles itself (D16, D17, D40, D84, E5, E7).
  401 and 403 come before any body or path check (D17, D51, D162).

  @AC_SIMPL-18196_AC3 @AC_SIMPL-18196_AC4 @AC_SIMPL-18173_AC5 @D17 @D16 @D84 @D48 @negative @OWASP-02 @ARCH-32 @SEC-05 @API-46 @API-47 @BLG-err-problem @API-36
  Scenario Outline: Without a token every secured operation answers 401 (<method> <path>)
    Given I am not authenticated
    When I send "<method>" to "<path>"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And the problem title is "Unauthorized"

    Examples:
      | method | path                                               |
      | GET    | /vocabularies                                      |
      | POST   | /vocabularies                                      |
      | POST   | /vocabularies/searches                             |
      | GET    | /vocabularies/Unknown{{run}}                       |
      | PATCH  | /vocabularies/Unknown{{run}}                       |
      | GET    | /vocabularies/Unknown{{run}}/versions              |
      | POST   | /vocabularies/Unknown{{run}}/versions              |
      | GET    | /vocabularies/Unknown{{run}}/versions/1            |
      | PATCH  | /vocabularies/Unknown{{run}}/versions/1            |
      | GET    | /content/Unknown{{run}}                            |
      | GET    | /content/Unknown{{run}}/versions/1                 |
      | GET    | /external-vocabularies                             |
      | POST   | /external-vocabularies                             |
      | POST   | /external-vocabulary/searches                      |
      | POST   | /external-vocabularies/content                     |
      | GET    | /external-vocabularies/Unknown{{run}}              |
      | GET    | /external-vocabularies/Unknown{{run}}/content      |
      | DELETE | /external-vocabularies/Unknown{{run}}              |

  @AC_SIMPL-18196_AC3 @D17 @E5 @E7 @D132 @negative @OWASP-08 @OWT-A07
  Scenario Outline: A token that is <kind> is refused with 401
    Given I use a bearer token that is "<kind>"
    When I send "GET" to "/vocabularies"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And the problem title is "Unauthorized"

    Examples:
      | kind          |
      | garbage       |
      | bad-signature |
      | edited-claims |
      | unsigned      |
      | edited-expiry |
      | no-audience   |
      | other-issuer  |

  @AC_SIMPL-18196_AC3 @D17 @E5 @E7 @negative @OWASP-08
  Scenario Outline: A write with a token that is <kind> is refused with 401 and stores nothing
    Given I use a bearer token that is "<kind>"
    When I send "POST" to "/vocabularies" with a multipart body
      | field          | value             |
      | name           | {{Health}}        |
      | description    | A description     |
      | changelog      | Initial version.  |
      | vocabularyFile | file:valid.ttl    |
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And no vocabulary named "{{Health}}" is stored

    Examples:
      | kind          |
      | no-audience   |
      | other-issuer  |
      | bad-signature |

  @AC_SIMPL-18196_AC2 @AC_SIMPL-18162_AC3 @D16 @D17 @D162 @D48 @negative @OWASP-02 @SEC-05 @API-46 @API-47
  Scenario Outline: A user without a vocabulary role gets 403 on every secured operation (<method> <path>)
    Given I am authenticated as "ga-norole"
    When I send "<method>" to "<path>"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And the problem title is "Forbidden"
    And the response field "items" is absent

    Examples:
      | method | path                                               |
      | GET    | /vocabularies                                      |
      | POST   | /vocabularies                                      |
      | POST   | /vocabularies/searches                             |
      | GET    | /vocabularies/Unknown{{run}}                       |
      | PATCH  | /vocabularies/Unknown{{run}}                       |
      | GET    | /vocabularies/Unknown{{run}}/versions              |
      | POST   | /vocabularies/Unknown{{run}}/versions              |
      | GET    | /vocabularies/Unknown{{run}}/versions/1            |
      | PATCH  | /vocabularies/Unknown{{run}}/versions/1            |
      | GET    | /content/Unknown{{run}}                            |
      | GET    | /content/Unknown{{run}}/versions/1                 |
      | GET    | /external-vocabularies                             |
      | POST   | /external-vocabularies                             |
      | POST   | /external-vocabulary/searches                      |
      | POST   | /external-vocabularies/content                     |
      | GET    | /external-vocabularies/Unknown{{run}}              |
      | GET    | /external-vocabularies/Unknown{{run}}/content      |
      | DELETE | /external-vocabularies/Unknown{{run}}              |

  @AC_SIMPL-18196_AC2 @AC_SIMPL-18173_AC4 @D16 @D17 @D46 @negative @OWASP-02 @SEC-05
  Scenario Outline: A viewer gets 403 on write operations (<method> <path>)
    Given I am authenticated as "ga-viewer"
    When I send "<method>" to "<path>"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"

    Examples:
      | method | path                                       |
      | POST   | /vocabularies                              |
      | PATCH  | /vocabularies/Unknown{{run}}               |
      | POST   | /vocabularies/Unknown{{run}}/versions      |
      | PATCH  | /vocabularies/Unknown{{run}}/versions/1    |
      | POST   | /external-vocabularies                     |
      | DELETE | /external-vocabularies/Unknown{{run}}      |

  @AC_SIMPL-18196_AC2 @AC_SIMPL-18173_AC4 @D16 @D46 @negative @OWASP-02
  Scenario: A viewer cannot create a vocabulary with a valid request, and nothing is stored
    Given I am authenticated as "ga-viewer"
    When I send "POST" to "/vocabularies" with a multipart body
      | field          | value            |
      | name           | {{Health}}       |
      | description    | A description    |
      | changelog      | Initial version. |
      | vocabularyFile | file:valid.ttl   |
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And no vocabulary named "{{Health}}" is stored

  @AC_SIMPL-18173_AC4 @D16 @D46 @negative @OWASP-02
  Scenario Outline: A <user> cannot create a new version, and nothing changes
    Given a vocabulary "{{Health}}" exists
    And I am authenticated as "<user>"
    When I create a new version of "{{Health}}"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And vocabulary "{{Health}}" has 1 version
    And version 1 of vocabulary "{{Health}}" has status "ACTIVE"

    Examples:
      | user      |
      | ga-viewer |
      | ga-norole |

  @AC_SIMPL-18196_AC2 @D16 @negative @OWASP-02
  Scenario: A viewer cannot change the status of a vocabulary
    Given a vocabulary "{{Health}}" exists
    And I am authenticated as "ga-viewer"
    When I send "PATCH" to "/vocabularies/{{Health}}" with JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And vocabulary "{{Health}}" has status "ACTIVE"

  @AC_SIMPL-18196_AC1 @AC_SIMPL-18176_AC1 @D16 @SEC-05
  Scenario Outline: A viewer may use every read operation (<method> <path>)
    Given a vocabulary "{{Health}}" exists
    And I am authenticated as "ga-viewer"
    When I send "<method>" to "<path>"
    Then the status is 200

    Examples:
      | method | path                                    |
      | GET    | /vocabularies                           |
      | GET    | /vocabularies/{{Health}}                |
      | GET    | /vocabularies/{{Health}}/versions       |
      | GET    | /vocabularies/{{Health}}/versions/1     |
      | GET    | /content/{{Health}}                     |
      | GET    | /content/{{Health}}/versions/1          |
      | GET    | /external-vocabularies                  |

  @AC_SIMPL-18196_AC1 @D16 @SEC-05
  Scenario: A viewer may use the vocabulary search
    Given a vocabulary "{{Health}}" exists
    And I am authenticated as "ga-viewer"
    When I send "POST" to "/vocabularies/searches" with JSON body
      """
      {"items": [{"name": "{{Health}}"}]}
      """
    Then the status is 200
    And the response field "items.0.name" equals "{{Health}}"

  @AC_SIMPL-18196_AC1 @D16 @SEC-05
  Scenario: A viewer may use the external vocabulary search and content lookup
    Given an external vocabulary "{{Ext}}" is registered with namespace "https://example.org/ext/{{run}}/terms/"
    And I am authenticated as "ga-viewer"
    When I send "POST" to "/external-vocabulary/searches" with JSON body
      """
      {"namespaceUri": "https://example.org/ext/{{run}}/terms/"}
      """
    Then the status is 200
    And the response field "items.0.name" equals "{{Ext}}"
    When I send "POST" to "/external-vocabularies/content" with JSON body
      """
      {"namespaceUri": "https://example.org/ext/{{run}}/terms/"}
      """
    Then the status is 200
    When I send "GET" to "/external-vocabularies/{{Ext}}"
    Then the status is 200
    When I send "GET" to "/external-vocabularies/{{Ext}}/content"
    Then the status is 200

  @AC_SIMPL-18196_AC1 @D16 @SEC-05
  Scenario: An admin may read and write
    Given I am authenticated as "ga-admin"
    When I create vocabulary "{{Health}}"
    Then the status is 201
    When I send "GET" to "/vocabularies/{{Health}}"
    Then the status is 200

  @D17 @D51 @negative
  Scenario: Authentication runs before body validation
    Given I am not authenticated
    When I send "POST" to "/vocabularies" with a multipart body
      | field | value |
      | name  | lower |
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"

  @D17 @D51 @negative
  Scenario: The role check runs before body validation
    Given I am authenticated as "ga-viewer"
    When I send "POST" to "/vocabularies" with a multipart body
      | field | value |
      | name  | lower |
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"

  @D17 @D51 @D63 @negative
  Scenario: The role check runs before the media type and Accept checks on writes
    Given I am authenticated as "ga-viewer"
    And the request header "Accept" is "application/xml"
    When I send "POST" to "/vocabularies" with body "x" and content type "text/plain"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"

  @D162 @negative
  Scenario: On reads 403 comes before 400 for an invalid query value
    Given I am authenticated as "ga-norole"
    When I send "GET" to "/vocabularies?pageSize=0"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"

  @D162 @negative
  Scenario: On reads 401 comes before 404
    Given I am not authenticated
    When I send "GET" to "/vocabularies/Unknown{{run}}/versions/7"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"

  @D67 @D84 @D155 @negative @ARCH-32
  Scenario: An unknown path gives 401 without a token
    Given I am not authenticated
    When I send "GET" to "/no-such-resource-{{run}}"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"

  @D67 @D84 @D55 @negative
  Scenario: An unknown path gives 404 problem+json with a token
    Given I am authenticated as "ga-admin"
    When I send "GET" to "/no-such-resource-{{run}}"
    Then the status is 404
    And the response media type is "application/problem+json"
    And the response field "status" equals 404

  @D67 @negative
  Scenario: Actuator paths do not exist on the API port
    Given I am not authenticated
    When I send "GET" to "/actuator/health"
    Then the status is 401
    Given I am authenticated as "ga-admin"
    When I send "GET" to "/actuator/health"
    Then the status is 404
    When I send "GET" to "/actuator/prometheus"
    Then the status is 404

  @D139 @D67 @D55 @negative @BLG-meth-http
  Scenario Outline: A known method the path does not declare gives 405 with Allow, even without a token (<method> <path>)
    Given I am not authenticated
    When I send "<method>" to "<path>"
    Then the status is 405
    And the response media type is "application/problem+json"
    And the response header "Allow" is present

    Examples:
      | method | path                                  |
      | PUT    | /vocabularies                         |
      | DELETE | /vocabularies                         |
      | DELETE | /vocabularies/Unknown{{run}}          |
      | PUT    | /external-vocabularies/Unknown{{run}} |

  @D77 @negative
  Scenario Outline: A method that a fixed path does not offer gives 405 (<method> <path>)
    Given I am authenticated as "ga-admin"
    When I send "<method>" to "<path>"
    Then the status is 405
    And the response header "Allow" is present

    Examples:
      | method | path                            |
      | GET    | /vocabularies/searches          |
      | GET    | /external-vocabularies/content  |

  @D126 @negative
  Scenario Outline: <method> on a declared path gives 405 with Allow
    Given I am authenticated as "ga-admin"
    When I send "<method>" to "/vocabularies"
    Then the status is 405
    And the response header "Allow" is present

    Examples:
      | method  |
      | HEAD    |
      | OPTIONS |

  @D166 @D122 @negative
  Scenario: TRACE on a declared path gives 405 problem+json
    Given I am authenticated as "ga-admin"
    When I send "TRACE" to "/vocabularies"
    Then the status is 405
    And the response media type is "application/problem+json"

  @D67 @negative
  Scenario: A method the server does not know gives 405 before the token check
    Given I am not authenticated
    When I send "QUERY" to "/vocabularies"
    Then the status is 405

  @D67 @D158 @negative
  Scenario: An encoded slash in the path is refused with 400 before the token check
    Given I am not authenticated
    When I send "GET" to "/vocabularies/Abc%2FDef"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has no issues
