Feature: Create a new version of a vocabulary
  POST /vocabularies/{vocabularyName}/versions assigns the next number (D7), stores the new
  version as ACTIVE and deprecates every older ACTIVE version in the same transaction (D4).
  Identical resubmissions are refused (D46). Order of checks D51.

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18173_AC1 @AC_SIMPL-18190_AC1 @AC_SIMPL-18191_AC1 @AC_SIMPL-18187_AC2 @AC_SIMPL-18189_AC1 @D4 @D7 @D26 @D27 @D6
  Scenario: A new version gets the next number, is ACTIVE and deprecates the previous version
    Given a vocabulary "Alpha" exists
    When I create a new version of "Alpha"
    Then the response is the created version 2 of "Alpha"
    And the response has no validation warnings
    And version 1 of "Alpha" has the version status "DEPRECATED"
    And version 2 of "Alpha" has the version status "ACTIVE"
    And the vocabulary "Alpha" has the latest version 2
    And the vocabulary "Alpha" has 2 versions

  @AC_SIMPL-18173_AC1 @AC_SIMPL-18191_AC1 @D4 @D7
  Scenario: All previous versions are DEPRECATED after the third version
    Given a vocabulary "Alpha" exists with 2 versions
    When I create a new version of "Alpha"
    Then the response is the created version 3 of "Alpha"
    And version 1 of "Alpha" has the version status "DEPRECATED"
    And version 2 of "Alpha" has the version status "DEPRECATED"
    And version 3 of "Alpha" has the version status "ACTIVE"

  @D4 @D58
  Scenario: A version deprecated by a new version gets the creation time as its last update date
    Given a vocabulary "Alpha" exists
    And I wait one second
    When I create a new version of "Alpha"
    And I remember the response field "creationDate" as "created"
    And I send GET "/vocabularies/{Alpha}/versions/1"
    Then the response JSON has
      | field          | value       |
      | status         | DEPRECATED  |
      | lastUpdateDate | {$created}  |

  @AC_SIMPL-18173_AC1 @D25 @D28 @D27
  Scenario: The vocabulary shows the latest version, its description and the creator of version 1
    Given a vocabulary "Alpha" exists
    And I remember the field "creationDate" of the vocabulary "Alpha" as "firstCreated"
    And I wait one second
    When I create a new version of "Alpha" with the parts
      | part           | value                          |
      | description    | Second description of {Alpha}. |
      | changelog      | Second version.                |
      | vocabularyFile | file:cpsv-like.ttl             |
    And I remember the response field "creationDate" as "secondCreated"
    And I send GET "/vocabularies/{Alpha}"
    Then the response JSON has
      | field          | value                          |
      | version        | 2                              |
      | description    | Second description of {Alpha}. |
      | status         | ACTIVE                         |
      | createdBy      | {admin_email}                  |
      | creationDate   | {$firstCreated}                |
      | lastUpdateDate | {$secondCreated}               |

  @D5
  Scenario: latestVersionHref always points at the latest version
    Given a vocabulary "Alpha" exists with 2 versions
    When I send GET "/vocabularies/{Alpha}/versions/1"
    Then the response JSON has
      | field             | value                            |
      | latestVersionHref | /vocabularies/{Alpha}/versions/2 |
    When I send GET "/vocabularies/{Alpha}/versions/2"
    Then the response JSON has
      | field             | value                            |
      | latestVersionHref | /vocabularies/{Alpha}/versions/2 |

  @D3
  Scenario: A new version of a DEPRECATED vocabulary is ACTIVE and the vocabulary stays DEPRECATED
    Given a vocabulary "Alpha" exists
    And the vocabulary status of "Alpha" is "DEPRECATED"
    When I create a new version of "Alpha"
    Then the response is the created version 2 of "Alpha"
    And the vocabulary "Alpha" has the vocabulary status "DEPRECATED"
    And version 1 of "Alpha" has the version status "DEPRECATED"

  @D3
  Scenario: Creating a version does not change an ACTIVE vocabulary status
    Given a vocabulary "Alpha" exists
    When I create a new version of "Alpha"
    Then the response status is 201
    And the vocabulary "Alpha" has the vocabulary status "ACTIVE"

  @AC_SIMPL-18190_AC2 @D7 @D8 @negative @OWA-API3
  Scenario: A manual version number is rejected and no version is created
    Given a vocabulary "Alpha" exists
    When I create a new version of "Alpha" with the parts
      | part           | value                   |
      | version        | 2                       |
      | description    | Manual number attempt.  |
      | changelog      | Should not be stored.   |
      | vocabularyFile | file:cpsv-like.ttl      |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "version"
    And the vocabulary "Alpha" has 1 version
    And version 1 of "Alpha" has the version status "ACTIVE"

  @AC_SIMPL-18173_AC3 @D24 @D66 @negative
  Scenario: A version of a vocabulary that does not exist is rejected with 404
    When I create a new version of "Ghost"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '{Ghost}' was not found."
    And the problem has exactly one issue
    And the problem has an issue in "path" named "vocabularyName"
    And no vocabulary named "{Ghost}" is stored

  @AC_SIMPL-18173_AC2 @D37 @D8 @negative
  Scenario Outline: A new version without <part> is rejected and nothing changes
    Given a vocabulary "Alpha" exists
    And I remember the state of the vocabulary "Alpha"
    When I create a new version of "Alpha" with the parts
      | part     | value     |
      | <first>  | <value 1> |
      | <second> | <value 2> |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<part>"
    And the vocabulary "Alpha" is unchanged

    Examples:
      | part           | first       | value 1          | second         | value 2            |
      | changelog      | description | New description. | vocabularyFile | file:cpsv-like.ttl |
      | description    | changelog   | New changelog.   | vocabularyFile | file:cpsv-like.ttl |
      | vocabularyFile | description | New description. | changelog      | New changelog.     |

  @AC_SIMPL-18173_AC2 @D37 @Q2 @NFR-04 @negative
  Scenario Outline: A new version with a <part> of <case> is rejected
    Given a vocabulary "Alpha" exists
    When I create a new version of "Alpha" with the parts
      | part           | value              |
      | description    | <description>      |
      | changelog      | <changelog>        |
      | vocabularyFile | file:cpsv-like.ttl |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<part>"
    And the vocabulary "Alpha" has 1 version

    Examples:
      | part        | case            | description         | changelog           |
      | changelog   | 1001 characters | New description.    | text-of-length:1001 |
      | changelog   | only blanks     | New description.    | blank:4             |
      | description | 1001 characters | text-of-length:1001 | New changelog.      |

  @D37 @Q2 @NFR-04
  Scenario: A new version accepts a changelog of 1000 characters
    Given a vocabulary "Alpha" exists
    When I create a new version of "Alpha" with the parts
      | part           | value               |
      | description    | New description.    |
      | changelog      | text-of-length:1000 |
      | vocabularyFile | file:cpsv-like.ttl  |
    Then the response is the created version 2 of "Alpha"

  @AC_SIMPL-18173_AC2 @AC_SIMPL-18181_AC2 @D8 @D9 @negative @OWT-A10
  Scenario: A new version with invalid content is rejected and no previous version is deprecated
    Given a vocabulary "Alpha" exists
    And I remember the state of the vocabulary "Alpha"
    When I create a new version of "Alpha" with the parts
      | part           | value                       |
      | description    | New description.            |
      | changelog      | New changelog.              |
      | vocabularyFile | file:syntax-error-line3.ttl |
    Then the response is a validation report
    And the vocabulary "Alpha" is unchanged

  @AC_SIMPL-18173_AC4 @D16 @D17 @D46 @negative @OWASP-02
  Scenario: A user without the admin role cannot create a version and nothing changes
    Given a vocabulary "Alpha" exists
    And I remember the state of the vocabulary "Alpha"
    And I am authenticated as "ga-viewer"
    When I create a new version of "Alpha"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And the vocabulary "Alpha" is unchanged

  @AC_SIMPL-18173_AC5 @D17 @negative @OWASP-08
  Scenario: A request without a token cannot create a version and nothing changes
    Given a vocabulary "Alpha" exists
    And I remember the state of the vocabulary "Alpha"
    And I am not authenticated
    When I create a new version of "Alpha"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And the vocabulary "Alpha" is unchanged

  @AC_SIMPL-18173_AC6 @D15 @negative @API-38
  Scenario: A new version sent as JSON gets 415
    Given a vocabulary "Alpha" exists
    When I send POST "/vocabularies/{Alpha}/versions" with the JSON body
      """
      {"description": "New description.", "changelog": "New changelog."}
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And the vocabulary "Alpha" has 1 version

  @D15 @D71 @Q1 @Q2 @NFR-04
  Scenario: A new version with a file of exactly 5 MB is accepted within 30 seconds
    Given a vocabulary "Alpha" exists
    When I create a new version of "Alpha" with the parts
      | part           | value                  |
      | description    | Large version.         |
      | changelog      | At the size limit.     |
      | vocabularyFile | turtle-of-size:5242880 |
    Then the response is the created version 2 of "Alpha"
    And the response arrived within 30 seconds

  @D15 @Q2 @NFR-04 @negative
  Scenario: A new version with a file above 5 MB gets 413 and nothing changes
    Given a vocabulary "Alpha" exists
    And I remember the state of the vocabulary "Alpha"
    When I create a new version of "Alpha" with the parts
      | part           | value                  |
      | description    | Too large.             |
      | changelog      | Above the size limit.  |
      | vocabularyFile | turtle-of-size:5242881 |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And the vocabulary "Alpha" is unchanged

  @D46 @D8 @negative @API-46
  Scenario: Resubmitting the latest version unchanged is refused and deprecates nothing
    Given a vocabulary "Alpha" exists
    And I remember the state of the vocabulary "Alpha"
    When I create a new version of "Alpha" with the same content and description as version 1
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Version 1 of vocabulary '{Alpha}' already has this content and description."
    And the vocabulary "Alpha" is unchanged

  @D46 @negative
  Scenario: The identical-version check trims the description
    Given a vocabulary "Alpha" exists with the description "Stable description."
    When I create a new version of "Alpha" with the content of version 1 and the description "   Stable description.   "
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the vocabulary "Alpha" has 1 version

  @D46 @D33
  Scenario: A description-only change creates a new version
    Given a vocabulary "Alpha" exists with the description "Stable description."
    When I create a new version of "Alpha" with the content of version 1 and the description "Changed description."
    Then the response is the created version 2 of "Alpha"
    And version 1 of "Alpha" has the version status "DEPRECATED"

  @D46
  Scenario: A content-only change creates a new version
    Given a vocabulary "Alpha" exists with the description "Stable description."
    When I create a new version of "Alpha" with the parts
      | part           | value               |
      | description    | Stable description. |
      | changelog      | Only the content.   |
      | vocabularyFile | file:cpsv-like.ttl  |
    Then the response is the created version 2 of "Alpha"

  @D46
  Scenario: The identical-version check compares with the latest version only
    Given a vocabulary "Alpha" exists with the description "Stable description."
    And a new version of "Alpha" exists
    When I create a new version of "Alpha" with the content of version 1 and the description "Stable description."
    Then the response is the created version 3 of "Alpha"

  @D46 @D51 @negative
  Scenario: The field checks run before the identical-version check
    Given a vocabulary "Alpha" exists
    When I create a new version of "Alpha" with the parts
      | part           | value                   |
      | description    | Description of {Alpha}. |
      | vocabularyFile | default-turtle          |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "changelog"

  @D51 @negative
  Scenario: A malformed request to an unknown vocabulary gets 400, not 404
    When I create a new version of "Ghost" with the parts
      | part           | value              |
      | description    | New description.   |
      | vocabularyFile | file:cpsv-like.ttl |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "changelog"

  @D51 @negative
  Scenario: The not-found check runs before content validation
    When I create a new version of "Ghost" with the parts
      | part           | value                       |
      | description    | New description.            |
      | changelog      | New changelog.              |
      | vocabularyFile | file:syntax-error-line3.ttl |
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"

  @D46
  Scenario: An admin who creates many versions is not stopped (abuse case 3 of D46)
    Given a vocabulary "Alpha" exists with 4 versions
    When I create a new version of "Alpha"
    Then the response is the created version 5 of "Alpha"
    When I send GET "/vocabularies/{Alpha}/versions/5"
    Then the response JSON has
      | field     | value         |
      | createdBy | {admin_email} |
