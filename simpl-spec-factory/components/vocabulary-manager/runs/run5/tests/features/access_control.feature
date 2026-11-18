Feature: Role-based access control for the vocabulary management API
  The service validates the bearer token and the realm roles itself (D17).
  GA_VOCABULARY_VIEWER may read; GA_VOCABULARY_ADMIN may read and write (D16).
  The scopes read and write are not checked; tests request no scope beyond openid (D40).

  Background:
    Given a vocabulary "{Subject}" exists
    And an external vocabulary "{ExtSubject}" is registered with namespace "https://ext-{run}.example.org/subject#"

  @AC_SIMPL-18196_AC3 @D17 @D8 @negative @OWASP-02
  Scenario: A create request without a token stores nothing
    Given I am not authenticated
    When I call the operation "createVocabulary"
    Then the response is a problem with status 401
    And no vocabulary named "{New}" is stored

  @AC_SIMPL-18196_AC1 @D16 @D40 @E1 @SEC-05 @OWASP-02 @API-12
  Scenario Outline: A Governance Authority user with GA_VOCABULARY_VIEWER can call every read operation (<operation>)
    Given I am a Governance Authority user with role "GA_VOCABULARY_VIEWER"
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

  @AC_SIMPL-18196_AC1 @D16 @E1 @SEC-05 @OWASP-02 @API-12
  Scenario Outline: A Governance Authority user with GA_VOCABULARY_ADMIN can call every operation (<operation>)
    Given I am a Governance Authority user with role "GA_VOCABULARY_ADMIN"
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

  @AC_SIMPL-18196_AC2 @D16 @D17 @D48 @E1 @negative @SEC-05 @OWASP-02 @API-46 @API-47 @BLG-err-problem
  Scenario Outline: An authenticated user without a vocabulary management role gets 403 (<operation>)
    Given I am an authenticated user with no vocabulary management role
    When I call the operation "<operation>"
    Then the response is a problem with status 403
    And the problem type follows the simpl URN form

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

  @AC_SIMPL-18196_AC2 @D16 @D17 @D48 @E1 @negative @OWASP-02 @API-46
  Scenario Outline: A viewer gets 403 on a write operation and nothing changes (<operation>)
    Given I am a Governance Authority user with role "GA_VOCABULARY_VIEWER"
    When I call the operation "<operation>"
    Then the response is a problem with status 403
    And the vocabulary "{Subject}" has status "ACTIVE"
    And the vocabulary "{Subject}" has 1 version
    And version 1 of "{Subject}" has status "ACTIVE"
    And no vocabulary named "{New}" is stored
    And the external vocabulary "{ExtSubject}" is registered
    And no external vocabulary named "{ExtNew}" is registered

    Examples:
      | operation                     |
      | createVocabulary              |
      | updateVocabularyStatus        |
      | createVocabularyVersion       |
      | updateVocabularyVersionStatus |
      | createExternalVocabulary      |
      | deleteExternalVocabulary      |

  @AC_SIMPL-18196_AC3 @AC_SIMPL-18196_AC4 @D17 @D48 @negative @OWASP-02 @OWASP-08 @ARCH-32 @API-46 @API-47
  Scenario Outline: A request without a token gets 401 (<operation>)
    Given I am not authenticated
    When I call the operation "<operation>"
    Then the response is a problem with status 401
    And the problem type follows the simpl URN form

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

  @AC_SIMPL-18196_AC3 @AC_SIMPL-18196_AC4 @D17 @negative @OWASP-08 @OWT-A07
  Scenario Outline: An invalid bearer token gets 401 (<case>)
    Given <token step>
    When I call the operation "getVocabularies"
    Then the response is a problem with status 401

    Examples:
      | case                 | token step                                                      |
      | tampered signature   | I send a token with a tampered signature                        |
      | unsigned token       | I send an unsigned token                                        |
      | expired token        | I send an expired token                                         |
      | not a JWT            | I send the authorization header "Bearer not-a-real-token"       |
      | wrong scheme         | I send the authorization header "Basic Z2EtYWRtaW46UGFzc3dvcmQxIQ==" |

  @AC_SIMPL-18196_AC3 @D17 @negative @OWASP-08
  Scenario: A tampered token that claims the admin role cannot write
    Given I send a token with a tampered signature
    When I call the operation "createVocabulary"
    Then the response is a problem with status 401
    And no vocabulary named "{New}" is stored

  @D17 @D51 @negative @OWASP-02
  Scenario: Authentication is checked before the body (no token, invalid body)
    Given I am not authenticated
    When I create a vocabulary with the form
      | field          | value        |
      | name           | lowercase    |
      | vocabularyFile | fixture:syntax-error.ttl |
    Then the response is a problem with status 401

  @AC_SIMPL-18196_AC2 @D17 @D51 @negative @OWASP-02
  Scenario: The role is checked before the body (viewer, invalid body)
    Given I am a Governance Authority user with role "GA_VOCABULARY_VIEWER"
    When I create a vocabulary with the form
      | field          | value                    |
      | name           | lowercase                |
      | vocabularyFile | fixture:syntax-error.ttl |
    Then the response is a problem with status 403

  @AC_SIMPL-18196_AC2 @D17 @D51 @negative @OWASP-02
  Scenario: The role is checked before the media type (viewer, JSON body on a multipart operation)
    Given I am a Governance Authority user with role "GA_VOCABULARY_VIEWER"
    When I send "POST" to "/vocabularies" with JSON
      """
      {"name": "{New}", "description": "x", "changelog": "y"}
      """
    Then the response is a problem with status 403
