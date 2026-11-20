@access
Feature: Role-based access control on the Vocabulary Management API
  Every secured operation checks the bearer token and the realm role itself (D16, D17, D84).
  No or invalid token gives 401, a valid token without the needed role gives 403,
  and both checks run before any body validation.

  @D48 @AC_SIMPL-18196_AC3 @D17 @D84 @D24 @OWASP-02 @OWASP-08 @ARCH-32 @API-46 @API-47 @negative
  Scenario Outline: No token on <method> <path> gives 401 Unauthorized
    Given I use no token
    When I send "<method>" to "<path>"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And the problem title is "Unauthorized"

    Examples:
      | method | path                                               |
      | GET    | /vocabularies                                      |
      | POST   | /vocabularies                                      |
      | POST   | /vocabularies/searches                             |
      | GET    | /vocabularies/AnyVocabulary                        |
      | PATCH  | /vocabularies/AnyVocabulary                        |
      | GET    | /vocabularies/AnyVocabulary/versions               |
      | GET    | /vocabularies/AnyVocabulary/versions/1             |
      | PATCH  | /vocabularies/AnyVocabulary/versions/1             |
      | GET    | /content/AnyVocabulary                             |
      | GET    | /content/AnyVocabulary/versions/1                  |
      | GET    | /external-vocabularies                             |
      | POST   | /external-vocabularies                             |
      | POST   | /external-vocabulary/searches                      |
      | POST   | /external-vocabularies/content                     |
      | GET    | /external-vocabularies/AnyVocabulary/content       |
      | GET    | /external-vocabularies/AnyVocabulary               |
      | DELETE | /external-vocabularies/AnyVocabulary               |

    @AC_SIMPL-18173_AC5
    Examples: new version without a token
      | method | path                                               |
      | POST   | /vocabularies/AnyVocabulary/versions               |

  @AC_SIMPL-18196_AC3 @D17 @E5 @E7 @D88 @OWASP-08 @negative
  Scenario Outline: An invalid token of kind <kind> gives 401 on <method> <path>
    Given I use an invalid token of kind "<kind>"
    When I send "<method>" to "<path>"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And the problem title is "Unauthorized"

    Examples:
      | kind        | method | path                   |
      | garbage     | GET    | /vocabularies          |
      | tampered    | GET    | /vocabularies          |
      | unsigned    | GET    | /vocabularies          |
      | unknownkid  | GET    | /vocabularies          |
      | noaud       | GET    | /vocabularies          |
      | otherissuer | GET    | /vocabularies          |
      | tampered    | POST   | /vocabularies          |
      | noaud       | POST   | /vocabularies          |
      | otherissuer | POST   | /vocabularies          |
      | unsigned    | POST   | /external-vocabularies |

  @D48 @AC_SIMPL-18196_AC2 @AC_SIMPL-18162_AC3 @D16 @D17 @D84 @OWASP-02 @SEC-05 @API-46 @API-47 @negative
  Scenario Outline: A user without a vocabulary role gets 403 on <method> <path>
    Given I use the token of "ga-norole"
    When I send "<method>" to "<path>"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And the problem title is "Forbidden"
    And the response field "items" is absent

    Examples:
      | method | path                                               |
      | GET    | /vocabularies                                      |
      | POST   | /vocabularies                                      |
      | POST   | /vocabularies/searches                             |
      | GET    | /vocabularies/AnyVocabulary                        |
      | PATCH  | /vocabularies/AnyVocabulary                        |
      | GET    | /vocabularies/AnyVocabulary/versions               |
      | POST   | /vocabularies/AnyVocabulary/versions               |
      | GET    | /vocabularies/AnyVocabulary/versions/1             |
      | PATCH  | /vocabularies/AnyVocabulary/versions/1             |
      | GET    | /content/AnyVocabulary                             |
      | GET    | /content/AnyVocabulary/versions/1                  |
      | GET    | /external-vocabularies                             |
      | POST   | /external-vocabularies                             |
      | POST   | /external-vocabulary/searches                      |
      | POST   | /external-vocabularies/content                     |
      | GET    | /external-vocabularies/AnyVocabulary/content       |
      | GET    | /external-vocabularies/AnyVocabulary               |
      | DELETE | /external-vocabularies/AnyVocabulary               |

  @AC_SIMPL-18196_AC2 @AC_SIMPL-18173_AC4 @D16 @D17 @D46 @OWASP-02 @SEC-05 @negative
  Scenario Outline: A viewer gets 403 on the write operation <method> <path>
    Given a vocabulary "A" exists
    And an external vocabulary "E" is registered with namespace "${extns:e}"
    And I use the token of "ga-viewer"
    When I send "<method>" to "<path>"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"

    Examples:
      | method | path                                  |
      | POST   | /vocabularies                         |
      | PATCH  | /vocabularies/${A}                    |
      | POST   | /vocabularies/${A}/versions           |
      | PATCH  | /vocabularies/${A}/versions/1         |
      | POST   | /external-vocabularies                |
      | DELETE | /external-vocabularies/${E}           |

  @AC_SIMPL-18196_AC1 @D16 @SEC-05
  Scenario Outline: A viewer may use the read operation <method> <path>
    Given a vocabulary "A" exists
    And an external vocabulary "E" is registered with namespace "${extns:e}"
    And I use the token of "ga-viewer"
    When I send "<method>" to "<path>"
    Then the status is 200

    Examples:
      | method | path                                   |
      | GET    | /vocabularies                          |
      | GET    | /vocabularies/${A}                     |
      | GET    | /vocabularies/${A}/versions            |
      | GET    | /vocabularies/${A}/versions/1          |
      | GET    | /content/${A}                          |
      | GET    | /content/${A}/versions/1               |
      | GET    | /external-vocabularies                 |
      | GET    | /external-vocabularies/${E}            |
      | GET    | /external-vocabularies/${E}/content    |

  @AC_SIMPL-18196_AC1 @D16 @D38
  Scenario: A viewer may use the read-only search operations
    Given a vocabulary "A" exists
    And an external vocabulary "E" is registered with namespace "${extns:e}"
    And I use the token of "ga-viewer"
    When I send "POST" to "/vocabularies/searches" with the JSON body
      """
      {"items": [{"name": "${A}"}]}
      """
    Then the status is 200
    When I send "POST" to "/external-vocabulary/searches" with the JSON body
      """
      {"namespaceUri": "${extns:e}"}
      """
    Then the status is 200
    When I send "POST" to "/external-vocabularies/content" with the JSON body
      """
      {"namespaceUri": "${extns:e}"}
      """
    Then the status is 200

  @AC_SIMPL-18196_AC1 @AC_SIMPL-18196_AC4 @D16
  Scenario: The admin may use every write operation
    Given I use the token of "ga-admin"
    When I create the vocabulary "A"
    Then the status is 201
    When I create a new version of "A"
    Then the status is 201
    When I send "PATCH" to "/vocabularies/${A}/versions/1" with the JSON body
      """
      {"status": "ACTIVE"}
      """
    Then the status is 200
    When I send "PATCH" to "/vocabularies/${A}" with the JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the status is 200
    When I register the external vocabulary "E" with namespace "${extns:e}"
    Then the status is 201
    When I send "DELETE" to "/external-vocabularies/${E}"
    Then the status is 204

  @AC_SIMPL-18196_AC2 @D17 @D51 @negative
  Scenario: A viewer gets 403 before the body is validated
    Given I use the token of "ga-viewer"
    When I send a multipart "POST" to "/vocabularies" with these parts
      | name | value |
      | name | lower-case-and-invalid |
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"

  @AC_SIMPL-18196_AC3 @D17 @D51 @negative
  Scenario: No token gives 401 before the media type and body are checked
    Given I use no token
    When I send "POST" to "/vocabularies" with content type "text/plain" and the body
      """
      not a multipart body
      """
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"

  @AC_SIMPL-18196_AC2 @AC_SIMPL-18173_AC4 @D17 @D51 @negative
  Scenario: A viewer gets 403, not 404, for a new version of an unknown vocabulary
    Given I use the token of "ga-viewer"
    When I create a new version of "Unknown"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"

  @AC_SIMPL-18196_AC2 @D8 @D16 @negative
  Scenario Outline: A refused create by <user> stores nothing
    Given I use the token of "<user>"
    When I create the vocabulary "A"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And the vocabulary list filtered by name "${A}" has 0 items

    Examples:
      | user      |
      | ga-viewer |
      | ga-norole |

  @AC_SIMPL-18173_AC4 @D46 @D16 @D17 @negative
  Scenario Outline: A refused new version by <user> stores nothing and deprecates nothing
    Given a vocabulary "A" exists
    And I use the token of "<user>"
    When I create a new version of "A"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And vocabulary "A" has 1 version
    And version 1 of vocabulary "A" has the status "ACTIVE"

    Examples:
      | user      |
      | ga-viewer |
      | ga-norole |

  @AC_SIMPL-18196_AC2 @D16 @negative
  Scenario: A refused status change by a viewer leaves the status unchanged
    Given a vocabulary "A" exists
    And I use the token of "ga-viewer"
    When I send "PATCH" to "/vocabularies/${A}" with the JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And vocabulary "A" has the status "ACTIVE"

  @AC_SIMPL-18196_AC2 @D16 @D41 @negative
  Scenario: A refused delete by a viewer keeps the external vocabulary
    Given an external vocabulary "E" is registered with namespace "${extns:e}"
    And I use the token of "ga-viewer"
    When I send "DELETE" to "/external-vocabularies/${E}"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And "/external-vocabularies/${E}" answers 200

  @AC_SIMPL-18196_AC4 @D17 @D44 @NFR-07 @MACH-08 @BLG-hlth-res
  Scenario: The health operation needs no token
    Given I use no token
    When I send "GET" to "/health"
    Then the status is 200
    And the media type is "application/json"
    And the response field "status" is "UP"

  @D67 @D84 @negative
  Scenario Outline: A path that matches no operation gives 401 without a token and 404 with one: <path>
    Given I use no token
    When I send "GET" to "<path>"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    Given I use the token of "ga-admin"
    When I send "GET" to "<path>"
    Then the response is a problem with status 404

    Examples:
      | path                    |
      | /no-such-resource       |
      | /actuator/prometheus    |
      | /actuator/health        |
      | /vocabularies/A/b/c/d   |

  @BLG-hdr-case @D17
  Scenario: The Authorization header name is case-insensitive
    Given I use the token of "ga-viewer"
    When I send "GET" to "/vocabularies?pageSize=1" with the "Authorization" header written as "AUTHORIZATION"
    Then the status is 200
