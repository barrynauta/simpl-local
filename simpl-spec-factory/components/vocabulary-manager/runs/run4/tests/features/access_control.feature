Feature: Role-based access control on the Vocabulary Management API
  The service checks the bearer token and the realm roles itself (D17).
  GA_VOCABULARY_VIEWER may read, GA_VOCABULARY_ADMIN may read and write (D16).
  Users: ga-admin, ga-viewer and ga-norole (E1).

  Background:
    Given a vocabulary "A" exists
    And an external vocabulary "X" is registered

  @AC_SIMPL-18196_AC3 @AC_SIMPL-18196_AC4 @D17 @D16 @D40 @negative @OWASP-02 @OWASP-08 @ARCH-32 @API-46 @API-47 @BLG-err-problem @API-36 @E1 @D39
  Scenario Outline: A request without a token is refused with 401 on <operation>
    Given I am not authenticated
    When I call the operation "<operation>"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And the problem title is "Unauthorized"

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

  @AC_SIMPL-18196_AC3 @D17 @D44 @negative @OWASP-08 @API-46 @E1
  Scenario Outline: A token that is <kind> is refused with 401
    Given I use a token that is "<kind>"
    When I send GET "/vocabularies"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And the problem title is "Unauthorized"

    Examples:
      | kind        |
      | garbage     |
      | tampered    |
      | unsigned    |
      | unknown key |
      | expired     |

  @AC_SIMPL-18196_AC2 @AC_SIMPL-18162_AC3 @D16 @D17 @negative @OWASP-02 @SEC-05 @API-46 @API-47 @BLG-err-problem @E1
  Scenario Outline: A user with no vocabulary role gets 403 on <operation>
    Given I am authenticated as "ga-norole"
    When I call the operation "<operation>"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And the problem title is "Forbidden"
    And the response has no "items" field

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

  @AC_SIMPL-18196_AC2 @AC_SIMPL-18173_AC4 @D16 @D17 @D46 @D8 @negative @OWASP-02 @SEC-05 @API-46 @E1
  Scenario Outline: A viewer gets 403 on the write operation <operation> and nothing changes
    Given I am authenticated as "ga-viewer"
    When I call the operation "<operation>"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And the problem title is "Forbidden"
    And nothing was changed by the refused call

    Examples:
      | operation                     |
      | createVocabulary              |
      | updateVocabularyStatus        |
      | createVocabularyVersion       |
      | updateVocabularyVersionStatus |
      | createExternalVocabulary      |
      | deleteExternalVocabulary      |

  @AC_SIMPL-18196_AC2 @D16 @D17 @negative @OWASP-02 @E1
  Scenario: A user with no vocabulary role cannot change anything
    Given I am authenticated as "ga-norole"
    When I call the operation "deleteExternalVocabulary"
    Then the response status is 403
    And nothing was changed by the refused call

  @AC_SIMPL-18196_AC1 @D16 @SEC-05 @E1
  Scenario Outline: A viewer may use the read operation <operation>
    Given I am authenticated as "ga-viewer"
    When I call the operation "<operation>"
    Then the response status is 200
    And the request is authorised and processed

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

  @AC_SIMPL-18196_AC1 @D16 @SEC-05 @API-12 @E1
  Scenario Outline: An administrator may use every operation (<operation>)
    Given I am authenticated as "ga-admin"
    When I call the operation "<operation>"
    Then the response status is <status>
    And the request is authorised and processed

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

  @D17 @negative @OWASP-02 @E1
  Scenario: Authentication is checked before body validation
    Given I am not authenticated
    When I send POST "/vocabularies" with the content type "application/json" and the body:
      """
      {"name": "not valid"}
      """
    Then the response status is 401

  @D17 @negative @OWASP-02 @E1
  Scenario: The role check runs before body validation
    Given I am authenticated as "ga-viewer"
    When I send PATCH "/vocabularies/${name:A}" with the JSON body:
      """
      {"status": "ARCHIVED"}
      """
    Then the response status is 403
    And the vocabulary "A" has status "ACTIVE"

  @D17 @D46 @negative @E1
  Scenario: The role check runs before the not-found check on version creation
    Given I am authenticated as "ga-viewer"
    When I create a new version of "Missing"
    Then the response status is 403

  @D17 @D44 @MACH-08 @BLG-hlth-res
  Scenario: The health operation needs no token
    Given I am not authenticated
    When I call the operation "getHealth"
    Then the response status is 200
    And the response content type is "application/json"
    And the field "status" is "UP"
