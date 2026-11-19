Feature: HTTP protocol rules that hold on every operation
  Content negotiation, methods, unknown paths, the request firewall and the problem format
  (D63, D67, D77, D48, D55, API-36, API-37, API-44, API-46, API-47).

  @D63 @D51 @negative
  Scenario: The Accept check comes after authentication
    When "anonymous" sends GET "/vocabularies" with the header "Accept" set to "application/xml"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"

  @D63 @API-37 @API-46 @API-47 @negative
  Scenario Outline: An Accept header the operation cannot serve gives 406 (<path>)
    Given vocabulary "Alpha" exists
    When "ga-admin" sends GET "<path>" with the header "Accept" set to "<accept>"
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"
    And the problem title is "Not Acceptable"
    And the problem has no issues
    And the problem type is a simpl problem type in lowerCamelCase

    Examples:
      | path                                 | accept           |
      | /vocabularies?name=${rid}            | application/xml  |
      | /vocabularies/${V:Alpha}             | text/html        |
      | /vocabularies/${V:Alpha}/versions    | text/turtle      |
      | /content/${V:Alpha}                  | application/json |
      | /content/${V:Alpha}/versions/1       | application/xml  |

  @D63 @API-37
  Scenario Outline: Accepted Accept headers are served (<path>, <accept>)
    Given vocabulary "Alpha" exists
    When "ga-admin" sends GET "<path>" with the header "Accept" set to "<accept>"
    Then the response status is 200

    Examples:
      | path                      | accept                   |
      | /vocabularies/${V:Alpha}  | application/json         |
      | /vocabularies/${V:Alpha}  | application/problem+json |
      | /vocabularies/${V:Alpha}  | */*                      |
      | /content/${V:Alpha}       | text/turtle              |
      | /content/${V:Alpha}       | */*                      |

  @D63 @D51 @negative
  Scenario: The Accept check comes before the media type check
    When "ga-admin" sends POST "/vocabularies/searches" with JSON and the header "Accept" set to "application/xml"
      """
      {"items": [{"name": "Valid${rid}"}]}
      """
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"
    When "ga-admin" sends POST "/vocabularies" with JSON and the header "Accept" set to "application/xml"
      """
      {"name": "V${rid}Json"}
      """
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"
    And no vocabulary of this scenario is stored

  @D67 @D55 @negative
  Scenario: An unknown path gives 401 without a token and 404 problem+json with one
    When "anonymous" sends GET "/nothing-here-${rid}"
    Then the response status is 401
    When "ga-admin" sends GET "/nothing-here-${rid}"
    Then the response is a problem with status 404

  @D67 @negative
  Scenario: Actuator paths do not exist on the API port
    When "anonymous" sends GET "/actuator/health"
    Then the response status is 401
    When "ga-admin" sends GET "/actuator/health"
    Then the response is a problem with status 404
    When "ga-admin" sends GET "/actuator/env"
    Then the response is a problem with status 404

  @D67 @D55 @negative
  Scenario: A method the server does not know gives 405 before the token check
    When "anonymous" sends QUERY "/vocabularies"
    Then the response is a problem with status 405

  @D67 @D79 @D55 @negative
  Scenario Outline: A method an operation path does not declare gives 405 (<method> <path>)
    Given vocabulary "Alpha" exists
    When "ga-admin" sends <method> "<path>"
    Then the response is a problem with status 405
    And vocabulary "Alpha" has 1 version

    Examples:
      | method | path                              |
      | DELETE | /vocabularies/${V:Alpha}          |
      | PUT    | /vocabularies/${V:Alpha}          |
      | DELETE | /vocabularies/${V:Alpha}/versions/1 |
      | PATCH  | /external-vocabularies            |

  @D77 @negative
  Scenario Outline: Fixed segments win over a name and a method they do not offer gives 405 (<path>)
    When "ga-admin" sends GET "<path>"
    Then the response is a problem with status 405

    Examples:
      | path                            |
      | /vocabularies/searches          |
      | /external-vocabularies/content  |

  @D67 @negative
  Scenario Outline: A path the firewall refuses gives 400 before the token check (<case>)
    When "anonymous" sends GET "<path>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has no issues

    Examples:
      | case           | path                         |
      | encoded slash  | /vocabularies/Abc%2FDef      |
      | control char   | /vocabularies/Abc%00Def      |

  @API-36 @API-44 @BLG-err-problem @BLG-err-sanit @D48 @negative
  Scenario: Client errors are 4xx problem details that reveal no implementation details
    When "ga-admin" sends POST "/vocabularies/searches" with the body "{not json" of type "application/json"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the response body does not reveal implementation details
    When "ga-admin" creates vocabulary "Alpha" with the file "syntax_error.ttl"
    Then the response is a problem with status 400
    And the response body does not reveal implementation details

  @API-36 @D29
  Scenario: Every response with a body carries a Content-Type
    Given vocabulary "Alpha" exists
    When "ga-admin" gets vocabulary "Alpha"
    Then the response media type is "application/json"
    When "ga-admin" downloads the latest content of "Alpha"
    Then the response media type is "text/turtle"
    When "ga-admin" gets version 9 of "Alpha"
    Then the response media type is "application/problem+json"
