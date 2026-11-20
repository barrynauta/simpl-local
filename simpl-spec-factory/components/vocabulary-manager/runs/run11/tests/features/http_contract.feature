Feature: HTTP contract rules that apply to every operation
  Media types and negotiation (D15, D63, D127), methods (D67, D77, D126, D139), the request
  firewall (D67), problem details (D48) and the correlation id (D86).

  Background:
    Given I am a vocabulary administrator

  @D63 @D127 @API-36
  Scenario Outline: A JSON operation accepts JSON, problem JSON, wildcards or no Accept header: <accept>
    When I send a GET request to "/vocabularies?name=${SID}" with the header "Accept" set to "<accept>"
    Then the response status is 200
    And the response content type is JSON

    Examples:
      | accept                   |
      | application/json         |
      | application/problem+json |
      | */*                      |
      | application/*            |

  @D63
  Scenario: A JSON operation without an Accept header returns JSON
    When I send a GET request to "/vocabularies?name=${SID}" without an Accept header
    Then the response status is 200
    And the response content type is JSON

  @D63 @negative @API-37 @API-46 @API-47
  Scenario Outline: A JSON operation with an Accept header it cannot serve gets 406: <operation>
    Given a vocabulary "A" exists
    When I send a GET request to "<path>" with the header "Accept" set to "text/html"
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"
    And the problem title is "Not Acceptable"
    And the problem has no issues

    Examples:
      | operation      | path                            |
      | list           | /vocabularies                   |
      | details        | /vocabularies/${A}              |
      | versions       | /vocabularies/${A}/versions     |
      | version        | /vocabularies/${A}/versions/1   |
      | external list  | /external-vocabularies          |

  @D63 @D51 @negative
  Scenario: Accept is checked after the token and the role
    Given I am a vocabulary viewer
    When I send a GET request to "/vocabularies" with the header "Accept" set to "text/html"
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"
    Given I am a user without a vocabulary role
    When I send a GET request to "/vocabularies" with the header "Accept" set to "text/html"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    Given I am not authenticated
    When I send a GET request to "/vocabularies" with the header "Accept" set to "text/html"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"

  @D63 @D51 @negative
  Scenario: Accept is checked before the media type of a write
    When I send a POST request to "/vocabularies" with content type "text/plain", the header "Accept" set to "text/html" and body
      """
      not multipart
      """
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"
    And no vocabulary of this scenario is stored

  @D63 @negative
  Scenario: A write with an Accept header it cannot serve gets 406 and stores nothing
    Given a create vocabulary request for "A"
    When I submit the request with the header "Accept" set to "text/html"
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"
    And no vocabulary "A" is stored

  @D51 @negative
  Scenario: The media type is checked before the vocabulary is looked up
    When I send a PATCH request to "/vocabularies/${Missing}" with content type "text/plain" and body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"

  @D67 @D139 @D126 @negative @BLG-meth-http
  Scenario Outline: A method that the path does not declare gets 405 with Allow, with or without a token: <method> <path>
    Given I am <who>
    When I send a <method> request to "<path>"
    Then the response is a problem with status 405
    And the response header "Allow" lists "<allow>"

    Examples:
      | who                        | method  | path                       | allow       |
      | not authenticated          | PUT     | /vocabularies              | GET, POST   |
      | a vocabulary administrator | PUT     | /vocabularies              | GET, POST   |
      | not authenticated          | DELETE  | /vocabularies/${A}         | GET, PATCH  |
      | a vocabulary administrator | DELETE  | /vocabularies/${A}         | GET, PATCH  |
      | a vocabulary administrator | POST    | /content/${A}              | GET         |
      | a vocabulary administrator | PATCH   | /external-vocabularies/${A} | GET, DELETE |
      | not authenticated          | OPTIONS | /vocabularies              | GET, POST   |
      | a vocabulary administrator | OPTIONS | /vocabularies/${A}/versions | GET, POST  |
      | a vocabulary administrator | GET     | /vocabularies/searches     | POST        |
      | a vocabulary administrator | GET     | /external-vocabularies/content | POST    |
      | a vocabulary administrator | GET     | /external-vocabulary/searches | POST     |

  @D126 @D67 @negative
  Scenario Outline: HEAD on a declared path gets 405 with Allow: <who> <path>
    Given I am <who>
    When I send a HEAD request to "<path>"
    Then the response status is 405
    And the response header "Allow" lists "<allow>"

    Examples:
      | who                        | path               | allow      |
      | not authenticated          | /vocabularies      | GET, POST  |
      | a vocabulary administrator | /vocabularies/${A} | GET, PATCH |

  @D67 @negative
  Scenario Outline: A method the server does not know gets 405 before the token check: <who>
    Given I am <who>
    When I send a QUERY request to "/vocabularies"
    Then the response status is 405

    Examples:
      | who                        |
      | not authenticated          |
      | a vocabulary administrator |

  @D67 @negative
  Scenario Outline: A path with an encoded slash is refused before the token check: <who>
    Given I am <who>
    When I send a GET request to "/vocabularies/Abc%2FDef"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has no issues

    Examples:
      | who                        |
      | not authenticated          |
      | a vocabulary administrator |

  @D77
  Scenario: The fixed search path is never read as a vocabulary name
    When I send a POST request to "/vocabularies/searches" with JSON
      """
      {"items": [{"name": "${Missing}"}]}
      """
    Then the response status is 200
    And the response has no items

  @D86 @NFR-08
  Scenario: The service returns the correlation id it receives
    When I send a GET request to "/vocabularies?name=${SID}" with the header "X-Correlation-Id" set to "corr-${SID}"
    Then the response status is 200
    And the response header "X-Correlation-Id" is "corr-${SID}"

  @D86 @NFR-08
  Scenario: The service creates a correlation id when none is sent
    When I send a GET request to "/vocabularies?name=${SID}"
    Then the response status is 200
    And the response header "X-Correlation-Id" is present

  @D86
  Scenario: A request with W3C trace context is served
    When I send a GET request to "/vocabularies?name=${SID}" with the header "traceparent" set to "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
    Then the response status is 200

  @D48 @BLG-err-problem @BLG-prb-type @BLG-err-sanit @API-44 @negative
  Scenario: Client errors are 4xx problem details without implementation details
    When I send a GET request to "/vocabularies?page=abc"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem reveals no implementation details
    When I send a GET request to "/vocabularies/${Missing}"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem reveals no implementation details
