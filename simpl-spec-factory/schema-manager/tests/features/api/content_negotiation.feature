@openapi-only
Feature: Content negotiation and media types
  The 406 and 415 responses of the OpenAPI, as fixed by D8. SIMPL-20529 is the
  only story that names 406 (on PATCH, covered in status_change.feature).

  Background:
    Given the RESOURCE schema "$A" exists for resource type "$RT"

  @D8
  Scenario Outline: JSON operations accept JSON, problem JSON and */*
    When I send a GET request to "<path>" with header "Accept" set to "<accept>"
    Then the response status is 200

    Examples:
      | path                 | accept                   |
      | /schemas             | application/json         |
      | /schemas             | application/problem+json |
      | /schemas             | */*                      |
      | /schemas/$A/versions | application/json         |
      | /webhooks            | application/json         |

  @D8
  Scenario Outline: JSON operations refuse an Accept header they cannot satisfy
    When I send a GET request to "<path>" with header "Accept" set to "<accept>"
    Then the response is a "notAcceptable" problem with status 406

    Examples:
      | path                 | accept          |
      | /schemas             | text/csv        |
      | /schemas             | application/xml |
      | /schemas/$A/versions | text/html       |
      | /webhooks            | application/xml |

  @D8 @D9 @SIMPL-13347
  Scenario Outline: The latest content refuses an Accept header other than Turtle
    When I send a GET request to "/schemas/$A" with header "Accept" set to "<accept>"
    Then the response is a "notAcceptable" problem with status 406

    Examples:
      | accept          |
      | application/xml |
      | text/html       |

  @D8 @D10 @SIMPL-13364
  Scenario Outline: A version refuses an Accept header other than Turtle or JSON
    When I send a GET request to "/schemas/$A/1" with header "Accept" set to "<accept>"
    Then the response is a "notAcceptable" problem with status 406

    Examples:
      | accept          |
      | application/xml |
      | text/csv        |

  @D8 @SIMPL-19992
  Scenario: A webhook subscription that is not JSON is rejected with 415
    When I send a POST request to "/webhooks" with content type "application/x-www-form-urlencoded" and body
      """
      targetUrl=https%3A%2F%2Fexample.org%2Fhook&events=SchemaPublished
      """
    Then the response is a "unsupportedMediaType" problem with status 415

  @D62
  Scenario Outline: Out-of-scope operations of the OpenAPI are not served
    When I send a GET request to "<path>"
    Then the response status is 404

    Examples:
      | path                                   |
      | /vocabularies                          |
      | /schemas/$A/qualityProfiles/active     |
