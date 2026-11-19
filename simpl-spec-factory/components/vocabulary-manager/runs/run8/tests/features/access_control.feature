Feature: Authentication and role-based access control on every operation
  The service validates the bearer token (Keycloak realm simpl, audience
  vocabulary-manager-be) and the realm roles itself. GA_VOCABULARY_VIEWER may
  read, GA_VOCABULARY_ADMIN may read and write. GET /health needs no token.

  Background:
    Given a vocabulary "Alpha" exists
    And an external vocabulary "Ext" is registered with namespace "http://ext.example.org/${uid}/terms"

  @D44 @D17 @NFR-07 @MACH-08
  Scenario: GET /health needs no token and reports UP
    Given I am not authenticated
    When I send a GET request to "/health"
    Then the response status is 200
    And the response media type is "application/json"
    And the response field "status" is "UP"

  @AC_SIMPL-18196_AC3 @AC_SIMPL-18196_AC4 @AC_SIMPL-18195_AC3 @D17 @D48 @D84 @negative @OWASP-08 @OWT-A07 @API-46 @API-47 @BLG-err-problem
  Scenario Outline: Without a token every secured operation answers 401
    Given I am not authenticated
    When I call the operation "<operation>"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And the problem title is "Unauthorized"

    Examples:
      | operation                         |
      | getVocabularies                   |
      | createVocabulary                  |
      | searchVocabularies                |
      | getVocabulary                     |
      | updateVocabularyStatus            |
      | getVocabularyVersions             |
      | createVocabularyVersion           |
      | getVocabularyVersion              |
      | updateVocabularyVersionStatus     |
      | downloadVocabulary                |
      | downloadVocabularyVersion         |
      | getExternalVocabularies           |
      | createExternalVocabulary          |
      | searchExternalVocabularies        |
      | downloadExternalVocabularyContent |
      | getExternalVocabularyContentByName|
      | getExternalVocabulary             |
      | deleteExternalVocabulary          |

  @AC_SIMPL-18196_AC3 @AC_SIMPL-18196_AC4 @D17 @D40 @E5 @E7 @negative @OWASP-08 @OWT-A07
  Scenario Outline: An invalid token is refused with 401
    Given I use a "<kind>" token of "ga-admin"
    When I call the operation "<operation>"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And the problem title is "Unauthorized"

    Examples:
      | kind        | operation         |
      | tampered    | getVocabularies   |
      | tampered    | createVocabulary  |
      | unsigned    | getVocabularies   |
      | noaud       | getVocabularies   |
      | noaud       | createVocabulary  |
      | otherissuer | getVocabularies   |
      | otherissuer | createVocabulary  |

  @AC_SIMPL-18196_AC3 @D17 @negative @OWASP-08
  Scenario Outline: A bearer value that is not a token is refused with 401
    Given I use the bearer token "<token>"
    When I call the operation "getVocabularies"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And the problem title is "Unauthorized"

    Examples:
      | token                 |
      | not-a-token           |
      | e30.e30.c2ln          |

  @AC_SIMPL-18196_AC2 @D16 @D17 @D48 @negative @OWASP-02 @SEC-05 @API-46
  Scenario Outline: A user with neither role gets 403 on every secured operation
    Given I am authenticated as "ga-norole"
    When I call the operation "<operation>"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And the problem title is "Forbidden"

    Examples:
      | operation                         |
      | getVocabularies                   |
      | createVocabulary                  |
      | searchVocabularies                |
      | getVocabulary                     |
      | updateVocabularyStatus            |
      | getVocabularyVersions             |
      | createVocabularyVersion           |
      | getVocabularyVersion              |
      | updateVocabularyVersionStatus     |
      | downloadVocabulary                |
      | downloadVocabularyVersion         |
      | getExternalVocabularies           |
      | createExternalVocabulary          |
      | searchExternalVocabularies        |
      | downloadExternalVocabularyContent |
      | getExternalVocabularyContentByName|
      | getExternalVocabulary             |
      | deleteExternalVocabulary          |

  @AC_SIMPL-18196_AC2 @D16 @D46 @negative @OWASP-02 @SEC-05
  Scenario Outline: A viewer gets 403 on every write operation and nothing changes
    Given I am authenticated as "ga-viewer"
    When I call the operation "<operation>"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And vocabulary "Alpha" has vocabulary status "ACTIVE"
    And vocabulary "Alpha" has 1 version
    And version 1 of "Alpha" has version status "ACTIVE"
    And 1 vocabulary whose name contains "${uid}" is stored
    And an external vocabulary is registered for namespace "http://ext.example.org/${uid}/terms"

    Examples:
      | operation                     |
      | createVocabulary              |
      | updateVocabularyStatus        |
      | createVocabularyVersion       |
      | updateVocabularyVersionStatus |
      | createExternalVocabulary      |
      | deleteExternalVocabulary      |

  @AC_SIMPL-18196_AC1 @AC_SIMPL-18176_AC1 @D16 @SEC-05
  Scenario Outline: A viewer is authorised for every read operation
    Given I am authenticated as "ga-viewer"
    When I call the operation "<operation>"
    Then the request is authorised and processed

    Examples:
      | operation                         |
      | getVocabularies                   |
      | searchVocabularies                |
      | getVocabulary                     |
      | getVocabularyVersions             |
      | getVocabularyVersion              |
      | downloadVocabulary                |
      | downloadVocabularyVersion         |
      | getExternalVocabularies           |
      | searchExternalVocabularies        |
      | downloadExternalVocabularyContent |
      | getExternalVocabularyContentByName|
      | getExternalVocabulary             |

  @AC_SIMPL-18196_AC1 @AC_SIMPL-18195_AC1 @D16 @SEC-05
  Scenario Outline: An admin is authorised for every operation
    Given I am authenticated as "ga-admin"
    When I call the operation "<operation>"
    Then the request is authorised and processed

    Examples:
      | operation                         |
      | getVocabularies                   |
      | createVocabulary                  |
      | searchVocabularies                |
      | getVocabulary                     |
      | updateVocabularyStatus            |
      | getVocabularyVersions             |
      | createVocabularyVersion           |
      | getVocabularyVersion              |
      | updateVocabularyVersionStatus     |
      | downloadVocabulary                |
      | downloadVocabularyVersion         |
      | getExternalVocabularies           |
      | createExternalVocabulary          |
      | searchExternalVocabularies        |
      | downloadExternalVocabularyContent |
      | getExternalVocabularyContentByName|
      | getExternalVocabulary             |
      | deleteExternalVocabulary          |

  @D17 @D51 @negative
  Scenario: Authentication runs before body validation
    Given I am not authenticated
    When I send a POST request to "/vocabularies" with these form parts
      | part | value       |
      | name | lower${uid} |
    Then the response is a problem with status 401

  @D17 @D51 @negative
  Scenario: The role check runs before body validation
    Given I am authenticated as "ga-viewer"
    When I send a PATCH request to "/vocabularies/${Alpha}" with JSON body
      """
      {"status": "ARCHIVED", "unknown": true}
      """
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"

  @D63 @D51 @negative
  Scenario: The token is checked before the Accept header
    Given I am not authenticated
    When I send a GET request to "/vocabularies" with header "Accept" set to "application/xml"
    Then the response is a problem with status 401

  @D67 @D84 @D55 @negative
  Scenario: An unknown path gives 401 without a token and 404 with one
    Given I am not authenticated
    When I send a GET request to "/unknown-${uid}"
    Then the response is a problem with status 401
    Given I am authenticated as "ga-admin"
    When I send a GET request to "/unknown-${uid}"
    Then the response is a problem with status 404

  @D67 @D84 @negative @OWT-A02
  Scenario Outline: Actuator paths do not exist on the API port
    Given I am not authenticated
    When I send a GET request to "<path>"
    Then the response is a problem with status 401
    Given I am authenticated as "ga-admin"
    When I send a GET request to "<path>"
    Then the response is a problem with status 404

    Examples:
      | path                  |
      | /actuator             |
      | /actuator/health      |
      | /actuator/prometheus  |
      | /actuator/env         |

  @D67 @D55 @negative
  Scenario: A method the server does not know gives 405 before the token check
    Given I am not authenticated
    When I send a QUERY request to "/vocabularies"
    Then the response is a problem with status 405

  @D67 @D77 @negative
  Scenario: A method a fixed path does not offer gives 405
    Given I am authenticated as "ga-admin"
    When I send a GET request to "/external-vocabularies/content"
    Then the response is a problem with status 405

  @D67 @negative
  Scenario: A path the firewall refuses gives 400 before the token check
    Given I am not authenticated
    When I send a GET request to "/vocabularies/Abc%2F${uid}"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has no issues

  @D21 @D52 @negative @BLG-req-valid
  Scenario Outline: An unknown query parameter is rejected on every operation
    Given I am authenticated as "ga-admin"
    When I call the operation "<operation>" with the query parameter "unknownParam=1"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "unknownParam"

    Examples:
      | operation                         |
      | getVocabularies                   |
      | createVocabulary                  |
      | searchVocabularies                |
      | getVocabulary                     |
      | updateVocabularyStatus            |
      | getVocabularyVersions             |
      | createVocabularyVersion           |
      | getVocabularyVersion              |
      | updateVocabularyVersionStatus     |
      | downloadVocabulary                |
      | downloadVocabularyVersion         |
      | getExternalVocabularies           |
      | createExternalVocabulary          |
      | searchExternalVocabularies        |
      | downloadExternalVocabularyContent |
      | getExternalVocabularyContentByName|
      | getExternalVocabulary             |
      | deleteExternalVocabulary          |

  @D86
  Scenario: The service returns X-Correlation-Id and keeps the one it was given
    Given I am authenticated as "ga-admin"
    When I send a GET request to "/vocabularies?name=${uid}"
    Then the response header "X-Correlation-Id" is present
    When I send a GET request to "/vocabularies?name=${uid}" with header "X-Correlation-Id" set to "corr-${uid}"
    Then the response header "X-Correlation-Id" is "corr-${uid}"
