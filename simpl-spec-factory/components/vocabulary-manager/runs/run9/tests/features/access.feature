Feature: Access control on the Vocabulary Management API
  Tokens come from Keycloak (E1). The service checks the token and the realm roles itself (D17).
  GA_VOCABULARY_VIEWER may read, GA_VOCABULARY_ADMIN may also write (D16).

  @AC_SIMPL-18196_AC3 @AC_SIMPL-18196_AC4 @D17 @D16 @D84 @negative @OWASP-02 @ARCH-32 @API-46 @API-47 @BLG-err-problem
  Scenario Outline: A request without a token is refused with 401 on <operation>
    Given the data for the operation matrix exists
    And I am "anonymous"
    When I call the operation "<operation>"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And the problem title is "Unauthorized"
    And the problem type follows API-46 and API-47

    Examples:
      | operation                          |
      | getVocabularies                    |
      | createVocabulary                   |
      | searchVocabularies                 |
      | getVocabulary                      |
      | updateVocabularyStatus             |
      | getVocabularyVersions              |
      | createVocabularyVersion            |
      | getVocabularyVersion               |
      | updateVocabularyVersionStatus      |
      | downloadVocabulary                 |
      | downloadVocabularyVersion          |
      | getExternalVocabularies            |
      | createExternalVocabulary           |
      | searchExternalVocabularies         |
      | downloadExternalVocabularyContent  |
      | getExternalVocabularyContentByName |
      | getExternalVocabulary              |
      | deleteExternalVocabulary           |

  @AC_SIMPL-18196_AC2 @AC_SIMPL-18162_AC3 @D16 @D17 @negative @OWASP-02 @SEC-05 @API-46 @API-47
  Scenario Outline: A user with neither role is refused with 403 on <operation>
    Given the data for the operation matrix exists
    And I am "ga-norole"
    When I call the operation "<operation>"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And the problem title is "Forbidden"
    And the problem type follows API-46 and API-47

    Examples:
      | operation                          |
      | getVocabularies                    |
      | createVocabulary                   |
      | searchVocabularies                 |
      | getVocabulary                      |
      | updateVocabularyStatus             |
      | getVocabularyVersions              |
      | createVocabularyVersion            |
      | getVocabularyVersion               |
      | updateVocabularyVersionStatus      |
      | downloadVocabulary                 |
      | downloadVocabularyVersion          |
      | getExternalVocabularies            |
      | createExternalVocabulary           |
      | searchExternalVocabularies         |
      | downloadExternalVocabularyContent  |
      | getExternalVocabularyContentByName |
      | getExternalVocabulary              |
      | deleteExternalVocabulary           |

  @AC_SIMPL-18162_AC3 @D16 @negative
  Scenario: A user without a role gets no vocabulary metadata
    Given a vocabulary "Hidden" exists
    And I am "ga-norole"
    When I send a "GET" request to "/vocabularies?name=${run}"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And the problem has a detail
    And the response field "items" is absent or null

  @AC_SIMPL-18196_AC1 @D16 @E1 @SEC-05
  Scenario Outline: A viewer may use the read operation <operation>
    Given the data for the operation matrix exists
    And I am "ga-viewer"
    When I call the operation "<operation>"
    Then the response status is 200

    Examples:
      | operation                          |
      | getVocabularies                    |
      | searchVocabularies                 |
      | getVocabulary                      |
      | getVocabularyVersions              |
      | getVocabularyVersion               |
      | downloadVocabulary                 |
      | downloadVocabularyVersion          |
      | getExternalVocabularies            |
      | searchExternalVocabularies         |
      | downloadExternalVocabularyContent  |
      | getExternalVocabularyContentByName |
      | getExternalVocabulary              |

  @AC_SIMPL-18196_AC2 @AC_SIMPL-18173_AC4 @D16 @D17 @D46 @negative @OWASP-02 @SEC-05
  Scenario Outline: A viewer is refused with 403 on the write operation <operation> and nothing changes
    Given the data for the operation matrix exists
    And I am "ga-viewer"
    When I call the operation "<operation>"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And the problem title is "Forbidden"
    And the operation "<operation>" changed nothing

    Examples:
      | operation                     |
      | createVocabulary              |
      | updateVocabularyStatus        |
      | createVocabularyVersion       |
      | updateVocabularyVersionStatus |
      | createExternalVocabulary      |
      | deleteExternalVocabulary      |

  @AC_SIMPL-18173_AC4 @D46 @D16 @negative
  Scenario: A user without a role who tries to create a version gets 403 and no version is created
    Given a vocabulary "Guarded" exists
    And I am "ga-norole"
    When I create a new version of "Guarded"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And the vocabulary "Guarded" has 1 versions

  @AC_SIMPL-18173_AC5 @D17 @negative
  Scenario: Creating a version without a token is refused and no version is created
    Given a vocabulary "Guarded" exists
    And I am "anonymous"
    When I create a new version of "Guarded"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And the vocabulary "Guarded" has 1 versions

  @AC_SIMPL-18196_AC1 @D16 @E1
  Scenario Outline: An admin may use the operation <operation>
    Given the data for the operation matrix exists
    And I am "ga-admin"
    When I call the operation "<operation>"
    Then the response status is <status>

    Examples:
      | operation                          | status |
      | getVocabularies                    | 200    |
      | createVocabulary                   | 201    |
      | searchVocabularies                 | 200    |
      | getVocabulary                      | 200    |
      | updateVocabularyStatus             | 200    |
      | getVocabularyVersions              | 200    |
      | createVocabularyVersion            | 201    |
      | getVocabularyVersion               | 200    |
      | updateVocabularyVersionStatus      | 200    |
      | downloadVocabulary                 | 200    |
      | downloadVocabularyVersion          | 200    |
      | getExternalVocabularies            | 200    |
      | createExternalVocabulary           | 201    |
      | searchExternalVocabularies         | 200    |
      | downloadExternalVocabularyContent  | 200    |
      | getExternalVocabularyContentByName | 200    |
      | getExternalVocabulary              | 200    |
      | deleteExternalVocabulary           | 204    |

  @AC_SIMPL-18196_AC3 @D17 @E5 @E7 @negative @OWASP-08 @OWT-A07
  Scenario Outline: A token that is not valid for this service is refused with 401 (<kind>)
    Given a vocabulary "Target" exists
    And I use a token of kind "<kind>"
    When I send a "GET" request to "/vocabularies/${Target}"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And the problem title is "Unauthorized"

    Examples:
      | kind               |
      | no-audience        |
      | other-issuer       |
      | tampered-signature |
      | tampered-payload   |
      | malformed          |

  @AC_SIMPL-18196_AC3 @D17 @E5 @E7 @negative @OWASP-08 @OWT-A07
  Scenario Outline: A write with a token that is not valid for this service is refused and stores nothing (<kind>)
    Given I use a token of kind "<kind>"
    When I create the vocabulary "Sneaky"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And no vocabulary named "Sneaky" is stored

    Examples:
      | kind               |
      | no-audience        |
      | other-issuer       |
      | tampered-signature |

  @D17 @D51 @negative
  Scenario: Authentication runs before body validation
    Given I am "anonymous"
    When I send a multipart POST to "/vocabularies" with
      | part        | value |
      | name        | bad-name |
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"

  @D17 @D51 @negative
  Scenario: The role check runs before body validation
    Given I am "ga-viewer"
    When I send a multipart POST to "/vocabularies" with
      | part        | value |
      | name        | bad-name |
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"

  @D51 @D16 @negative
  Scenario: The role check runs before the not-found check on a write
    Given I am "ga-viewer"
    When I send a "PATCH" request to "/vocabularies/${Ghost}" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"

  @D16
  Scenario: A viewer token that asked only for the openid scope reads the list
    Given I am "ga-viewer"
    When I send a "GET" request to "/vocabularies?name=${run}"
    Then the response status is 200

  @D17 @D44 @MACH-08 @NFR-07 @BLG-hlth-res
  Scenario: GET /health needs no token and reports UP
    Given I am "anonymous"
    When I send a "GET" request to "/health"
    Then the response status is 200
    And the response media type is "application/json"
    And the response field "status" is "UP"

  @D67 @D84 @negative @OWT-A02
  Scenario: An unknown path gives 401 without a token
    Given I am "anonymous"
    When I send a "GET" request to "/no-such-resource-${run}"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"

  @D67 @D84 @D55 @negative @OWT-A02
  Scenario: An unknown path gives 404 problem+json with a token
    When I send a "GET" request to "/no-such-resource-${run}"
    Then the response is a problem with status 404

  @D67 @negative @OWT-A02
  Scenario Outline: Actuator paths do not exist on the API port (<user>)
    Given I am "<user>"
    When I send a "GET" request to "<path>"
    Then the response status is <status>
    And the response media type is "application/problem+json"

    Examples:
      | user      | path                 | status |
      | anonymous | /actuator/health     | 401    |
      | anonymous | /actuator/prometheus | 401    |
      | ga-admin  | /actuator/health     | 404    |
      | ga-admin  | /actuator/prometheus | 404    |
      | ga-admin  | /actuator            | 404    |

  @D67 @D55 @negative
  Scenario Outline: A method the server does not know returns 405 before the token check (<user>)
    Given I am "<user>"
    When I send a "QUERY" request to "/vocabularies"
    Then the response is a problem with status 405

    Examples:
      | user      |
      | anonymous |
      | ga-admin  |

  @D67 @negative
  Scenario Outline: A path the firewall refuses gives 400 before the token check (<user>, <path>)
    Given I am "<user>"
    When I send a GET request for the raw path "<path>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has no issues

    Examples:
      | user      | path                    |
      | anonymous | /vocabularies/Abc%2FDef |
      | ga-admin  | /vocabularies/Abc%2FDef |
      | anonymous | /vocabularies/Abc%0ADef |
      | ga-admin  | /vocabularies/Abc%00Def |

  @D77 @D55 @negative
  Scenario Outline: A method that a fixed path does not offer returns 405 (<path>)
    When I send a "GET" request to "<path>"
    Then the response is a problem with status 405

    Examples:
      | path                           |
      | /vocabularies/searches         |
      | /external-vocabularies/content |
