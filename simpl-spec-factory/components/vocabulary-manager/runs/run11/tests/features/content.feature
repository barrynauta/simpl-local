Feature: Download vocabulary content through the API
  The content operations return the stored Turtle bytes as uploaded, media type exactly
  text/turtle, as an attachment named '<Name>.ttl' or '<Name>-v<n>.ttl' (D29, D70).

  Background:
    Given I am a vocabulary administrator

  @D29 @D5 @D70 @API-36
  Scenario: The latest content is downloaded as uploaded
    Given a vocabulary "A" exists
    And vocabulary "A" has a new version
    When I send a GET request to "/content/${A}"
    Then the response status is 200
    And the response content type is exactly "text/turtle"
    And the response header "Content-Disposition" names the attachment "${A}.ttl"
    And the response body equals the content of version 2 of vocabulary "A"

  @D29 @D70
  Scenario: The content of one version is downloaded as uploaded
    Given a vocabulary "A" exists
    And vocabulary "A" has a new version
    When I send a GET request to "/content/${A}/versions/1"
    Then the response status is 200
    And the response content type is exactly "text/turtle"
    And the response header "Content-Disposition" names the attachment "${A}-v1.ttl"
    And the response body equals the content of version 1 of vocabulary "A"

  @D29 @D5
  Scenario: The latest content is that of the highest version even when it is DEPRECATED
    Given a vocabulary "A" exists
    And vocabulary "A" has a new version
    And the status of version 2 of vocabulary "A" is changed to "DEPRECATED"
    When I send a GET request to "/content/${A}"
    Then the response status is 200
    And the response body equals the content of version 2 of vocabulary "A"

  @D29
  Scenario: Content is stored byte for byte, including comments, layout and non-ASCII text
    Given a create vocabulary request for "A"
    And the request file is
      """
      # A comment that must survive   with odd    spacing
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${NS:A}> .

      ex:Concept1   a skos:Concept ;
          skos:prefLabel "Économie"@fr ,
                         "Wirtschaft · Übersicht"@de .
      """
    When I submit the request
    Then the response status is 201
    When I send a GET request to "/content/${A}/versions/1"
    Then the response status is 200
    And the response body equals the uploaded file

  @D29 @D3
  Scenario: A DEPRECATED vocabulary can still be downloaded
    Given a vocabulary "A" exists
    And the status of vocabulary "A" is changed to "DEPRECATED"
    When I send a GET request to "/content/${A}"
    Then the response status is 200
    And the response body equals the content of version 1 of vocabulary "A"

  @D24 @D66 @negative
  Scenario: Content of a vocabulary that does not exist gives 404
    When I send a GET request to "/content/${Missing}"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${Missing}' was not found."
    And the problem has an issue in "path" named "vocabularyName"

  @D24 @D55 @negative
  Scenario: Content of a version that does not exist gives 404
    Given a vocabulary "A" exists
    When I send a GET request to "/content/${A}/versions/2"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Version 2 of vocabulary '${A}' was not found."

  @D63 @D127
  Scenario Outline: A content download accepts text/turtle, */* or no Accept header: <accept>
    Given a vocabulary "A" exists
    When I send a GET request to "/content/${A}" with the header "Accept" set to "<accept>"
    Then the response status is 200
    And the response content type is exactly "text/turtle"

    Examples:
      | accept                     |
      | text/turtle                |
      | */*                        |

  @D63
  Scenario: A content download without an Accept header returns Turtle
    Given a vocabulary "A" exists
    When I send a GET request to "/content/${A}" without an Accept header
    Then the response status is 200
    And the response content type is exactly "text/turtle"

  @D63 @negative @API-37 @API-46
  Scenario Outline: A content download with an Accept header it cannot serve gets 406: <accept>
    Given a vocabulary "A" exists
    When I send a GET request to "/content/${A}/versions/1" with the header "Accept" set to "<accept>"
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"
    And the problem title is "Not Acceptable"
    And the problem has no issues

    Examples:
      | accept           |
      | application/json |
      | text/html        |
      | application/rdf+xml |

  @D127 @D63
  Scenario: A content download accepts application/* and answers Turtle
    Given a vocabulary "A" exists
    When I send a GET request to "/content/${A}" with the header "Accept" set to "application/*"
    Then the response status is 200
    And the response content type is exactly "text/turtle"
