@DataProvider @SchemaValidation @CBSchemaValidation @ContractBilling @regression @Data1
Feature: Contract Billing - Response Schema Validation
  As a QA engineer
  I want to verify that Contract Billing API endpoints return
  a response body that conforms to the shape declared in the OpenAPI specification

  @TCCB_SV03
  Scenario: [POST /credentials/.../issue] 401 - error response conforms to error response schema
    Given a user without authorization is based on "DATA_PROVIDER"
    When the user issues a verifiable credential for schema validation
    Then the response status code is 401
    And the contract billing response body conforms to the error response schema

  @TCCB_SV04
  Scenario: [POST /credentials/.../create] 401 - error response conforms to error response schema
    Given a user without authorization is based on "DATA_PROVIDER"
    When the user creates contract data for schema validation
    Then the response status code is 401
    And the contract billing response body conforms to the error response schema

  @TCCB_SV05
  Scenario: [POST /agreements/{id}/search] 401 - error response conforms to error response schema
    Given a user without authorization is based on "DATA_PROVIDER"
    When the user searches contract agreements for schema validation
    Then the response status code is 401
    And the contract billing response body conforms to the error response schema

  @TCCB_SV07 @SIMPL-11127
  Scenario: [GET /agreements/{id}] 404 - error response for random agreement id conforms to error response schema
    Given a participant with participant type "DATA_PROVIDER" is onboarded with the following data:
      | Username         | RANDOM_USERNAME     |
      | First name       | PSO                 |
      | Last name        | SV_CB_AGR           |
      | Email            | RANDOM_EMAIL        |
      | Password         | RANDOM_PASSWORD     |
      | Participant Type | DATA_PROVIDER       |
      | Organization     | RANDOM_ORGANIZATION |
    And a user with role "T1UAR_M" logs in to "data provider"
    When the user retrieves a contract agreement by random id for schema validation
    Then the response status code is 404
    And the contract billing response body conforms to the error response schema
