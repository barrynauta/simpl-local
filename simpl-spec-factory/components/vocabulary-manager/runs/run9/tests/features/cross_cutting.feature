Feature: Content negotiation, problem format and trace headers
  Accept handling (D63), problem types (D48, API-46, API-47), correlation id (D86).

  @D63 @negative @API-37 @API-46 @BLG-cnt-negot
  Scenario Outline: An Accept header a JSON operation cannot serve gives 406 (<accept>)
    Given a vocabulary "Health" exists
    And the request header "Accept" is "<accept>"
    When I send a "GET" request to "/vocabularies/${Health}"
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"
    And the problem title is "Not Acceptable"
    And the problem has no issues
    And the problem type follows API-46 and API-47

    Examples:
      | accept           |
      | application/xml  |
      | text/turtle      |
      | text/html        |

  @D63 @negative @API-37
  Scenario Outline: An Accept header a download cannot serve gives 406 (<accept>)
    Given a vocabulary "Health" exists
    And the request header "Accept" is "<accept>"
    When I send a "GET" request to "/content/${Health}"
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"
    And the problem has no issues

    Examples:
      | accept           |
      | application/json |
      | application/xml  |

  @D63
  Scenario Outline: A JSON operation accepts <accept>
    Given a vocabulary "Health" exists
    And the request header "Accept" is "<accept>"
    When I send a "GET" request to "/vocabularies/${Health}"
    Then the response status is 200

    Examples:
      | accept                   |
      | application/json         |
      | application/problem+json |
      | */*                      |

  @D63
  Scenario: A JSON operation accepts a request without an Accept header
    Given a vocabulary "Health" exists
    And the request has no Accept header
    When I send a "GET" request to "/vocabularies/${Health}"
    Then the response status is 200

  @D63 @D29
  Scenario Outline: A download accepts <accept>
    Given a vocabulary "Health" exists
    And the request header "Accept" is "<accept>"
    When I send a "GET" request to "/content/${Health}/versions/1"
    Then the response status is 200
    And the response content type is exactly "text/turtle"

    Examples:
      | accept      |
      | text/turtle |
      | */*         |

  @D63 @D51 @negative
  Scenario: The Accept check runs before the media type check on a write
    Given the request header "Accept" is "application/xml"
    When I send a "POST" request to "/vocabularies" with content type "text/plain" and body
      """
      not multipart
      """
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"

  @D63 @D51 @negative
  Scenario: The role check runs before the Accept check
    Given I am "ga-viewer"
    And the request header "Accept" is "application/xml"
    When I send a "POST" request to "/vocabularies" with content type "text/plain" and body
      """
      not multipart
      """
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"

  @D63 @D8 @negative
  Scenario: A create refused with 406 stores nothing
    Given the request header "Accept" is "application/xml"
    When I create the vocabulary "Picky"
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"
    And no vocabulary named "Picky" is stored

  @D86
  Scenario: The service returns the X-Correlation-Id it received
    Given the request header "X-Correlation-Id" is "corr-${run}"
    When I send a "GET" request to "/vocabularies?name=${run}"
    Then the response status is 200
    And the response header "X-Correlation-Id" is "corr-${run}"

  @D86
  Scenario: The service creates an X-Correlation-Id when none is sent
    When I send a "GET" request to "/vocabularies?name=${run}"
    Then the response header "X-Correlation-Id" is present

  @D86
  Scenario: A W3C traceparent header is accepted
    Given the request header "traceparent" is "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
    When I send a "GET" request to "/vocabularies?name=${run}"
    Then the response status is 200
    And the response header "X-Correlation-Id" is present

  @D48 @API-36 @API-44 @API-47 @BLG-err-problem
  Scenario Outline: Every error is problem+json with a lowerCamelCase type (<case>)
    Given a vocabulary "Health" exists
    When I send a "GET" request to "<path>"
    Then the response is a problem with status <status> and type "<type>"
    And the response has a Content-Type header
    And the problem type follows API-46 and API-47
    And the problem has a detail

    Examples:
      | case           | path                                  | status | type                                   |
      | bad query      | /vocabularies?pageSize=0              | 400    | urn:problem-type:belgif:badRequest     |
      | not found      | /vocabularies/${Ghost}                | 404    | urn:problem-type:belgif:resourceNotFound |
      | bad path value | /vocabularies/lower${run}             | 400    | urn:problem-type:belgif:badRequest     |
