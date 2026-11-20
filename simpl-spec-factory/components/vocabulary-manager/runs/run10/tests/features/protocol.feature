@protocol
Feature: Cross-cutting protocol rules
  Problem details, methods, rejected paths, trace headers and health (D44, D55, D63, D67, D86).

  @D67 @D55 @negative
  Scenario: A method the server does not know gives 405 before the token is checked
    Given I use no token
    When I send "QUERY" to "/vocabularies"
    Then the response is a problem with status 405

  @D67 @D55 @D79 @BLG-meth-http @negative
  Scenario Outline: An undeclared method <method> on <path> gives 405
    Given I use the token of "ga-admin"
    When I send "<method>" to "<path>"
    Then the response is a problem with status 405

    Examples:
      | method | path                          |
      | PUT    | /vocabularies                 |
      | DELETE | /vocabularies                 |
      | DELETE | /vocabularies/AnyVocabulary   |
      | PUT    | /vocabularies/AnyVocabulary   |
      | POST   | /content/AnyVocabulary        |
      | PATCH  | /external-vocabularies/AnyVocabulary |

  @D67 @negative
  Scenario Outline: A path the firewall refuses gives 400 before the token is checked: <path>
    Given I use no token
    When I send "GET" to "<path>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has no issues

    Examples:
      | path                       |
      | /vocabularies/Abc%2FDef    |
      | /vocabularies/Abc%00Def    |
      | /content/Abc%2FDef         |

  @D86 @NFR-08
  Scenario: The service returns the correlation id it was sent
    Given I use the token of "ga-viewer"
    When I send "GET" to "/vocabularies?pageSize=1" with the header "X-Correlation-Id" set to "vmtest-${run}-corr"
    Then the status is 200
    And the header "X-Correlation-Id" is "vmtest-${run}-corr"

  @D86 @NFR-08
  Scenario: The service creates a correlation id when none is sent
    Given I use the token of "ga-viewer"
    When I send "GET" to "/vocabularies?pageSize=1"
    Then the status is 200
    And the header "X-Correlation-Id" is present

  @D86 @API-75
  Scenario: A W3C traceparent header is accepted
    Given I use the token of "ga-viewer"
    When I send "GET" to "/vocabularies?pageSize=1" with the header "traceparent" set to "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
    Then the status is 200

  @D44 @D108 @NFR-07 @MACH-08 @BLG-hlth-res
  Scenario: Health reports UP while the store answers
    Given I use no token
    When I send "GET" to "/health"
    Then the status is 200
    And the response field "status" is "UP"

  @D48 @BLG-err-problem @BLG-err-sanit @API-44 @negative
  Scenario: A client error is a 4xx problem without implementation details
    Given I use the token of "ga-admin"
    When I send "GET" to "/vocabularies?page=abc"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the response field "detail" matches "^(?!.*(Exception|\.java|at org\.|at com\.|SQL|SPARQL)).*$"
