Feature: Cross-cutting HTTP behaviour
  Error format, media types and status codes that hold for every operation
  (D48, D55, BLG-err-problem, API-36, API-44).

  Background:
    Given I am authenticated as "ga-admin"
    And the vocabulary "Health" exists

  @D29 @D48 @API-36 @BLG-rep-struct
  Scenario Outline: Every response with a body carries a Content-Type on <path>
    When I send "GET" to "<path>"
    Then the response status is <status>
    And the response has a Content-Type header

    Examples:
      | path                                      | status |
      | /health                                   | 200    |
      | /vocabularies?name={{run}}                | 200    |
      | /vocabularies/{{name:Health}}             | 200    |
      | /vocabularies/{{name:Health}}/versions    | 200    |
      | /content/{{name:Health}}                  | 200    |
      | /vocabularies/{{name:Ghost}}              | 404    |
      | /vocabularies?page=0                      | 400    |

  @D55 @BLG-err-problem @API-45 @negative
  Scenario: A method the resource does not support gives 405 as a problem
    When I send "PUT" to "/vocabularies/{{name:Health}}" with the JSON body:
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 405
    And the vocabulary "Health" has the status "ACTIVE"

  @D55 @BLG-err-problem @API-45 @negative
  Scenario: An unknown path gives 404 as a problem
    When I send "GET" to "/unknown-resources/{{run}}"
    Then the response is a problem with status 404

  @D38 @API-44 @BLG-stat-codes @BLG-err-sanit @negative
  Scenario: A client error is a 4xx problem without implementation details
    When I send "POST" to "/vocabularies/searches" with the JSON body:
      """
      {"items": [
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem detail does not leak implementation details

  @D48 @API-47 @negative
  Scenario Outline: Problem types use lowerCamelCase segments: <case>
    When I send "<method>" to "<path>"
    Then the response status is <status>
    And the problem type segments are lowerCamelCase

    Examples:
      | case      | method | path                              | status |
      | not found | GET    | /vocabularies/{{name:Ghost}}      | 404    |
      | bad query | GET    | /vocabularies?colour=blue         | 400    |

  @D17 @D48 @API-47 @negative
  Scenario: The 401 problem type uses a lowerCamelCase segment
    Given I am not authenticated
    When I send "GET" to "/vocabularies"
    Then the response status is 401
    And the problem type segments are lowerCamelCase
