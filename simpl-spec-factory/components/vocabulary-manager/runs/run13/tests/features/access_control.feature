Feature: Access control on every operation
  The service checks the bearer token and the realm roles itself (D17, D40).
  GA_VOCABULARY_VIEWER may read, GA_VOCABULARY_ADMIN may read and write (D16).
  Every operation has a role rule; nothing is allowed by default (D84).
  Token and role checks run before body, query and path checks (D17, D51, D162).

  @AC_SIMPL-18196_AC3 @D17 @E5 @E7 @negative @OWT-A07
  Scenario: A token from another realm cannot write
    Given I use a token of "ga-admin" from the realm "simpl-other"
    When I create the vocabulary "Alpha"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And no vocabulary of this scenario is stored

  @AC_SIMPL-18196_AC3 @AC_SIMPL-18196_AC4 @D17 @D84 @D16 @negative @OWASP-02 @OWASP-08 @API-46 @API-47 @BLG-err-problem @API-36
  Scenario Outline: A request without a token gets 401 on <operation>
    Given I am not authenticated
    When I call the operation "<operation>"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And the problem title is "Unauthorized"
    And the problem type follows the Simpl URN form

    Examples: Read operations
      | operation                          |
      | getVocabularies                    |
      | getVocabulary                      |
      | getVocabularyVersions              |
      | getVocabularyVersion               |
      | downloadVocabulary                 |
      | downloadVocabularyVersion          |
      | searchVocabularies                 |
      | getExternalVocabularies            |
      | searchExternalVocabularies         |
      | downloadExternalVocabularyContent  |
      | getExternalVocabularyContentByName |
      | getExternalVocabulary              |

    Examples: Write operations
      | operation                     |
      | createVocabulary              |
      | updateVocabularyStatus        |
      | updateVocabularyVersionStatus |
      | createExternalVocabulary      |
      | deleteExternalVocabulary      |

    @AC_SIMPL-18173_AC5
    Examples: New version
      | operation               |
      | createVocabularyVersion |

  @AC_SIMPL-18196_AC3 @D17 @E5 @E7 @D40 @negative @OWT-A07 @OWASP-08
  Scenario Outline: A token that is not valid for this service gets 401: <kind>
    Given <token step>
    When I send GET "/vocabularies?name={tok}"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And the problem title is "Unauthorized"

    Examples:
      | kind                    | token step                                                                  |
      | not a JWT               | I send a bearer token that is not a JWT                                     |
      | changed signature       | I use a token of "ga-admin" with a changed signature                        |
      | no signature            | I use a token of "ga-admin" without a signature                             |
      | wrong audience          | I use a token of "ga-admin" from the client without the audience mapper     |
      | issuer of another realm | I use a token of "ga-admin" from the realm "simpl-other"                     |

  @AC_SIMPL-18196_AC2 @D16 @D17 @D84 @negative @OWASP-02 @API-46 @API-47
  Scenario Outline: A user with neither role gets 403 on <operation>
    Given I am authenticated as "ga-norole"
    When I call the operation "<operation>"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And the problem title is "Forbidden"
    And the problem type follows the Simpl URN form

    Examples: Read operations
      | operation                          |
      | getVocabulary                      |
      | getVocabularyVersions              |
      | getVocabularyVersion               |
      | downloadVocabulary                 |
      | downloadVocabularyVersion          |
      | searchVocabularies                 |
      | getExternalVocabularies            |
      | searchExternalVocabularies         |
      | downloadExternalVocabularyContent  |
      | getExternalVocabularyContentByName |
      | getExternalVocabulary              |

    @AC_SIMPL-18162_AC3
    Examples: Vocabulary list
      | operation       |
      | getVocabularies |

    Examples: Write operations
      | operation                     |
      | createVocabulary              |
      | updateVocabularyStatus        |
      | updateVocabularyVersionStatus |
      | createExternalVocabulary      |
      | deleteExternalVocabulary      |

    @AC_SIMPL-18173_AC4
    Examples: New version
      | operation               |
      | createVocabularyVersion |

  @AC_SIMPL-18162_AC3 @D16 @D17 @negative @OWASP-02
  Scenario: A user without a role gets no vocabulary metadata
    Given a vocabulary "Alpha" exists
    And I am authenticated as "ga-norole"
    When I send GET "/vocabularies?name={tok}"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And the response JSON has
      | field | value    |
      | items | <absent> |

  @AC_SIMPL-18196_AC1 @AC_SIMPL-18176_AC1 @D16 @OWASP-02
  Scenario Outline: The viewer may use the read operation <operation>
    Given a vocabulary "Voc" exists
    And an external vocabulary "Ext" is registered with the namespace "https://ext-{tok:lower}.example.org/ns"
    And I am authenticated as "ga-viewer"
    When I call the operation "<operation>"
    Then the response status is <status>

    Examples:
      | operation                          | status |
      | getVocabularies                    | 200    |
      | getVocabulary                      | 200    |
      | getVocabularyVersions              | 200    |
      | getVocabularyVersion               | 200    |
      | downloadVocabulary                 | 200    |
      | downloadVocabularyVersion          | 200    |
      | searchVocabularies                 | 200    |
      | getExternalVocabularies            | 200    |
      | searchExternalVocabularies         | 200    |
      | downloadExternalVocabularyContent  | 200    |
      | getExternalVocabularyContentByName | 200    |
      | getExternalVocabulary              | 200    |

  @AC_SIMPL-18196_AC2 @D16 @D17 @negative @OWASP-02
  Scenario Outline: The viewer gets 403 on the write operation <operation>
    Given a vocabulary "Voc" exists
    And an external vocabulary "Ext" is registered with the namespace "https://ext-{tok:lower}.example.org/ns"
    And I remember the state of the vocabulary "Voc"
    And I am authenticated as "ga-viewer"
    When I call the operation "<operation>"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And the vocabulary "Voc" is unchanged
    And the external vocabulary "Ext" exists
    And no vocabulary named "{Fresh}" is stored
    And the external vocabulary "FreshExt" does not exist

    Examples:
      | operation                     |
      | createVocabulary              |
      | updateVocabularyStatus        |
      | updateVocabularyVersionStatus |
      | createExternalVocabulary      |
      | deleteExternalVocabulary      |

    @AC_SIMPL-18173_AC4 @D46
    Examples: New version (abuse case 2 of D46)
      | operation               |
      | createVocabularyVersion |

  @AC_SIMPL-18196_AC1 @D16 @OWASP-02
  Scenario Outline: The admin may use every operation: <operation>
    Given a vocabulary "Voc" exists
    And an external vocabulary "Ext" is registered with the namespace "https://ext-{tok:lower}.example.org/ns"
    And I am authenticated as "ga-admin"
    When I call the operation "<operation>"
    Then the response status is <status>

    Examples:
      | operation                          | status |
      | getVocabularies                    | 200    |
      | getVocabulary                      | 200    |
      | getVocabularyVersions              | 200    |
      | getVocabularyVersion               | 200    |
      | downloadVocabulary                 | 200    |
      | downloadVocabularyVersion          | 200    |
      | searchVocabularies                 | 200    |
      | getExternalVocabularies            | 200    |
      | searchExternalVocabularies         | 200    |
      | downloadExternalVocabularyContent  | 200    |
      | getExternalVocabularyContentByName | 200    |
      | getExternalVocabulary              | 200    |
      | createVocabulary                   | 201    |
      | updateVocabularyStatus             | 200    |
      | createVocabularyVersion            | 201    |
      | updateVocabularyVersionStatus      | 200    |
      | createExternalVocabulary           | 201    |
      | deleteExternalVocabulary           | 204    |

  @D17 @D44 @D108 @MACH-08 @NFR-07 @BLG-hlth-res
  Scenario: The health operation needs no token
    Given I am not authenticated
    When I send GET "/health"
    Then the response status is 200
    And the response media type is "application/json"
    And the response JSON has
      | field  | value |
      | status | UP    |

  @D17 @D51 @negative
  Scenario: Authentication runs before the media type and body checks
    Given I am not authenticated
    When I send POST "/vocabularies" with the JSON body
      """
      {"name": "x"}
      """
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"

  @D17 @D51 @negative
  Scenario: The role check runs before the body checks
    Given I am authenticated as "ga-viewer"
    When I create a vocabulary with the parts
      | part           | value         |
      | name           | lowercase     |
      | vocabularyFile | empty-file    |
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"

  @D17 @D51 @negative
  Scenario: The role check runs before the media type check
    Given I am authenticated as "ga-viewer"
    When I send POST "/vocabularies" with the body "name=x" of type "text/plain"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"

  @D162 @D17 @negative
  Scenario: On a read the role check runs before the query check
    Given I am authenticated as "ga-norole"
    When I send GET "/vocabularies?pageSize=0&unknownParameter=1"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"

  @D162 @D17 @negative
  Scenario: On a read the role check runs before the not-found check
    Given I am authenticated as "ga-norole"
    When I send GET "/vocabularies/{Ghost}"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"

  @D162 @negative
  Scenario: On a read an invalid query is reported before the not-found check
    Given I am authenticated as "ga-viewer"
    When I send GET "/vocabularies/{Ghost}/versions?pageSize=0"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "pageSize"
