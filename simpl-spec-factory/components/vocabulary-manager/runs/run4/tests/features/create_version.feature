Feature: Create a new version of a vocabulary through the API
  POST /vocabularies/{vocabularyName}/versions assigns the next number (D7), makes the new
  version ACTIVE and every older ACTIVE version DEPRECATED in one transaction (D4).

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18173_AC1 @AC_SIMPL-18187_AC2 @AC_SIMPL-18190_AC1 @AC_SIMPL-18191_AC1 @AC_SIMPL-18186_AC1 @AC_SIMPL-18194_AC2 @D4 @D6 @D7 @D10 @D26 @D27
  Scenario: A new version gets the next number, is ACTIVE and deprecates the previous one
    Given a vocabulary "Health" exists
    When I create a new version of "Health" with:
      | field       | value                                      |
      | description | Healthcare types with extended diagnostics. |
      | changelog   | Added new concept scheme for diagnostics.  |
    Then the response status is 201
    And the Location header is the href of version 2 of "Health"
    And the response is version 2 of "Health" with status "ACTIVE"
    And the field "changelog" is "Added new concept scheme for diagnostics."
    And the field "createdBy" is the email of "ga-admin"
    And the response has no validation warnings
    And the versions of "Health" have the statuses "1=DEPRECATED, 2=ACTIVE"
    And version 1 of "Health" has a lastUpdateDate equal to the creationDate of version 2

  @AC_SIMPL-18173_AC1 @AC_SIMPL-18191_AC1 @D4 @D7
  Scenario: Every previous version is DEPRECATED after the third version
    Given a vocabulary "Health" exists with 2 versions
    When I create a new version of "Health"
    Then the response status is 201
    And the field "version" is the number 3
    And the versions of "Health" have the statuses "1=DEPRECATED, 2=DEPRECATED, 3=ACTIVE"

  @AC_SIMPL-18187_AC2 @D3 @D5 @D25 @D28
  Scenario: The vocabulary shows the new latest version and its description, its status unchanged
    Given a vocabulary "Health" exists
    And a new version of "Health" exists with:
      | field       | value                  |
      | description | Second description.    |
    When I send GET "/vocabularies/${name:Health}"
    Then the response is vocabulary "Health" with latest version 2 and status "ACTIVE"
    And the field "description" is "Second description."
    And the vocabulary "Health" was created by "ga-admin"
    And the vocabulary "Health" has a lastUpdateDate not before the creationDate of version 2

  @D4
  Scenario: After a manual reactivation two versions are ACTIVE and the next version deprecates both
    Given a vocabulary "Health" exists with 2 versions
    And version 1 of vocabulary "Health" has status "ACTIVE"
    Then the versions of "Health" have the statuses "1=ACTIVE, 2=ACTIVE"
    When I create a new version of "Health"
    Then the response status is 201
    And the versions of "Health" have the statuses "1=DEPRECATED, 2=DEPRECATED, 3=ACTIVE"

  @D3
  Scenario: A new version may be created for a DEPRECATED vocabulary, which stays DEPRECATED
    Given a vocabulary "Health" exists
    And the vocabulary "Health" has status "DEPRECATED"
    When I create a new version of "Health"
    Then the response status is 201
    And the field "status" is "ACTIVE"
    And the vocabulary "Health" has status "DEPRECATED"
    And the versions of "Health" have the statuses "1=DEPRECATED, 2=ACTIVE"

  @AC_SIMPL-18190_AC2 @D7 @D8 @negative @OWA-API3 @D39
  Scenario: A request that carries a version number is rejected and creates nothing
    Given a vocabulary "Health" exists
    When I create a new version of "Health" with:
      | field   | value |
      | version | 5     |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "version"
    And the vocabulary "Health" has 1 version
    And the versions of "Health" have the statuses "1=ACTIVE"

  @AC_SIMPL-18173_AC2 @D37 @D8 @negative
  Scenario Outline: An invalid or incomplete new version is rejected: <case>
    Given a vocabulary "Health" exists
    When I create a new version of "Health" with:
      | field   | value   |
      | <field> | <value> |
    Then the response is a problem with status 400
    And the vocabulary "Health" has 1 version
    And the versions of "Health" have the statuses "1=ACTIVE"

    Examples:
      | case                      | field       | value       |
      | description missing       | description | <absent>    |
      | description only spaces   | description | <spaces>    |
      | description of 1001 chars | description | <text:1001> |
      | changelog missing         | changelog   | <absent>    |
      | changelog empty           | changelog   | <empty>     |
      | changelog of 1001 chars   | changelog   | <text:1001> |
      | file missing              | file        | <absent>    |
      | file is not Turtle        | file        | not-turtle.json |

  @AC_SIMPL-18173_AC3 @D24 @negative
  Scenario: A new version of a vocabulary that does not exist is rejected with 404
    When I create a new version of "Missing"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail contains "${name:Missing}"
    And the problem has an issue in "path" named "vocabularyName"

  @AC_SIMPL-18173_AC5 @D17 @negative
  Scenario: A new version without authentication is rejected
    Given a vocabulary "Health" exists
    And I am not authenticated
    When I create a new version of "Health"
    Then the response status is 401
    And the vocabulary "Health" has 1 version

  @AC_SIMPL-18173_AC6 @D15 @negative @API-38 @API-46
  Scenario: A new version in an unsupported format is rejected with 415
    Given a vocabulary "Health" exists
    When I send POST "/vocabularies/${name:Health}/versions" with the content type "application/json" and the body:
      """
      {"description": "d", "changelog": "c"}
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And the vocabulary "Health" has 1 version

  @D15 @D8 @negative @API-46
  Scenario: A new version larger than 5 MB is rejected with 413 and deprecates nothing
    Given a vocabulary "Health" exists
    When I create a new version of "Health" with:
      | field | value           |
      | file  | <bytes:5242881> |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And the versions of "Health" have the statuses "1=ACTIVE"

  @D46 @D8 @negative @API-46
  Scenario: Resubmitting the latest version unchanged is refused with 409 and deprecates nothing
    Given a vocabulary "Health" exists with:
      | field       | value              |
      | description | Same description.  |
    When I create a new version of "Health" with:
      | field       | value                |
      | file        | <same as version 1>  |
      | description | Same description.    |
      | changelog   | A different changelog. |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Version 1 of vocabulary '${name:Health}' already has this content and description."
    And the vocabulary "Health" has 1 version
    And the versions of "Health" have the statuses "1=ACTIVE"

  @D46 @negative
  Scenario: The identical-version check compares the trimmed description
    Given a vocabulary "Health" exists with:
      | field       | value              |
      | description | Same description.  |
    When I create a new version of "Health" with:
      | field       | value                        |
      | file        | <same as version 1>          |
      | description | <padded:Same description.>   |
    Then the response status is 409
    And the vocabulary "Health" has 1 version

  @D46 @D33
  Scenario: A description-only change creates a new version
    Given a vocabulary "Health" exists
    When I create a new version of "Health" with:
      | field       | value                  |
      | file        | <same as version 1>    |
      | description | A better description.  |
    Then the response status is 201
    And the field "version" is the number 2

  @D46
  Scenario: A content-only change creates a new version
    Given a vocabulary "Health" exists with:
      | field       | value              |
      | description | Same description.  |
    When I create a new version of "Health" with:
      | field       | value              |
      | description | Same description.  |
    Then the response status is 201

  @D46 @D37 @negative
  Scenario: Field checks run before the identical-version check
    Given a vocabulary "Health" exists with:
      | field       | value              |
      | description | Same description.  |
    When I create a new version of "Health" with:
      | field       | value               |
      | file        | <same as version 1> |
      | description | Same description.   |
      | changelog   | <absent>            |
    Then the response status is 400

  @D43 @D4
  Scenario: Five overlapping version creations are serialised
    Given a vocabulary "Health" exists
    When I create 5 new versions of "Health" in parallel
    Then the responses have the statuses 201, 201, 201, 201, 201
    And the parallel versions are 2 to 6, each once, with distinct Location headers
    And the versions of "Health" have the statuses "1=DEPRECATED, 2=DEPRECATED, 3=DEPRECATED, 4=DEPRECATED, 5=DEPRECATED, 6=ACTIVE"
