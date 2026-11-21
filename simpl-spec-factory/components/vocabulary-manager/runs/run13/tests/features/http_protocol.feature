Feature: HTTP protocol rules: methods, media types, unknown paths and server limits
  Content negotiation D63, D127, D151. Methods and unknown paths D67, D77, D126, D139, D166.
  Server-level refusals D158. Correlation id D86. Health D44, D108, D149.

  @D63 @D151
  Scenario: A JSON operation accepts a request without an Accept header
    Given I am authenticated as "ga-admin"
    When I send GET "/vocabularies?name={tok}"
    Then the response status is 200
    And the response media type is "application/json"

  @D63 @D151 @API-37
  Scenario Outline: A JSON operation accepts the Accept value <accept>
    Given I am authenticated as "ga-admin"
    When I send GET "/vocabularies?name={tok}" with the headers
      | header | value    |
      | Accept | <accept> |
    Then the response status is 200
    And the response media type is "application/json"

    Examples:
      | accept                   |
      | application/json         |
      | application/problem+json |
      | */*                      |
      | application/*            |

  @D63 @API-37 @API-46 @API-47 @negative
  Scenario Outline: A JSON operation refuses the Accept value <accept> with 406
    Given I am authenticated as "ga-admin"
    When I send GET "/vocabularies?name={tok}" with the headers
      | header | value    |
      | Accept | <accept> |
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"
    And the problem title is "Not Acceptable"
    And the problem has no issues
    And the problem type follows the Simpl URN form

    Examples:
      | accept      |
      | text/html   |
      | text/turtle |
      | image/png   |

  @D63 @D127 @D29
  Scenario Outline: A content download accepts the Accept value <accept>
    Given I am authenticated as "ga-admin"
    And a vocabulary "Alpha" exists
    When I send GET "/content/{Alpha}" with the headers
      | header | value    |
      | Accept | <accept> |
    Then the response status is 200
    And the response Content-Type is exactly "text/turtle"

    Examples:
      | accept        |
      | text/turtle   |
      | */*           |
      | application/* |

  @D63 @API-37 @negative
  Scenario: A content download refuses application/json with 406
    Given I am authenticated as "ga-admin"
    And a vocabulary "Alpha" exists
    When I send GET "/content/{Alpha}/versions/1" with the headers
      | header | value            |
      | Accept | application/json |
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"

  @D63 @D51 @negative
  Scenario: The Accept check runs after the role check and before the media type check
    Given I am authenticated as "ga-admin"
    When I send POST "/vocabularies" with a JSON body and the Accept header "text/html"
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"
    Given I am authenticated as "ga-viewer"
    When I send POST "/vocabularies" with a JSON body and the Accept header "text/html"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"

  @D139 @D67 @negative @API-12
  Scenario Outline: A method the path does not declare gets 405 with Allow, with or without a token: <method> <path>
    Given <auth>
    When I send <method> "<path>"
    Then the response is a problem with status 405
    And the response header "Allow" lists "<allowed>"

    Examples:
      | auth                                | method | path                    | allowed     |
      | I am not authenticated              | PUT    | /vocabularies           | GET, POST   |
      | I am authenticated as "ga-admin"    | PUT    | /vocabularies           | GET, POST   |
      | I am authenticated as "ga-admin"    | DELETE | /vocabularies/{Alpha}   | GET, PATCH  |
      | I am not authenticated              | POST   | /health                 | GET         |
      | I am authenticated as "ga-admin"    | PATCH  | /external-vocabularies  | GET, POST   |

  @D126 @negative
  Scenario Outline: <method> on a declared path gets 405 with Allow
    Given I am authenticated as "ga-admin"
    When I send <method> "/vocabularies"
    Then the response status is 405
    And the response header "Allow" lists "GET, POST"

    Examples:
      | method  |
      | HEAD    |
      | OPTIONS |

  @D166 @D67 @negative
  Scenario: TRACE is refused with 405
    Given I am authenticated as "ga-admin"
    When I send TRACE "/vocabularies"
    Then the response is a problem with status 405

  @D67 @negative
  Scenario: A method the server does not know gets 405
    Given I am not authenticated
    When I send QUERY "/vocabularies"
    Then the response is a problem with status 405

  @D77 @negative
  Scenario Outline: A fixed path is never read as a name: <method> <path>
    Given I am authenticated as "ga-admin"
    When I send <method> "<path>"
    Then the response is a problem with status 405

    Examples:
      | method | path                           |
      | GET    | /vocabularies/searches         |
      | PATCH  | /vocabularies/searches         |
      | GET    | /external-vocabularies/content |
      | DELETE | /external-vocabularies/content |

  @D67 @D84 @D155 @negative
  Scenario Outline: An unknown path gives 401 without a token and 404 with one: <path>
    Given I am not authenticated
    When I send GET "<path>"
    Then the response is a problem with status 401
    Given I am authenticated as "ga-admin"
    When I send GET "<path>"
    Then the response is a problem with status 404

    Examples:
      | path                    |
      | /unknown-{tok:lower}    |
      | /vocabulary             |
      | /actuator/health        |
      | /actuator/prometheus    |
      | /vocabularies/{Alpha}/x |

  @D67 @D98 @negative
  Scenario Outline: The management paths are not served on the API port: <path>
    Given I am not authenticated
    When I send GET "<path>" outside the API base path
    Then the response is a problem with status 401
    Given I am authenticated as "ga-admin"
    When I send GET "<path>" outside the API base path
    Then the response is a problem with status 404

    Examples:
      | path                 |
      | /actuator/health     |
      | /actuator/prometheus |

  @D67 @D158 @negative
  Scenario: A path with an encoded slash is refused with 400 before the token check
    Given I am not authenticated
    When I send GET "/vocabularies/Abc%2F{tok}"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has no issues

  @D158 @negative
  Scenario: A query with bad percent-encoding is refused with 400
    Given I am authenticated as "ga-admin"
    When I send GET "/vocabularies?name=%ZZ"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"

  @D158 @negative
  Scenario: A URL that is far too long gets 414 as problem+json
    Given I am authenticated as "ga-admin"
    When I send GET "/vocabularies" with a query string of 200000 characters
    Then the response is a problem with one of the statuses 400, 414

  @D158 @negative
  Scenario: Headers that are far too large get 431 as problem+json
    Given I am authenticated as "ga-admin"
    When I send GET "/vocabularies" with a header "X-Padding" of 200000 characters
    Then the response is a problem with one of the statuses 400, 431

  @D86
  Scenario: The service returns the correlation id it received
    Given I am authenticated as "ga-admin"
    When I send GET "/vocabularies?name={tok}" with the headers
      | header           | value          |
      | X-Correlation-Id | corr-{tok}     |
    Then the response status is 200
    And the response header "X-Correlation-Id" is "corr-{tok}"

  @D86
  Scenario: The service creates a correlation id when none is sent
    Given I am authenticated as "ga-admin"
    When I send GET "/vocabularies?name={tok}"
    Then the response header "X-Correlation-Id" is present

  @D86
  Scenario: A request with a W3C traceparent header is served
    Given I am authenticated as "ga-admin"
    When I send GET "/vocabularies?name={tok}" with the headers
      | header      | value                                                   |
      | traceparent | 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01 |
    Then the response status is 200

  @D149 @D21 @negative
  Scenario: An unknown query parameter on the health operation is rejected
    Given I am not authenticated
    When I send GET "/health?verbose=true"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "verbose"

  @D48 @API-44 @API-36 @BLG-err-problem @BLG-prb-type
  Scenario Outline: Error responses are problem details with the decided type: <status>
    Given I am authenticated as "<user>"
    When I send <method> "<path>"
    Then the response is a problem with status <status> and type "<type>"
    And every response with a body has a Content-Type header

    Examples:
      | user      | method | path                       | status | type                                     |
      | ga-admin  | GET    | /vocabularies?pageSize=0   | 400    | urn:problem-type:belgif:badRequest       |
      | ga-norole | GET    | /vocabularies              | 403    | urn:problem-type:simpl:forbidden         |
      | ga-admin  | GET    | /vocabularies/{Ghost}      | 404    | urn:problem-type:belgif:resourceNotFound |

  @D162 @D63 @negative
  Scenario: On a read the Accept check runs before the query check
    Given I am authenticated as "ga-admin"
    When I send GET "/vocabularies?pageSize=0" with the headers
      | header | value     |
      | Accept | text/html |
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"
