Feature: HTTP rules that hold for every operation
  Every error is application/problem+json (D48, D158, D178); the simpl problem types follow API-46
  and API-47 (D48); client errors are 4xx (API-44); every body has a Content-Type (API-36).

  @D158 @D178 @negative
  Scenario: A URL that is too long gets 414 as problem+json
    Given I am authenticated as "ga-admin"
    When I send a GET request to "/vocabularies" with a query value of 200000 characters
    Then the response is a problem with status 414

  @D158 @D178 @negative
  Scenario: Headers that are too large get 431 as problem+json
    Given I am authenticated as "ga-admin"
    When I send a GET request to "/vocabularies" with a header of 200000 characters
    Then the response is a problem with status 431

  @D158 @negative
  Scenario: A query with bad percent-encoding gets 400 badRequest without issues
    Given I am authenticated as "ga-admin"
    When I send a GET request to "/vocabularies?name=%ZZ"
    Then the response is a problem with status 400
    And the problem type is "urn:problem-type:belgif:badRequest"
    And the problem has no issues

  @D48 @D17 @D63 @API-46 @API-47 @API-44 @API-36 @BLG-err-problem @BLG-prb-type @negative
  Scenario Outline: Problem bodies with a decided title: <status>
    Given I am authenticated as "<user>"
    And the request header "Accept" is "<accept>"
    When I send a GET request to "/vocabularies"
    Then the response is a problem with status <status>
    And the problem type is "<type>"
    And the problem title is "<title>"
    And the response has a Content-Type header

    Examples:
      | user      | accept          | status | type                                 | title          |
      | ga-norole | */*             | 403    | urn:problem-type:simpl:forbidden     | Forbidden      |
      | ga-admin  | application/xml | 406    | urn:problem-type:simpl:notAcceptable | Not Acceptable |

  @D48 @D55 @API-44 @API-36 @BLG-err-problem @BLG-prb-type @negative
  Scenario Outline: Problem bodies of the belgif codes carry the decided type: <status>
    Given I am authenticated as "ga-admin"
    When I send a GET request to "<path>"
    Then the response is a problem with status <status>
    And the problem type is "<type>"
    And the response has a Content-Type header

    Examples:
      | path                   | status | type                                     |
      | /vocabularies/$Missing | 404    | urn:problem-type:belgif:resourceNotFound |
      | /vocabularies?page=0   | 400    | urn:problem-type:belgif:badRequest       |
