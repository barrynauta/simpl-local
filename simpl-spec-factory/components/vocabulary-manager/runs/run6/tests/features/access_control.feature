Feature: Role-based access control on the Vocabulary Management API
  The service validates the bearer token and the realm roles itself (D17).
  GA_VOCABULARY_VIEWER may read, GA_VOCABULARY_ADMIN may also write (D16).
  Authentication and role checks run before any other check (D17, D51).

  Background:
    Given the vocabulary "Acl" exists
    And the external vocabulary "AclExt" is registered

  @AC_SIMPL-18196_AC3 @AC_SIMPL-18196_AC4 @AC_SIMPL-18173_AC5 @D17 @D16 @D48 @D51 @OWASP-02 @OWASP-08 @ARCH-32 @API-46 @BLG-err-problem @negative
  Scenario Outline: A request without a token is refused with 401 on <operation>
    Given I am not authenticated
    When I call the operation "<operation>"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And the problem title is "Unauthorized"
    And the problem type follows the simpl URN form
    And the refused request changed nothing

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

  @AC_SIMPL-18196_AC3 @D17 @OWASP-08 @negative
  Scenario Outline: An invalid token is refused with 401 on <operation>
    Given I use the invalid token "<token>"
    When I call the operation "<operation>"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And the refused request changed nothing

    Examples:
      | operation              | token                 |
      | getVocabularies        | not-a-jwt             |
      | createVocabulary       | abc.def.ghi           |
      | updateVocabularyStatus | eyJhbGciOiJIUzI1NiJ9. |

  @AC_SIMPL-18196_AC3 @D17 @OWASP-08 @negative
  Scenario Outline: A token with a broken signature is refused with 401 on <operation>
    Given I use a token with a tampered signature
    When I call the operation "<operation>"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And the refused request changed nothing

    Examples:
      | operation                |
      | getVocabularies          |
      | createVocabulary         |
      | deleteExternalVocabulary |

  @AC_SIMPL-18196_AC3 @D17 @D44 @OWASP-08 @negative
  Scenario Outline: An unsigned token is never accepted on <operation>
    Given I use an unsigned token
    When I call the operation "<operation>"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And the refused request changed nothing

    Examples:
      | operation              |
      | getVocabularies        |
      | updateVocabularyStatus |

  @AC_SIMPL-18196_AC2 @AC_SIMPL-18196_AC4 @D16 @D17 @D48 @OWASP-02 @SEC-05 @API-46 @BLG-err-problem @negative
  Scenario Outline: A user without a vocabulary management role gets 403 on <operation>
    Given I am authenticated as "ga-norole"
    When I call the operation "<operation>"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And the problem title is "Forbidden"
    And the problem type follows the simpl URN form
    And the refused request changed nothing

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

  @AC_SIMPL-18162_AC3 @D16 @D17 @negative
  Scenario: A user without a vocabulary management role gets no metadata from the list
    Given I am authenticated as "ga-norole"
    When I list the vocabularies with "name={{run}}"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And the problem has a detail
    And the response holds no metadata

  @AC_SIMPL-18196_AC2 @AC_SIMPL-18173_AC4 @D16 @D17 @D46 @OWASP-02 @SEC-05 @negative
  Scenario Outline: A viewer gets 403 on the write operation <operation>
    Given I am authenticated as "ga-viewer"
    When I call the operation "<operation>"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And the refused request changed nothing

    Examples:
      | operation                     |
      | createVocabulary              |
      | updateVocabularyStatus        |
      | createVocabularyVersion       |
      | updateVocabularyVersionStatus |
      | createExternalVocabulary      |
      | deleteExternalVocabulary      |

  @AC_SIMPL-18196_AC1 @AC_SIMPL-18176_AC1 @D16 @SEC-05
  Scenario Outline: A viewer may use the read operation <operation>
    Given I am authenticated as "ga-viewer"
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

  @AC_SIMPL-18196_AC1 @D16 @SEC-05
  Scenario Outline: An admin may use the operation <operation>
    Given I am authenticated as "ga-admin"
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

  @D17 @D51 @negative
  Scenario: Authentication runs before body validation
    Given I am not authenticated
    When I create the vocabulary "Broken" with:
      | field       | value      |
      | description | (omitted)  |
      | name        | lower{{run}} |
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And no vocabulary named "raw:lower{{run}}" is stored

  @D17 @D51 @negative
  Scenario: The role check runs before body validation
    Given I am authenticated as "ga-viewer"
    When I create the vocabulary "Broken" with:
      | field          | value                     |
      | description    | (omitted)                 |
      | vocabularyFile | file:syntax_error_line3.ttl |
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And no vocabulary named "Broken" is stored

  @D17 @D51 @negative
  Scenario: The role check runs before the not-found check on writes
    Given I am authenticated as "ga-viewer"
    When I set the status of "Ghost" to "DEPRECATED"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"

  @D46 @D16 @negative
  Scenario: A user without a role cannot create a version, even an identical one
    Given I am authenticated as "ga-norole"
    When I resubmit the latest version of "Acl" unchanged with a new change log
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And the vocabulary "Acl" has 1 version

  @D17 @D44 @BLG-hlth-res @MACH-08 @NFR-07
  Scenario: The health operation needs no token
    Given I am not authenticated
    When I send "GET" to "/health"
    Then the response status is 200
    And the response media type is "application/json"
    And the health status is "UP"
