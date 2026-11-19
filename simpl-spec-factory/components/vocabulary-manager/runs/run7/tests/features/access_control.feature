Feature: Role-based access control on every operation
  Two realm roles: GA_VOCABULARY_VIEWER reads, GA_VOCABULARY_ADMIN reads and writes (D16).
  The service checks the token and the roles itself (D17, D40, E5).

  Background:
    Given the access check data exists

  @AC_SIMPL-18196_AC3 @D17 @E5 @negative @OWASP-08
  Scenario: A tampered token is rejected on a write and nothing is stored
    When "tampered-token" calls the operation "POST /vocabularies"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And no vocabulary named "AccNew" is stored

  @AC_SIMPL-18196_AC3 @AC_SIMPL-18196_AC4 @AC_SIMPL-18173_AC5 @D17 @D16 @D40 @negative @OWASP-02 @OWASP-08 @ARCH-32 @API-46 @API-47 @BLG-err-problem @BLG-prb-type
  Scenario Outline: A request without a token is rejected with 401 on <operation>
    When "anonymous" calls the operation "<operation>"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And the problem title is "Unauthorized"
    And the problem type is a simpl problem type in lowerCamelCase

    Examples:
      | operation                                               |
      | GET /vocabularies                                       |
      | POST /vocabularies                                      |
      | POST /vocabularies/searches                             |
      | GET /vocabularies/{vocabularyName}                      |
      | PATCH /vocabularies/{vocabularyName}                    |
      | GET /vocabularies/{vocabularyName}/versions             |
      | POST /vocabularies/{vocabularyName}/versions            |
      | GET /vocabularies/{vocabularyName}/versions/{version}   |
      | PATCH /vocabularies/{vocabularyName}/versions/{version} |
      | GET /content/{vocabularyName}                           |
      | GET /content/{vocabularyName}/versions/{version}        |
      | GET /external-vocabularies                              |
      | POST /external-vocabularies                             |
      | POST /external-vocabulary/searches                      |
      | POST /external-vocabularies/content                     |
      | GET /external-vocabularies/{name}/content               |
      | GET /external-vocabularies/{name}                       |
      | DELETE /external-vocabularies/{name}                    |

  @AC_SIMPL-18196_AC3 @D17 @E5 @negative @OWASP-08 @ARCH-32
  Scenario Outline: A token that is not valid is rejected with 401 (<caller>)
    When "<caller>" calls the operation "GET /vocabularies"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And the problem title is "Unauthorized"

    Examples:
      | caller         |
      | invalid-token  |
      | tampered-token |
      | unsigned-token |

  @AC_SIMPL-18196_AC2 @AC_SIMPL-18162_AC3 @D16 @D17 @negative @OWASP-02 @SEC-05 @API-46 @API-47 @BLG-err-problem
  Scenario Outline: A user with neither role gets 403 on <operation>
    When "ga-norole" calls the operation "<operation>"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And the problem title is "Forbidden"
    And the problem type is a simpl problem type in lowerCamelCase
    And the response field "detail" is present
    And the response field "items" is absent

    Examples:
      | operation                                               |
      | GET /vocabularies                                       |
      | POST /vocabularies                                      |
      | POST /vocabularies/searches                             |
      | GET /vocabularies/{vocabularyName}                      |
      | PATCH /vocabularies/{vocabularyName}                    |
      | GET /vocabularies/{vocabularyName}/versions             |
      | POST /vocabularies/{vocabularyName}/versions            |
      | GET /vocabularies/{vocabularyName}/versions/{version}   |
      | PATCH /vocabularies/{vocabularyName}/versions/{version} |
      | GET /content/{vocabularyName}                           |
      | GET /content/{vocabularyName}/versions/{version}        |
      | GET /external-vocabularies                              |
      | POST /external-vocabularies                             |
      | POST /external-vocabulary/searches                      |
      | POST /external-vocabularies/content                     |
      | GET /external-vocabularies/{name}/content               |
      | GET /external-vocabularies/{name}                       |
      | DELETE /external-vocabularies/{name}                    |

  @AC_SIMPL-18196_AC2 @AC_SIMPL-18173_AC4 @D16 @D17 @D46 @negative @OWASP-02 @SEC-05
  Scenario Outline: A viewer gets 403 on the write operation <operation>
    When "ga-viewer" calls the operation "<operation>"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And the problem title is "Forbidden"

    Examples:
      | operation                                               |
      | POST /vocabularies                                      |
      | PATCH /vocabularies/{vocabularyName}                    |
      | POST /vocabularies/{vocabularyName}/versions            |
      | PATCH /vocabularies/{vocabularyName}/versions/{version} |
      | POST /external-vocabularies                             |
      | DELETE /external-vocabularies/{name}                    |

  @AC_SIMPL-18196_AC2 @D16 @D8 @negative @OWASP-02
  Scenario: A viewer's write changes nothing
    When "ga-viewer" calls the operation "POST /vocabularies/{vocabularyName}/versions"
    Then the response status is 403
    And vocabulary "Acc" has 1 version
    And version 1 of "Acc" has status "ACTIVE"
    When "ga-viewer" calls the operation "POST /vocabularies"
    Then the response status is 403
    And no vocabulary named "AccNew" is stored
    When "ga-viewer" calls the operation "PATCH /vocabularies/{vocabularyName}"
    Then the response status is 403
    And vocabulary "Acc" has status "ACTIVE"
    When "ga-viewer" calls the operation "DELETE /external-vocabularies/{name}"
    Then the response status is 403
    And the external vocabulary list contains "Acc"

  @AC_SIMPL-18196_AC1 @AC_SIMPL-18176_AC1 @D16 @OWASP-02 @SEC-05
  Scenario Outline: A viewer may use the read operation <operation>
    When "ga-viewer" calls the operation "<operation>"
    Then the response status is <status>

    Examples:
      | operation                                             | status |
      | GET /vocabularies                                     | 200    |
      | POST /vocabularies/searches                           | 200    |
      | GET /vocabularies/{vocabularyName}                    | 200    |
      | GET /vocabularies/{vocabularyName}/versions           | 200    |
      | GET /vocabularies/{vocabularyName}/versions/{version} | 200    |
      | GET /content/{vocabularyName}                         | 200    |
      | GET /content/{vocabularyName}/versions/{version}      | 200    |
      | GET /external-vocabularies                            | 200    |
      | POST /external-vocabulary/searches                    | 200    |
      | POST /external-vocabularies/content                   | 200    |
      | GET /external-vocabularies/{name}/content             | 200    |
      | GET /external-vocabularies/{name}                     | 200    |

  @AC_SIMPL-18196_AC1 @D16 @OWASP-02 @SEC-05
  Scenario Outline: An admin may use every operation (<operation>)
    When "ga-admin" calls the operation "<operation>"
    Then the response status is <status>

    Examples:
      | operation                                               | status |
      | GET /vocabularies                                       | 200    |
      | POST /vocabularies                                      | 201    |
      | POST /vocabularies/searches                             | 200    |
      | GET /vocabularies/{vocabularyName}                      | 200    |
      | PATCH /vocabularies/{vocabularyName}                    | 200    |
      | GET /vocabularies/{vocabularyName}/versions             | 200    |
      | POST /vocabularies/{vocabularyName}/versions            | 201    |
      | GET /vocabularies/{vocabularyName}/versions/{version}   | 200    |
      | PATCH /vocabularies/{vocabularyName}/versions/{version} | 200    |
      | GET /content/{vocabularyName}                           | 200    |
      | GET /content/{vocabularyName}/versions/{version}        | 200    |
      | GET /external-vocabularies                              | 200    |
      | POST /external-vocabularies                             | 201    |
      | POST /external-vocabulary/searches                      | 200    |
      | POST /external-vocabularies/content                     | 200    |
      | GET /external-vocabularies/{name}/content               | 200    |
      | GET /external-vocabularies/{name}                       | 200    |
      | DELETE /external-vocabularies/{name}                    | 204    |

  @D17 @D51 @negative @OWASP-02
  Scenario: Authentication is checked before the body (no token and an unsupported body)
    When "anonymous" sends POST "/vocabularies" with the body "{}" of type "application/json"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"

  @D17 @D51 @negative @OWASP-02
  Scenario: The role is checked before the body (viewer and an invalid body)
    When "ga-viewer" sends a multipart POST to "/vocabularies" with the fields
      | field          | value          |
      | name           | lowercase      |
      | vocabularyFile | <empty>        |
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"

  @D17 @D51 @negative
  Scenario: The role is checked before a 404 on an unknown vocabulary
    When "ga-viewer" sets the status of vocabulary "Unknown" to "DEPRECATED"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
